package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* Design inspiration from ConcurrentLinkedHashMap */
public class ConcurrentLruCache implements Cache {
    static final int DRAIN_THRESHOLD = 40;

    /** Information-storing data structures */
    private final Map<String, MapNode> data;
    private final DoublyLinkedList<MapNode> lruQueue = new DoublyLinkedList<>();

    /** Lock controlling access to the above lruQueue */
    private final Lock lock = new ReentrantLock();

    /** Amount of data currently in cache versus what's allowed */
    private final int capacity;
    private AtomicInteger load = new AtomicInteger(0);

    /** Buffer and its counter */
    private final ConcurrentLinkedQueue<Action> buffer;
    private AtomicInteger bufSize = new AtomicInteger(0);
    /** Tracks the status of the drain */
    private boolean drainActive = false;
    private final AtomicBoolean isEager = new AtomicBoolean(false);
    /** Pool on which to run the drain thread */
    private final ExecutorService pool;

    public ConcurrentLruCache(int capacity) {
        this.capacity = capacity;
        data = new ConcurrentHashMap<>();
        buffer = new ConcurrentLinkedQueue<>();
        pool = Executors.newCachedThreadPool();
    }

    @Override
    public String get(String key) {
        MapNode result = data.get(key);
        String value = null;

        if (result != null) {
            buffer.offer(new Action(AccessType.READ, result));
            bufSize.incrementAndGet();
            value = result.getValue();
        }

        asyncDrainIfNeeded();
        return value;
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        // If previous value exists, remove it
        if (data.containsKey(key)) {
            return false;
        }

        load.addAndGet(size);
        evict();

        MapNode node = new MapNode(key, value, cost, size);
        data.put(key, node);

        buffer.offer(new Action(AccessType.WRITE, node));
        bufSize.incrementAndGet();

        isEager.lazySet(true);
        asyncDrainIfNeeded();
        return true;
    }

    public void shutDown() {
        pool.shutdownNow();
    }

    private void asyncDrainIfNeeded() {
        if (shouldDrain()) {
            pool.execute(this::tryDrain);
        }
    }

    /** Checks if buffer should be drained */
    private boolean shouldDrain() {
        if (drainActive) {
            return false;
        } else if (isEager.get()) {
            return true;
        }
        return bufSize.intValue() > DRAIN_THRESHOLD;
    }

    /** Tries to drain buffer, if someone else isn't already draining it */
    private void tryDrain() {
        boolean success = lock.tryLock();
        if (!success) {
            return;
        }

        drain();
        lock.unlock();
    }

    private void drain() {
        drainActive = true;
        isEager.lazySet(false);

        // Drain elements according to number that was in the buffer originally
        int toDrain = bufSize.intValue();
        bufSize.addAndGet(-1 * toDrain);
        for(int i = 0; i < toDrain; i++) {
            Action a = buffer.poll();
            if (a.node.isEvicted()) {
                continue;
            }
            // For reads, don't bother doing anything if the node not in list
            if (a.type == AccessType.READ) {
                lruQueue.moveTail(a.node);
            } else if (a.type == AccessType.WRITE) {
                lruQueue.pushTail(a.node);
            }
        }

        drainActive = false;
    }

    /** Evicts until properly sized. */
    private void evict() {
        if (!canAndShouldEvict() || !lock.tryLock()) {
            return;
        }

        while (shouldEvict()) {
            while (canAndShouldEvict()) {
                evictOne();
            }
            if (shouldEvict()) {
                drain();
            }
        }
        lock.unlock();
    }

    /** Evicts an entry. Expects to hold lock. */
    private void evictOne() {
        MapNode node = lruQueue.popHead();
        if (node == null) {
            return;
        }

        node.setEvicted();
        String key = node.getKey();
        if (data.containsKey(key)) {
            load.addAndGet(-1 * node.getSize());
            data.remove(key);
        }
    }

    private boolean canAndShouldEvict() {
        return shouldEvict() && !lruQueue.isEmpty();
    }

    private boolean shouldEvict() {
        return load.intValue() > capacity;
    }

    private enum AccessType {
        READ, WRITE
    }

    private class Action {
        public AccessType type;
        public MapNode node;

        public Action(AccessType type, MapNode node) {
            this.type = type;
            this.node = node;
        }
    }
}
