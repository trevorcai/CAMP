package test;

import cache.LruCache;

public class BasicLruTest {
    public static void main(String[] args) {
        LruCache cache = new LruCache(4);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.put("d", "4");

        assert(cache.getIfPresent("a").equals("1"));
        assert(cache.getIfPresent("e") == null);

        cache.put("e", "5");
        assert(cache.getIfPresent("b") == null);
        assert(cache.getIfPresent("e").equals("5"));

        System.out.println("Tests successfully passed");
    }
}
