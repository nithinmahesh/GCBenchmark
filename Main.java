package edu.uwash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    static int threadCount      = 12;
    static int runDurationSec   = 60;
    static int cycleTimeSec     = 0;
    static int memSizeMB        = 10;
    static int baseMemUsage     = 100;
    static ArrayList<Long> maxDiffs = new ArrayList<>();
    static ArrayList<Long> minDiffs = new ArrayList<>();

    static AtomicInteger throughput = new AtomicInteger(0);

    public static void main(String[] args) {
        List<Thread> threadList =  new ArrayList<>();

        for (int i = 0; i < threadCount; i++)
        {
            Thread t = new Thread(Main::singleWorker);
            t.start();
            threadList.add(t);
        }

        threadList.forEach(t ->
        {
            try
            {
                t.join();
            }
            catch (Exception e)
            {

            }
        });

        Collections.sort(maxDiffs);
        Collections.sort(minDiffs);
        System.out.println("Min Diff: " + minDiffs.toArray()[0]);
        System.out.println("Max Diff: " + maxDiffs.toArray()[maxDiffs.size()-1]);
        System.out.println("Total operations: " + throughput.get());
    }

    public static void singleWorker()
    {
        byte[] baseMem = new byte[baseMemUsage * 1024 * 1024];
        long maxDiff = 0;
        long minDiff = Long.MAX_VALUE;
        long totalTime = 0;
        long diff;
        try
        {
            long startTime = System.currentTimeMillis();
            while (totalTime < runDurationSec * 1000)
            {
                {
                    byte[] byteArray = new byte[memSizeMB * 1024 * 1024];
                    Thread.sleep(cycleTimeSec * 1000);
                }
                diff = System.currentTimeMillis() - startTime;
                totalTime += diff;
                maxDiff = Math.max(diff, maxDiff);
                minDiff = Math.min(diff, minDiff);
                throughput.incrementAndGet();
                startTime = System.currentTimeMillis();
            }
        }
        catch (Exception e)
        {

        }

//        System.out.println("MaxDiff: " + maxDiff);
//        System.out.println("MinDiff: " + minDiff);

        maxDiffs.add(maxDiff - cycleTimeSec * 1000);
        minDiffs.add(minDiff - cycleTimeSec * 1000);
    }
}
