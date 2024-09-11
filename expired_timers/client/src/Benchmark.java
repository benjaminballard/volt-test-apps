/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * This class can be customized for simple tests or micro-benchmarks by modifying the
 * benchmarkItem method which generates random parameter values and calls a procedure
 *
 */
package org.voltdb.example;

import java.util.Random;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;
import org.voltdb.example.util.BenchmarkCallback;
import org.voltdb.example.util.BenchmarkStats;

public class Benchmark {

    private Client client;
    private BenchmarkStats stats;
    private Random rand = new Random();
    private int benchmarkSize;

    // For data gen
    String ipPrefix = "10.15.52.";

    // String[] affinities = {"10.15.52.8.clientgx.com",
    //                        "mp6.vdrfpp2n2-imscore.test.orange-ims.fr.test.orange-ims.fr",
    //                        "mp6.vdrfpp2n1-imscore.test.orange-ims.fr.test.orange-ims.fr",
    //                        "mp5.vdrfpp2n2-imscore.test.orange-ims.fr.test.orange-ims.fr",
    //                        "mp5.vdrfpp2n1-imscore.test.orange-ims.fr.test.orange-ims.fr"};
    String[] affinities = {
                           "mp6.vdrfpp2n2-imscore.test.orange-ims.fr.test.orange-ims.fr",
                           "mp6.vdrfpp2n1-imscore.test.orange-ims.fr.test.orange-ims.fr",
                           "mp5.vdrfpp2n2-imscore.test.orange-ims.fr.test.orange-ims.fr",
                           "mp5.vdrfpp2n1-imscore.test.orange-ims.fr.test.orange-ims.fr"};
    // String[] affinities = {"10.15.52.8.clientgx.com"};

    String[] timers = {"GX_BU_TIMEOUT","RX_BU_TIMEOUT"};
    int timerRange = 8_000;
    int entryRange = 100_000_000;
    String[] failoverFilter = {"CLA"};
    int queryLimit = 40;

    public Benchmark(String servers, int size) throws Exception {

        this.benchmarkSize = size;

        // create a java client instance using default settings
        ClientConfig config = new ClientConfig();
        config.setMaxOutstandingTxns(50);
        client = ClientFactory.createClient(config);

        // connect to each server listed (separated by commas) in the first argument
        String[] serverArray = servers.split(",");
        for (String server : serverArray) {
            client.createConnection(server);
        }

        // This helper class is used to capture client statistics
        stats = new BenchmarkStats(client, true);
    }

    public void benchmarkItem() throws Exception {
        for (int i=0; i<10; i++) {
            insertTimer();
        }
        getExpiredTimers();
    }

    public void truncateTimer() throws Exception {
        client.callProcedure("@AdHoc", "truncate table timer;");
    }


    public void insertTimer() throws Exception {
        String procName = "insert_timer";
        ProcedureCallback callback = new BenchmarkCallback(procName);

        String ip = "10.15.52." + (5+ rand.nextInt(5));

        String documentId = ip + ";Gx_WIFI;" + rand.nextInt(100_000_000);
        String timerName = timers[rand.nextInt(timers.length)];
        int timerCode = rand.nextInt(timerRange);
        String entryId = "Gx-" + ip + ";Gx_WIFI;" + rand.nextInt(entryRange);
        String affinity = ip + ".clientgx.com";
        String context = null;
        String lastActiveClusterId = "CLA";

        client.callProcedure(callback,
                             procName,
                             documentId,
                             timerName,
                             timerCode,
                             entryId,
                             affinity,
                             context,
                             lastActiveClusterId
                             );

    }

    public void getExpiredTimers() throws Exception {
        String procName = "GetExpiredTimersWithFilterWithAffinity";
        //ProcedureCallback callback = new BenchmarkCallback(procName);

        int rangeStart = 0;
        int rangeStop = 7999;


        String partitionKey = rand.nextInt(1000)+"FOO";

        client.callProcedure( //callback,
                             procName,
                             partitionKey,
                             rangeStart,
                             rangeStop,
                             failoverFilter,
                             affinities,
                             queryLimit
                             );

    }


    public void runBenchmark() throws Exception {

        // print a heading
        String dashes = new String(new char[80]).replace("\0", "-");
        System.out.println(dashes);
        System.out.println(" Loading " + benchmarkSize + " Records");
        System.out.println(dashes);

        // truncate table first
        truncateTimer();

        // start recording statistics for the benchmark, outputting every 5 seconds
        stats.startBenchmark();
        for (int i=0; i<benchmarkSize; i++) {
            insertTimer();
        }
        client.drain();
        stats.endBenchmark();

        int queryCount = 100;
        System.out.println(dashes);
        System.out.println(" Running " + queryCount + " Queries");
        System.out.println(dashes);

        stats.startBenchmark();
        for (int i=0; i<queryCount; i++) {
            getExpiredTimers();
        }
        client.drain();
        stats.endBenchmark();

        // wait for any outstanding responses to return before closing the client
        client.close();

        // print the transaction results tracked by BenchmarkCallback
        BenchmarkCallback.printAllResults();
    }


    public static void main(String[] args) throws Exception {

        // the first parameter can be a comma-separated list of hostnames or IPs
        String serverlist = "localhost";
        if (args.length > 0) {
            serverlist = args[0];
        }

        // the second parameter can be the number of transactions to execute
        int transactions = 5_000_000;
        if (args.length > 1) {
            transactions = Integer.parseInt(args[1]);
        }

        Benchmark benchmark = new Benchmark(serverlist, transactions);
        benchmark.runBenchmark();

    }
}
