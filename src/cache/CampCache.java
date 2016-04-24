package cache;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CampCache implements Cache {
    /** MIN_PRIORITY and MAX_PRIORITY contain the minimum and maximum cost-to-size
     * ratios allowed */
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 5000;
    /** Range represents the number of possible index values */
    private static final int RANGE = MAX_PRIORITY - MIN_PRIORITY + 1;

    private final Map<String, MapNode> data = new HashMap<>();
    private final DoublyLinkedList<MapNode>[] lruQueues;
    private final PriorityQueue<MapNode> heap = new PriorityQueue<>();

    private final Lock lock = new ReentrantLock();

    private final int capacity;
    private int load; // Represents amount of data currently in Cache

    private final int precision;

    @SuppressWarnings("unchecked")
    public CampCache(int capacity, int precision) {
        this.capacity = capacity;
        this.precision = precision;
        load = 0;

        // Get around restrictions on Arrays of Java Generics
        lruQueues = new DoublyLinkedList[RANGE];
        for (int i = 0; i < RANGE; i++) {
            lruQueues[i] = new DoublyLinkedList<>();
        }
    }

    /** Initializes CampCache with default precision of 5 */
    public CampCache(int capacity) {
        this(capacity, 5);
    }

    @Override
    public String get(String key) {
        lock.lock();
        MapNode result = data.get(key);
        refresh(result);
        lock.unlock();

        if (result != null) {
            return result.getValue();
        } else {
            return null;
        }
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        lock.lock();
        MapNode node = new MapNode(key, value, cost, size);
        if (data.putIfAbsent(key, node) != null) {
            lock.unlock();
            return false;
        }

        while(load > capacity) {
            evict();
        }

        push(node);
        lock.unlock();

        return true;
    }

    @Override
    public void shutDown() {}

    private void evict() {
        // Get the top of the Heap
        MapNode node = heap.poll();
        if (node == null) {
            return;
        }

        data.remove(node.getKey());
        load -= node.getSize();

        int index = calculatePriority(node.getCost(), node.getSize());
        lruQueues[index].remove(node);
        if (!lruQueues[index].isEmpty()) {
            heap.offer(lruQueues[index].peekHead());
        }
    }

    /** Places a MapNode into heap and correct LRUQueue */
    private void push(MapNode node) {
        if (node == null) {
            return;
        }

        load += node.getSize();
        int index = calculatePriority(node.getCost(), node.getSize());
        boolean isEmpty = lruQueues[index].isEmpty();

        int inflated = getBasePriority() + index;
        node.setOrdering(inflated);

        lruQueues[index].pushTail(node);
        if (isEmpty) {
            heap.offer(lruQueues[index].peekHead());
        }
    }

    private void refresh(MapNode node) {
        if (node == null) {
            return;
        }

        int index = calculatePriority(node.getCost(), node.getSize());
        boolean wasHead = lruQueues[index].isHead(node);

        lruQueues[index].remove(node);
        boolean wasEmpty = lruQueues[index].isEmpty();
        if (wasHead) {
            heap.remove(node);
            if (!wasEmpty) {
                heap.offer(lruQueues[index].peekHead());
            }
        }
        node.setOrdering(getBasePriority() + index);
        lruQueues[index].pushTail(node);
        // NOTE: wasEmpty implies wasHead. This could be simplified
        if (wasHead && wasEmpty) {
            heap.offer(lruQueues[index].peekHead());
        }
    }

    /** Finds the rounded priority for a given cost and size */
    private int calculatePriority(int cost, int size) {
        // Convert costRatio into a priority index
        int priority = cost / (size * MIN_PRIORITY);
        if (priority >= RANGE) {
            priority = RANGE - 1;
        }

        return round(priority);
    }

    /** Rounds a number to a precision, according to CAMP algorithm */
    private int round(int number) {
        // Find the number of trailing bits to zero out
        int numBits = Integer.SIZE - Integer.numberOfLeadingZeros(number);
        int extraBits = 0;
        if (numBits > precision) {
            extraBits = numBits - precision;
        }

        // Zero extraBits trailing bits
        int rounded = Integer.rotateRight(number, extraBits);
        rounded = Integer.rotateLeft(rounded, extraBits);
        return rounded;
    }

    /** Updates the base priority */
    private int getBasePriority() {
        if (heap.isEmpty()) {
            return 0;
        } else {
            return heap.peek().getOrdering();
        }
    }
}
