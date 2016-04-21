package cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LruCache implements Cache {
    private final Map<String, MapNode> data = new HashMap<>();
    private final DoublyLinkedList<MapNode> lruQueue = new DoublyLinkedList<>();

    private final Lock lock = new ReentrantLock();

    private final int capacity;
    private int load; // Represents amount currently in Cache

    public LruCache(int capacity) {
        this.capacity = capacity;
        load = 0;
    }

    @Override
    public String get(String key) {
        lock.lock();
        MapNode result = data.get(key);
        String value = null;
        if (result != null) {
            lruQueue.moveTail(result);
            value = result.getValue();
        }
        lock.unlock();
        return value;
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        lock.lock();
        // If we already contain key, ignore
        if (data.containsKey(key)) {
            lock.unlock();
            return false;
        }

        load += size;
        while(load > capacity) {
            evict();
        }

        MapNode node = new MapNode(key, value, cost, size);
        data.put(key, node);
        lruQueue.pushTail(node);
        lock.unlock();
        return true;
    }

    @Override
    public void shutDown() {}

    private void evict() {
        MapNode node = lruQueue.popHead();
        if (node == null) {
            return;
        }

        load -= node.getSize();
        data.remove(node.getKey());
    }
}
