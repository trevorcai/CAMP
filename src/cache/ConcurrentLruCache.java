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

public class ConcurrentLruCache implements Cache {
    static final int DRAIN_THRESHOLD = 40;

    /** Information-storing data structures */
    private final Map<String, MapNode> data;
    private final DoublyLinkedList<String> lruQueue = new DoublyLinkedList<>();

    /** Lock controlling access to the above lruQueue */
    private final Lock lock = new ReentrantLock();

    /** Amount of data currently in cache versus what's allowed */
    private final int capacity;
    private AtomicInteger load = new AtomicInteger(0);

    /** Buffer and its counter */
    private final ConcurrentLinkedQueue<Action> buffer;
    private AtomicInteger bufSize = new AtomicInteger(0);
    /** Tracks the status of the drain */
    private boolean isActive = false;
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
            buffer.offer(new Action(AccessType.READ, result.node));
            bufSize.incrementAndGet();
            value = result.value;
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

        ListNode<String> listNode = new ListNode<>(key);
        MapNode node = new MapNode(value, cost, size, listNode);
        data.put(key, node);
        load.addAndGet(size);

        buffer.offer(new Action(AccessType.WRITE, listNode));
        bufSize.incrementAndGet();

//        isEager.lazySet(true);
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
        if (isActive) {
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

        isActive = true;
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
                evict();
                lruQueue.pushTail(a.node);
            }
        }

        isActive = false;
        lock.unlock();
    }

    /** Evicts until properly sized. Expects to hold lock. */
    private void evict() {
        while (load.intValue() > capacity) {
            evictOne();
        }
    }
    /** Evicts an entry. Expects to hold lock. */
    private void evictOne() {
        ListNode<String> listNode = lruQueue.popHead();
        if (listNode == null) {
            return;
        }

        listNode.setEvicted();
        String key = listNode.value;
        MapNode node = data.get(key);
        if (node != null) {
            load.addAndGet(-1 * node.getSize());
            data.remove(key);
        }
    }

    private enum AccessType {
        READ, WRITE
    }

    private class Action {
        public AccessType type;
        public ListNode<String> node;

        public Action(AccessType type, ListNode<String> node) {
            this.type = type;
            this.node = node;
        }
    }
}
