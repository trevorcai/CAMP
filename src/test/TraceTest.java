package test;

import cache.Cache;
import cache.LruCache;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TraceTest {
    private final Cache c;
    private final int numThreads;
    private AtomicLong totalCost, missCost, totalAttempt, missAttempt;
    private final ExecutorService pool;
    private final ArrayList<Request> requests = new ArrayList<>();

    private long startTime, endTime;

    public TraceTest(Cache c, String fileName, int numThreads) {
        this.c = c;
        this.numThreads = numThreads;
        pool = Executors.newFixedThreadPool(numThreads);
        totalCost = new AtomicLong();
        missCost = new AtomicLong();
        totalAttempt = new AtomicLong();
        missAttempt = new AtomicLong();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splits = line.split(",");
                int size = Integer.parseInt(splits[2]);
                int cost = Integer.parseInt(splits[3]) * 8400;
                requests.add(new Request(splits[1], size, cost));
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        startTime = System.nanoTime();
        // To get a reasonable runtime, run through requests 20 times each
        for (int i = 0; i < 20; i++) {
            requests.forEach(pool::execute);
        }

        pool.shutdown();
        try {
            pool.awaitTermination(120L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = System.nanoTime();
        c.shutDown();
    }

    public void printResults() {
        double missRatio = missAttempt.doubleValue() / totalAttempt.longValue();
        double costMissRatio = missCost.doubleValue() / totalCost.longValue();
        float timeElapsed = (endTime - startTime) / 1000000;
        System.out.println("Time Elapsed: " + timeElapsed + "ms");
        System.out.println("Miss ratio: " + missRatio);
        System.out.println("Cost-Miss ratio: " + costMissRatio);
    }

    public void printResultsOneLine() {
        double missRatio = missAttempt.doubleValue() / totalAttempt.longValue();
        double costMissRatio = missCost.doubleValue() / totalCost.longValue();
        float timeElapsed = (endTime - startTime) / 1000000;

        System.out.println(numThreads + "," + missRatio + "," + costMissRatio +
                "," + timeElapsed);
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
            String result = c.get(key);
            if (result == null) {
                c.putIfAbsent(key, "", cost, size);

                missAttempt.incrementAndGet();
                missCost.addAndGet(cost);
            }
            totalAttempt.incrementAndGet();
            totalCost.addAndGet(cost);
        }
    }

    public static void main(String[] args) {
        Cache cache = new LruCache(200000000);
        TraceTest test = new TraceTest(cache, args[0], 1);
        System.out.println("Starting test...");
        test.run();
        test.printResults();
    }
}
