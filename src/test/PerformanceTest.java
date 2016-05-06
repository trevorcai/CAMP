package test;

import cache.Cache;
import cache.CampCache;
import cache.LruCache;
import cache.admission.WeightedAdmission;
import cache.concurrent.ConcurrentCampCache;
import cache.concurrent.ConcurrentLruCache;
import cache.fake.ConcurrentFakeCache;
import cache.fake.IdleCache;
import cache.fake.StripedFakeCache;

public class PerformanceTest {
    public static void main(String[] args) {
        String fname = args[0];
        // TODO Do this with reflection instead of copy/paste
        System.out.println("LruCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new LruCache(200000000);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("CampCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new CampCache(200000000);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("IdleCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new IdleCache();
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("ConcurrentFakeCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new ConcurrentFakeCache(1 << i);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("StripedFakeCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new StripedFakeCache(1 << i);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("ConcurrentLruCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new ConcurrentLruCache(200000000, 1 << i);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("ConcurrentCampCache");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new ConcurrentCampCache(200000000, 1 << i);
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("ConcurrentLruCacheAP");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new ConcurrentLruCache(200000000, 1 << i, new WeightedAdmission());
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
        System.out.println("ConcurrentCampCacheAP");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 25; j++) {
                Cache cache = new ConcurrentCampCache(200000000, 1 << i, new WeightedAdmission());
                TraceTest test = new TraceTest(cache, fname, 1 << i);
                test.run();
                test.printResultsOneLine();
            }
        }
    }
}
