package cache;

import java.lang.reflect.Array;
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
    private final DoublyLinkedList<String>[] lruQueues;
    private final PriorityQueue<ListNode<String>> heap = new PriorityQueue<>();

    private final Lock lock = new ReentrantLock();

    private final int capacity;
    private int load; // Represents amount of data currently in Cache

    private final int precision;

    public CampCache(int capacity, int precision) {
        this.capacity = capacity;
        this.precision = precision;
        load = 0;

        // Get around restrictions on Arrays of Java Generics
        lruQueues =(DoublyLinkedList<String>[]) Array.newInstance(
                DoublyLinkedList.class, RANGE);
        for (int i = 0; i < RANGE; i++) {
            lruQueues[i] = new DoublyLinkedList<>();
        }
    }

    /** Initializes CampCache with default precision of 5 */
    public CampCache(int capacity) {
        this(capacity, 5);
    }

    @Override
    public String getIfPresent(String key) {
        lock.lock();
        MapNode result = data.get(key);
        refresh(result);
        lock.unlock();

        if (result != null) {
            return result.value;
        } else {
            return null;
        }
    }

    /*
    Places an element into the cache with unit cost/size ratio
     */
    public void put(String key, String value) {
        put(key, value, value.length());
    }

    @Override
    public void put(String key, String value, int cost) {
        lock.lock();
        // Evict if necessary
        while(load > capacity) {
            evict();
        }

        // If we already contain a key, remove the previous value from queue
        // and adjust the size usage
        MapNode prevValue = data.get(key);
        remove(prevValue);

        ListNode<String> listNode = new ListNode<>(key);
        MapNode node = new MapNode(value, cost, listNode);
        data.put(key, node);
        push(node);
        lock.unlock();
    }

    private void evict() {
        // Get the top of the Heap
        ListNode<String> listNode = heap.poll();
        if (listNode == null) {
            return;
        }

        String key = listNode.value;
        MapNode node = data.get(key);
        data.remove(key);

        load -= node.getSize();

        int index = calculatePriority(node.cost, node.getSize());
        lruQueues[index].remove(listNode);
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
        int index = calculatePriority(node.cost, node.getSize());
        boolean isEmpty = lruQueues[index].isEmpty();

        int inflated = getBasePriority() + index;
        node.node.setOrdering(inflated);

        lruQueues[index].pushTail(node.node);
        if (isEmpty) {
            heap.offer(lruQueues[index].peekHead());
        }
    }

    private void refresh(MapNode node) {
        if (node == null) {
            return;
        }

        int index = calculatePriority(node.cost, node.getSize());
        boolean wasHead = lruQueues[index].isHead(node.node);

        lruQueues[index].remove(node.node);
        boolean wasEmpty = lruQueues[index].isEmpty();
        if (wasHead) {
            heap.remove(node.node);
            if (!wasEmpty) {
                heap.offer(lruQueues[index].peekHead());
            }
        }
        node.node.setOrdering(getBasePriority() + index);
        lruQueues[index].pushTail(node.node);
        // NOTE: wasEmpty implies wasHead. This could be simplified
        if (wasHead && wasEmpty) {
            heap.offer(lruQueues[index].peekHead());
        }
    }

    private void remove(MapNode node) {
        if (node == null)
            return;

        load -= node.getSize();
        int index = calculatePriority(node.cost, node.getSize());
        DoublyLinkedList<String> queue = lruQueues[index];
        boolean wasHead = queue.isHead(node.node);
        queue.remove(node.node);
        if (wasHead) {
            heap.remove(node.node);
            if (!queue.isEmpty()) {
                heap.offer(queue.peekHead());
            }
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
