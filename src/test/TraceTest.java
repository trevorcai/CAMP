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

public class TraceTest {
    private final Cache c;
    private final int numThreads;
    private long totalCost, missCost, totalAttempt, missAttempt;
    private final ExecutorService pool;
    private final ArrayList<Request> requests = new ArrayList<>();

    private long elapsedTime;

    public TraceTest(Cache c, String fileName, int numThreads) {
        this.c = c;
        this.numThreads = numThreads;
        pool = Executors.newFixedThreadPool(numThreads);

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
        // To get a reasonable runtime, run through requests 20 times each
        long startTime = System.currentTimeMillis();
        requests.forEach(pool::execute);

        pool.shutdown();
        try {
            pool.awaitTermination(120L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;

        c.shutDown();
        collectResults();
    }

    public void printResults() {
        double missRatio = (double) missAttempt / totalAttempt;
        double costMissRatio = (double) missCost / totalCost;
        System.out.println("Time Elapsed: " + elapsedTime + "ms");
        System.out.println("Miss ratio: " + missRatio);
        System.out.println("Cost-Miss ratio: " + costMissRatio);
    }

    public void printResultsOneLine() {
        double missRatio = (double) missAttempt / totalAttempt;
        double costMissRatio = (double) missCost / totalCost;

        System.out.println(numThreads + "," + missRatio + "," + costMissRatio +
                "," + elapsedTime);
    }

    private void collectResults() {
        totalCost = 0;
        missCost = 0;
        totalAttempt = 0;
        missAttempt = 0;
        for (Request r : requests) {
            totalCost += r.totalCost;
            missCost += r.missCost;
            totalAttempt += r.totalAttempt;
            missAttempt += r.missAttempt;
        }
    }

    private class Request implements Runnable {
        private final String key;
        private final int cost, size;
        private long totalCost, missCost, totalAttempt, missAttempt;

        public Request(String key, int size, int cost) {
            this.key = key;
            this.size = size;
            this.cost = cost;
            totalCost = 0;
            missCost = 0;
            totalAttempt = 0;
            missAttempt = 0;
        }

        @Override
        public void run() {
            String result = c.get(key);
            if (result == null) {
                c.putIfAbsent(key, "", cost, size);

                missAttempt++;
                missCost += cost;
            }
            totalAttempt++;
            totalCost += cost;
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
