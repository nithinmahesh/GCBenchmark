package edu.uwash;

import org.HdrHistogram.ConcurrentHistogram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    static int threadCount      = 12;
    static int runDurationSec   = 60;
    static int cycleTimeSec     = 0;
    static int memSizeMB        = 10;
    static int allocationSizeKB = 80;
    static int baseMemUsage     = 100;
    static ConcurrentHistogram histogram = new ConcurrentHistogram(2);

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

        System.out.println("Total operations: " + throughput.get());
        double[] percentiles = {50, 90, 95, 99, 99.9, 99.99, 100};
        for (double percentile : percentiles)
        {
            System.out.println(percentile + " : " + histogram.getValueAtPercentile(percentile));
        }
    }

    public static void singleWorker()
    {
        long workerStartTime = System.currentTimeMillis();
        byte[] baseMem = new byte[baseMemUsage * 1024 * 1024];
        Arrays.fill(baseMem, (byte) 1);
        long startTime;
        List<byte[]> list = new ArrayList<>();
        try
        {
            do
            {
                do
                {
                    startTime = System.currentTimeMillis();
                    byte[] bytes = new byte[allocationSizeKB * 1024];
                    histogram.recordValue(System.currentTimeMillis() - startTime);
                    throughput.incrementAndGet();
                    Arrays.fill( bytes, (byte) 1 );
                    list.add(bytes);
                } while (list.size() * allocationSizeKB < memSizeMB * 1024);
                list.clear();
                Thread.sleep(cycleTimeSec * 1000);
            } while (System.currentTimeMillis() - workerStartTime < runDurationSec * 1000);
        }
        catch (Exception e)
        {

        }
        Arrays.fill(baseMem, (byte) 0);
    }
}
