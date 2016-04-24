package cache.concurrent;

import cache.Cache;
import cache.MapNode;
import cache.admission.AdmissionPolicy;
import cache.admission.IdlePolicy;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* Design inspiration from ConcurrentLinkedHashMap */
public abstract class ConcurrentCache implements Cache {
    /** Buffer thresholds. */
    private static final int READ_THRESHOLD = 32;
    private static final int READ_MAX_DRAIN = 2 * READ_THRESHOLD;
    private static final int READ_BUFFER_SIZE = 4 * READ_MAX_DRAIN;

    private static final int READ_MASK = READ_BUFFER_SIZE - 1;

    /** The maximum number of write operations to perform per amortized drain. */
    private static final int WRITE_MAX_DRAIN = 16;

    /** Backing Map */
    final Map<String, MapNode> data;

    /** Lock controlling access to the above lruQueue */
    final Lock lock = new ReentrantLock();

    /** Amount of data currently in cache versus what's allowed */
    final int capacity;
    final AtomicInteger load = new AtomicInteger(0);

    /** Buffer and its counter */
    private final AtomicReference<MapNode>[] buffer;
    private long bufferReadPointer = 0;
    private final AtomicLong bufferWritePointer = new AtomicLong();
    private final Queue<MapNode> writeBuffer;

    /** Tracks the status of the drain */
    private boolean drainActive = false;
    private final AtomicBoolean isEager = new AtomicBoolean(false);
    /** Pool on which to run the drain thread */
    private final ExecutorService pool;

    /** Admission Policy */
    private final AdmissionPolicy policy;

    @SuppressWarnings("unchecked")
    public ConcurrentCache(int capacity, int concurrency,
                           AdmissionPolicy policy) {
        this.capacity = capacity;
        this.policy = policy;
        data = new ConcurrentHashMap<>(128, 0.75f, concurrency);
        writeBuffer = new ConcurrentLinkedQueue<>();
        buffer = new AtomicReference[READ_BUFFER_SIZE];
        for (int i = 0; i < READ_BUFFER_SIZE; i++) {
            buffer[i] = new AtomicReference<>();
        }
        pool = Executors.newCachedThreadPool();
    }

    public ConcurrentCache(int capacity, int concurrency) {
        this(capacity, concurrency, IdlePolicy.getInstance());
    }

    @Override
    public String get(String key) {
        MapNode result = data.get(key);
        if (result == null) {
            return null;
        }

        long writePtr = bufferWritePointer.get();
        int index = (int) writePtr & READ_MASK;
        if (buffer[index].compareAndSet(null, result)) {
            bufferWritePointer.incrementAndGet();
        }
        asyncDrainIfNeeded();
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

        long toConsume = bufferWritePointer.get() - bufferReadPointer;

        return toConsume > READ_THRESHOLD;
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
        drainReadBuffer();
        drainWriteBuffer();
    }

    private void drainReadBuffer() {
        int undrained = (int) (bufferWritePointer.get() - bufferReadPointer);
        // Drain at most READ_MAX_DRAIN elements
        int toDrain = (undrained < READ_MAX_DRAIN) ? undrained : READ_MAX_DRAIN;
        for (int i = 0; i < toDrain; i++) {
            int index = (int) bufferReadPointer & READ_MASK;
            MapNode n = buffer[index].get();
            // Shouldn't happen
            if (n == null) {
                break;
            }

            buffer[index].lazySet(null);
            doRead(n);
            bufferReadPointer++;
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
