package cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LruCache implements Cache {
    private final Map<String, MapNode> data = new HashMap<>();
    private final DoublyLinkedList<String> lruQueue = new DoublyLinkedList<>();

    private final Lock lock = new ReentrantLock();

    private final int capacity;
    private int load; // Represents amount currently in Cache

    public LruCache(int capacity) {
        this.capacity = capacity;
        load = 0;
    }

    @Override
    public String getIfPresent(String key) {
        lock.lock();
        MapNode result = data.get(key);
        String value = null;
        if (result != null) {
            lruQueue.moveTail(result.node);
            value = result.value;
        }
        lock.unlock();
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
        lock.lock();
        load += size;
        // Evict if necessary
        while(load > capacity) {
            evict();
        }

        // If we already contain a key, remove the previous value from queue
        // and adjust the size usage
        MapNode prevValue = data.get(key);
        if (prevValue != null) {
            load -= prevValue.getSize();
            lruQueue.remove(prevValue.node);
        }

        ListNode<String> listNode = new ListNode<>(key);
        MapNode node = new MapNode(value, cost, size, listNode);
        data.put(key, node);
        lruQueue.pushTail(listNode);
        lock.unlock();
    }

    private void evict() {
        ListNode<String> listNode = lruQueue.popHead();
        if (listNode == null) {
            return;
        }

        String key = listNode.value;
        MapNode node = data.get(key);
        load -= node.getSize();
        data.remove(key);
    }
}
