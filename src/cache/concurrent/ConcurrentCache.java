package cache.concurrent;

import cache.Cache;
import cache.MapNode;

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
public abstract class ConcurrentCache implements Cache {
    private static final int DRAIN_THRESHOLD = 40;

    /** Backing Map */
    final Map<String, MapNode> data;

    /** Lock controlling access to the above lruQueue */
    final Lock lock = new ReentrantLock();

    /** Amount of data currently in cache versus what's allowed */
    final int capacity;
    final AtomicInteger load = new AtomicInteger(0);

    /** Buffer and its counter */
    private final ConcurrentLinkedQueue<Action> buffer;
    private final AtomicInteger bufSize = new AtomicInteger(0);
    /** Tracks the status of the drain */
    private boolean drainActive = false;
    private final AtomicBoolean isEager = new AtomicBoolean(false);
    /** Pool on which to run the drain thread */
    private final ExecutorService pool;

    public ConcurrentCache(int capacity, int concurrency) {
        this.capacity = capacity;
        data = new ConcurrentHashMap<>(128, 0.75f, concurrency);
        buffer = new ConcurrentLinkedQueue<>();
        pool = Executors.newCachedThreadPool();
    }

    @Override
    public String get(String key) {
        MapNode result = data.get(key);
        if (result == null) {
            return null;
        }

        buffer.offer(new Action(AccessType.READ, result));
        bufSize.incrementAndGet();
        asyncDrainIfNeeded();
        return result.getValue();
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        // Attempt put & if previous if previous not absent, abort
        MapNode node = new MapNode(key, value, cost, size);
        if (data.putIfAbsent(key, node) != null) {
            return false;
        }

        load.addAndGet(size);
        buffer.offer(new Action(AccessType.WRITE, node));
        bufSize.incrementAndGet();

        isEager.lazySet(true);
        asyncDrainIfNeeded();
        return true;
    }

    public void shutDown() {
        pool.shutdownNow();
    }

    abstract void doRead(MapNode node);
    abstract void doWrite(MapNode node);

    abstract void evict();

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

    void drain() {
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
                doRead(a.node);
            } else if (a.type == AccessType.WRITE) {
                evict();
                doWrite(a.node);
            }
        }

        drainActive = false;
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
