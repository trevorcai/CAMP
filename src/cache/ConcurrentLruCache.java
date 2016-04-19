package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLruCache implements Cache {
    static final int DRAIN_THRESHOLD = 40;

    private final Map<String, MapNode> data;
    private final DoublyLinkedList<String> lruQueue = new DoublyLinkedList<>();

    /** Buffers and their counters */
    private final ConcurrentLinkedQueue<Action> buffer;
    private AtomicInteger bufSize = new AtomicInteger(0);

    private final Lock lock = new ReentrantLock();

    private final int capacity;
    /** Amount of data currently in Cache */
    private AtomicInteger load = new AtomicInteger(0);

    public ConcurrentLruCache(int capacity) {
        this.capacity = capacity;
        data = new ConcurrentHashMap<>();
        buffer = new ConcurrentLinkedQueue<>();
    }

    @Override
    public String getIfPresent(String key) {
        MapNode result = data.get(key);
        String value = null;

        if (result != null) {
            buffer.offer(new Action(AccessType.READ, result.node));
            bufSize.incrementAndGet();
            value = result.value;
        }

        return value;
    }

    /*
    Places an element into the cache with unit cost/size ratio
     */
    public void put(String key, String value) {
        put(key, value, value.length(), value.length());
    }

    @Override
    public void put(String key, String value, int cost, int size) {
        // If previous value exists, remove it
        MapNode prevValue = data.get(key);
        if (prevValue != null) {
            load.addAndGet(-1 * prevValue.getSize());
            lock.lock();
            lruQueue.remove(prevValue.node);
            lock.unlock();
        }

        if (shouldDrain()) {
            drain();
        }
        int newLoad = load.addAndGet(size);
        if (newLoad > capacity) {
            lock.lock();
            while (newLoad > capacity) {
                int evicted = evict();
                newLoad -= evicted;
            }
            lock.unlock();
        }

        // If we already contain a key, remove the previous value from queue
        // and adjust the size usage
        ListNode<String> listNode = new ListNode<>(key);
        MapNode node = new MapNode(value, cost, size, listNode);
        data.put(key, node);

        buffer.offer(new Action(AccessType.WRITE, listNode));
        bufSize.incrementAndGet();
    }

    /** Checks if buffer should be drained */
    private boolean shouldDrain() {
        return bufSize.intValue() > DRAIN_THRESHOLD;
    }

    /** Tries to drain buffer, if someone else isn't already draining it */
    private void drain() {
        boolean success = lock.tryLock();
        if (!success) {
            return;
        }

        // Drain elements according to number that was in the buffer originally
        int toDrain = bufSize.intValue();
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

        bufSize.addAndGet(-1 * toDrain);
        lock.unlock();
    }

    /** Evicts an entry. Expects to hold lock. */
    private int evict() {
        ListNode<String> listNode = lruQueue.popHead();
        if (listNode == null) {
            return 0;
        }

        listNode.setEvicted();
        String key = listNode.value;
        MapNode node = data.get(key);
        if (node == null) {
            return 0;
        }
        load.addAndGet(-1 * node.getSize());
        data.remove(key);

        return node.getSize();
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
