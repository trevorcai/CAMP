package test;

import cache.Cache;
import cache.CampCache;
import cache.LruCache;
import cache.concurrent.ConcurrentCampCache;
import cache.concurrent.ConcurrentFakeCache;
import cache.concurrent.ConcurrentLruCache;

public class PerformanceTest {
    public static void main(String[] args) {
        String fname = args[0];
        // TODO Do this with reflection instead of copy/paste
        System.out.println("LruCache");
        for (int i = 0; i < 7; i++) {
            Cache cache = new LruCache(200000000);
            TraceTest test = new TraceTest(cache, fname, 1 << i);
            test.run();
            test.printResultsOneLine();
        }
        System.out.println("CampCache");
        for (int i = 0; i < 7; i++) {
            Cache cache = new CampCache(200000000);
            TraceTest test = new TraceTest(cache, fname, 1 << i);
            test.run();
            test.printResultsOneLine();
        }
        System.out.println("ConcurrentFakeCache");
        for (int i = 0; i < 7; i++) {
            Cache cache = new ConcurrentFakeCache(200000000);
            TraceTest test = new TraceTest(cache, fname, 1 << i);
            test.run();
            test.printResultsOneLine();
        }
        System.out.println("ConcurrentLruCache");
        for (int i = 0; i < 7; i++) {
            Cache cache = new ConcurrentLruCache(200000000);
            TraceTest test = new TraceTest(cache, fname, 1 << i);
            test.run();
            test.printResultsOneLine();
        }
        System.out.println("ConcurrentCampCache");
        for (int i = 0; i < 7; i++) {
            Cache cache = new ConcurrentCampCache(200000000);
            TraceTest test = new TraceTest(cache, fname, 1 << i);
            test.run();
            test.printResultsOneLine();
        }
    }
}
