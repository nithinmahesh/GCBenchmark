using HdrHistogram;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace GCBenchmark
{
    class Program
    {
        private static readonly int runDurationSec   = 60;
        private static readonly int cycleTimeSec     = 0;

        // Specify the amount of memory pressure we'll create in this test case
        // Total memory pressure at any point in time will be equal to
        // threadCount * (baseMemUsage*1024*1024 + memSize*1024*1024)
        private static readonly int threadCount      = 12;
        private static readonly int memSizeMB        = 10;
        private static readonly int allocationSizeKB = 80;
        private static readonly int baseMemUsage     = 100;

        private static HistogramBase histogram = new LongConcurrentHistogram(1, TimeStamp.Minutes(10), 3);

        static int throughput = 0;

        static void Main(string[] args)
        {
            List<Thread> threadList = new List<Thread>();

            for (int i = 0; i < threadCount; i++)
            {
                Thread t = new Thread(WorkerJob);
                t.Start();
                threadList.Add(t);
            }

            foreach (Thread t in threadList)
            {
                t.Join();
            }

            Console.WriteLine("Total operations: " + throughput);
            double[] percentiles = new double[] { 50, 90, 95, 99, 99.9, 99.99, 100 };
            foreach (double percentile in percentiles)
            {
                Console.WriteLine($"{percentile} : {histogram.GetValueAtPercentile(percentile)}");
            }

            Console.WriteLine("Results:");

            // Write the results to console
            histogram.OutputPercentileDistribution(
                Console.Out,
                percentileTicksPerHalfDistance: 3,
                outputValueUnitScalingRatio: OutputScalingFactor.TimeStampToMilliseconds);

            // Save the results to a file
            using (StreamWriter writer = new StreamWriter("HistogramResults.hgrm"))
            {
                histogram.OutputPercentileDistribution(
                    writer,
                    percentileTicksPerHalfDistance: 3,
                    outputValueUnitScalingRatio: OutputScalingFactor.TimeStampToMilliseconds);
            }

            Console.WriteLine();
            Console.WriteLine("These results have been saved to HistogramResults.hgrm");
        }

        static void WorkerJob()
        {
            // Use a stopwatch to count how much time has elapsed
            // so we can exit the loop
            Stopwatch sw = new Stopwatch();
            sw.Start();

            byte[] baseMem = new byte[baseMemUsage * 1024 * 1024];
            List<byte[]> list = new List<byte[]>();

            while (sw.ElapsedMilliseconds < runDurationSec * 1000)
            {
                while (list.Count * allocationSizeKB < memSizeMB * 1024)
                {
                    // Surround the byte array allocation (to approximate pause time) with extremely
                    // accurate tick counters
                    long startTimestamp = Stopwatch.GetTimestamp();

                    byte[] byteArray = new byte[allocationSizeKB * 1024];

                    histogram.RecordValue(Stopwatch.GetTimestamp() - startTimestamp);
                    Interlocked.Increment(ref throughput);
                    list.Add(byteArray);
                }

                list.Clear();

                // Sleep the thread for the desired # of seconds
                Thread.Sleep(cycleTimeSec * 1000);
            }

            sw.Stop();
            Array.Clear(baseMem, 0, baseMem.Length);
        }
    }
}
