package cache.concurrent;

import cache.Cache;
import cache.MapNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** "Fake" cache that never evicts. Backed by CHM */
public class ConcurrentFakeCache implements Cache {
    private final Map<String, MapNode> data;

    public ConcurrentFakeCache(int concurrency) {
        data = new ConcurrentHashMap<>(20000, 0.5f, concurrency);
    }

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
}
