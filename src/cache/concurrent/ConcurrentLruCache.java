package cache.concurrent;

import cache.DoublyLinkedList;
import cache.MapNode;

public class ConcurrentLruCache extends ConcurrentCache {
    private final DoublyLinkedList<MapNode> lruQueue = new DoublyLinkedList<>();
    public ConcurrentLruCache(int capacity, int concurrency) {
        super(capacity, concurrency);
    }

    @Override
    void doRead(MapNode node) {
        lruQueue.moveTail(node);
    }

    @Override
    void doWrite(MapNode node) {
        lruQueue.pushTail(node);
    }

    /** Evicts until properly sized. */
    @Override
    void evict() {
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
}

