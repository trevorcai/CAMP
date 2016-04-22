package cache.concurrent;

import cache.DoublyLinkedList;
import cache.MapNode;

import java.util.PriorityQueue;

public class ConcurrentCampCache extends ConcurrentCache {
    /** MIN_PRIORITY and MAX_PRIORITY contain the minimum and maximum cost-to-size
     * ratios allowed */
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 5000;
    /** Range represents the number of possible index values */
    private static final int RANGE = MAX_PRIORITY - MIN_PRIORITY + 1;

    /** Eviction data structures */
    private final DoublyLinkedList<MapNode>[] lruQueues;
    private final PriorityQueue<MapNode> heap;

    /** Precision */
    private final int precision;

    @SuppressWarnings("unchecked")
    public ConcurrentCampCache(int capacity, int precision) {
        super(capacity);
        this.precision = precision;
        lruQueues = new DoublyLinkedList[RANGE];
        for (int i = 0; i < RANGE; i++) {
            lruQueues[i] = new DoublyLinkedList<>();
        }
        heap = new PriorityQueue<>();
    }

    public ConcurrentCampCache(int capacity) {
        this(capacity, 5);
    }

    @Override
    void doRead(MapNode node) {
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

    @Override
    void doWrite(MapNode node) {
        if (node == null) {
            return;
        }

        int index = calculatePriority(node.getCost(), node.getSize());
        boolean isEmpty = lruQueues[index].isEmpty();

        int inflated = getBasePriority() + index;
        node.setOrdering(inflated);

        lruQueues[index].pushTail(node);
        if (isEmpty) {
            heap.offer(node);
        }
    }

    @Override
    void evict() {
        if (!canAndShouldEvict() || !lock.tryLock()) {
            return;
        }

        boolean flag = false;
        while (shouldEvict()) {
            while (canAndShouldEvict()) {
                flag = false;
                evictOne();
            }
            if (shouldEvict()) {
                if (flag) {
                    break;
                }
                drain();
                flag = true;
            }
        }
        lock.unlock();
    }

    private void evictOne() {
        // Get the top of the Heap
        MapNode node = heap.poll();
        if (node == null) {
            return;
        }

        node.setEvicted();
        String key = node.getKey();
        if (data.containsKey(key)) {
            data.remove(key);
            load.addAndGet(-1 * node.getSize());
            int index = calculatePriority(node.getCost(), node.getSize());
            lruQueues[index].remove(node);
            if (!lruQueues[index].isEmpty()) {
                heap.offer(lruQueues[index].peekHead());
            }
        }
    }

    private boolean canAndShouldEvict() {
        return shouldEvict() && !heap.isEmpty();
    }

    private boolean shouldEvict() {
        return load.intValue() > capacity;
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
