package test;

import cache.Cache;
import cache.CampCache;
import cache.LruCache;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TraceTest {
    private final Cache c;
    private final String fileName;
    private AtomicLong totalCost, missCost, totalAttempt, missAttempt;
    public final ExecutorService pool;

    public TraceTest(Cache c, String fileName, int numThreads) {
        this.c = c;
        this.fileName = fileName;
        pool = Executors.newFixedThreadPool(numThreads);
        totalCost = new AtomicLong();
        missCost = new AtomicLong();
        totalAttempt = new AtomicLong();
        missAttempt = new AtomicLong();
    }

    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splits = line.split(",");
                int size = Integer.parseInt(splits[2]);
                int cost = Integer.parseInt(splits[3]);
                pool.execute(new Request(splits[1], size, cost));
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        pool.shutdown();
    }

    public void printResults() {
        double missRatio = missAttempt.doubleValue() / totalAttempt.longValue();
        double costMissRatio = missCost.doubleValue() / totalCost.longValue();
        System.out.println("Miss ratio: " + missRatio);
        System.out.println("Cost-Miss ratio: " + costMissRatio);
    }

    private class Request implements Runnable {
        private final String key;
        private final int cost, size;

        public Request(String key, int size, int cost) {
            this.key = key;
            this.size = size;
            this.cost = cost;
        }

        @Override
        public void run() {
            String result = c.getIfPresent(key);
            if (result == null) {
                c.put(key, "", cost, size);

                missAttempt.incrementAndGet();
                missCost.addAndGet(cost);
            }
            totalAttempt.incrementAndGet();
            totalCost.addAndGet(cost);
        }
    }

    public static void main(String[] args) {
        int numCores = Integer.parseInt(args[0]);
        System.out.println(numCores);
        System.out.println(args[1]);
        Cache cache = new CampCache(200000000);
        TraceTest test = new TraceTest(cache, args[1], numCores);
        test.run();
        try {
            test.pool.awaitTermination(120L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        test.printResults();
    }
}
