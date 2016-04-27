package cache.fake;

import cache.Cache;
import cache.MapNode;

import java.util.HashMap;
import java.util.Map;

/** "Fake" cache that never evicts. Backed by CHM */
public class StripedFakeCache implements Cache {
    private final Map<String, MapNode>[] data;
    private final int numBuffers;

    private static int ceilingNextPowerOfTwo(int x) {
        // From CLHM source code
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
    }

    @SuppressWarnings("unchecked")
    public StripedFakeCache(int concurrency) {
        numBuffers = ceilingNextPowerOfTwo(concurrency);
        data = new HashMap[numBuffers];
        for (int i = 0; i < numBuffers; i ++) {
            data[i] = new HashMap<>(20000 / numBuffers);
        }
    }

    @Override
    public String get(String key) {
        int index = getBufferIndex();
        MapNode result = data[index].get(key);
        return (result != null) ? result.getValue() : null;
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        int index = getBufferIndex();
        if (data[index].containsKey(key)) {
            return false;
        }
        MapNode n = new MapNode(key, value, 0, 0);
        data[index].put(key, n);
        return true;
    }

    private int getBufferIndex() {
        return (int) Thread.currentThread().getId() & (numBuffers - 1);
    }
}
