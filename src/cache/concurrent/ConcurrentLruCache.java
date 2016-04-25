package cache.concurrent;

import cache.DoublyLinkedList;
import cache.MapNode;
import cache.admission.AdmissionPolicy;

public class ConcurrentLruCache extends ConcurrentCache {
    private final DoublyLinkedList<MapNode> lruQueue = new DoublyLinkedList<>();
    public ConcurrentLruCache(int capacity, int concurrency) {
        super(capacity, concurrency);
    }

    public ConcurrentLruCache(int capacity, int concurrency,
                              AdmissionPolicy policy) {
        super(capacity, concurrency, policy);
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
        if (!shouldEvict() || !lock.tryLock()) {
            return;
        }

        while (shouldEvict()) {
            evictOne();
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

    private boolean shouldEvict() {
        return (load.intValue() > capacity) && !lruQueue.isEmpty();
    }
}

