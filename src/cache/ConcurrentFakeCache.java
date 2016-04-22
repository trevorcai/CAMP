package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** "Fake" cache that never evicts. Backed by CHM */
public class ConcurrentFakeCache implements Cache {
    private final Map<String, MapNode> data = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public ConcurrentFakeCache(int capacity) {}

    @Override
    public String get(String key) {
        MapNode result = data.get(key);
        return (result != null) ? result.getValue() : null;
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        if (data.containsKey(key)) {
            return false;
        }
        MapNode n = new MapNode(key, value, 0, 0);
        data.put(key, n);
        return true;
    }

    @Override
    public void shutDown() {}
}
