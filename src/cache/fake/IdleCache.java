package cache.fake;

import cache.Cache;

public class IdleCache implements Cache {
    @Override
    public String get(String key) {
        return "";
    }

    @Override
    public boolean putIfAbsent(String key, String value, int cost, int size) {
        return true;
    }
}
