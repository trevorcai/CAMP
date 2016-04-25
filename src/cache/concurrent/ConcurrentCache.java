package cache.concurrent;

import cache.Cache;
import cache.MapNode;
import cache.admission.AdmissionPolicy;
import cache.admission.IdlePolicy;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* Design inspiration from ConcurrentLinkedHashMap */
public abstract class ConcurrentCache implements Cache {
    private static final int DRAIN_THRESHOLD = 32;
    private static final int READ_MAX_DRAIN = 2 * DRAIN_THRESHOLD;
    private static final int WRITE_MAX_DRAIN = 16;

    /** Backing Map */
    final Map<String, MapNode> data;

    /** Lock controlling access to the above lruQueue */
    final Lock lock = new ReentrantLock();

    /** Amount of data currently in cache versus what's allowed */
    final int capacity;
    final AtomicInteger load = new AtomicInteger(0);

    /** Buffers and their counters */
    private final int numBuffers;
    private final Queue<MapNode>[] buffers;
    private final AtomicInteger[] bufSize;
    private final Queue<MapNode> writeBuffer;

    /** Tracks the status of the drain */
    private boolean drainActive = false;
    private final AtomicBoolean isEager = new AtomicBoolean(false);

    /** Admission Policy */
    private final AdmissionPolicy policy;

    /** Random number generator for which buffer to drain */
    private final Random generator = new Random();

    @SuppressWarnings("unchecked")
    public ConcurrentCache(int capacity, int concurrency,
                           AdmissionPolicy policy) {
        this.capacity = capacity;
        this.policy = policy;
        data = new ConcurrentHashMap<>(128, 0.75f, concurrency);

        writeBuffer = new ConcurrentLinkedQueue<>();
        numBuffers = ceilingNextPowerOfTwo(concurrency);
        buffers = new ConcurrentLinkedQueue[numBuffers];
        bufSize = new AtomicInteger[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            bufSize[i] = new AtomicInteger();
            buffers[i] = new ConcurrentLinkedQueue<>();
        }
    }

    public ConcurrentCache(int capacity, int concurrency) {
        this(capacity, concurrency, IdlePolicy.getInstance());
    }

    private static int ceilingNextPowerOfTwo(int x) {
        // From CLHM source code
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
    }

    @Override
    public String get(String key) {
        MapNode result = data.get(key);
        if (result == null) {
            return null;
        }

        int bufIndex = getBufferIndex();
        buffers[bufIndex].offer(result);
        bufSize[bufIndex].incrementAndGet();
        if (shouldDrain(bufIndex)) {
            tryDrain();
        }
        return result.getValue();
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        // Attempt put & if previous if previous not absent, abort
        MapNode node = new MapNode(key, value, cost, size);
        if (!policy.shouldAdmit(node)) {
            return false;
        }
        if (data.putIfAbsent(key, node) != null) {
            return false;
        }

        policy.registerWrite(node);
        writeBuffer.offer(node);
        isEager.lazySet(true);
        if (shouldDrain(-1)) {
            tryDrain();
        }
        return true;
    }

    abstract void doRead(MapNode node);
    abstract void doWrite(MapNode node);
    abstract void evict();

    private int getBufferIndex() {
        return (int) Thread.currentThread().getId() & (numBuffers - 1);
    }

    /** Checks if buffer should be drained */
    private boolean shouldDrain(int idx) {
        if (drainActive) {
            return false;
        } else if (isEager.get()) {
            return true;
        }

        if (idx > 0) {
            long toConsume = bufSize[idx].get();
            return toConsume > DRAIN_THRESHOLD;
        } else {
            return false;
        }
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

    private void drain() {
        drainReadBuffers();
        drainWriteBuffer();
    }

    private void drainReadBuffers() {
        int idx = generator.nextInt(numBuffers);
        for (int i = 0; i < numBuffers; i++) {
            drainReadBuffer((idx + i) & (numBuffers - 1));
        }
    }

    private void drainReadBuffer(int idx) {
        // Drain at most READ_MAX_DRAIN elements
        for (int i = 0; i < READ_MAX_DRAIN; i++) {
            MapNode n = buffers[idx].poll();
            if (n == null) {
                break;
            } else if (n.isEvicted()) {
                continue;
            }
            doRead(n);
        }
    }

    private void drainWriteBuffer() {
        drainActive = true;
        isEager.lazySet(false);

        for(int i = 0; i < WRITE_MAX_DRAIN; i++) {
            MapNode n = writeBuffer.poll();
            if (n == null) {
                break;
            }
            load.addAndGet(n.getSize());
            evict();
            doWrite(n);
        }

        drainActive = false;
    }
}
