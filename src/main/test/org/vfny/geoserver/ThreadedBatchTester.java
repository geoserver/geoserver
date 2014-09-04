/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


/**
 * ThreadedBatchTester purpose.
 * 
 * <p>
 * Description of ThreadedBatchTester ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: cholmesny $ (last modification)
 * @version $Id$
 */
public class ThreadedBatchTester extends Thread {
    private static int runs = 100;
    private static URL url;
    private static boolean isPost;
    private static String req = "";
    private static File log;
    private static long start;
    private static int finished = 0;
    private static int wait = 2;
	private static String postUrl = "http://localhost:8080/geoserver/wfs";

    public ThreadedBatchTester() {
    }

    public void run() {
        try {
            start = new Date().getTime();

            Thread[] threads = new Thread[runs];

            if (isPost) {
            	url = new URL(postUrl);
                for (int i = 0; i < runs; i++)
                    threads[i] = new TestPostThread(url, req);
            } else {
                if (url == null) {
                    url = new URL(req);
                }

                for (int i = 0; i < runs; i++)
                    threads[i] = new TestGetThread(url);
            }

            for (int i = 0; i < runs; i++) {
                threads[i].start();
                //If the wait time is not set at a few milliseconds then 
                //we get these BindExceptions, or ConnectExceptions.
                //I'm not sure if it's really a problem with the servlet
                //container or this client code...  Seems like there should
                //be someway to tell the threads to maybe try to wait for a
                //bit?
            	sleep(wait);
            }

			while (finished < runs) {
            	sleep(1);
			}

            PrintStream os = System.out;

            if ((log != null) && (log.getAbsoluteFile() != null)) {
                log = log.getAbsoluteFile();
                os = new PrintStream(new FileOutputStream(log));
            }

            generateOutput(threads, os);
        } catch (Exception e) {
            e.printStackTrace();
            usage();
        }
    }

	public static synchronized void threadDone(){
		finished++;
	}

    public static void main(String[] args) {
        try {
            loadArgs(args);

            ThreadedBatchTester tester = new ThreadedBatchTester();
            tester.run();
        } catch (Exception e) {
            e.printStackTrace();
            usage();
        }
    }

    private static void generateOutput(Thread[] threads, PrintStream os) {
        int good = 0;

        for (int i = 0; i < runs; i++) {
            switch (((TestGetThread) threads[i]).getResult()) {
            case HttpURLConnection.HTTP_OK:
                good++;

            default:}
        }

        os.println(good + "/" + runs + " Tests 'OK' ("
            + ((good * 1.0) / (runs * 1.0)) + ")\n");

        for (int i = 0; i < runs; i++) {
            TestGetThread tpt = (TestGetThread) threads[i];

			int result = tpt.getResult();
            if (tpt.getTime2() == null) {
                os.print(result + " Could not connect\n");
            } else if (tpt.getTime3() == null) {
            	os.print(result + " Could not complete read\n");
            } else {
                os.print(tpt.getResult() + ", ");
                os.print(tpt.getTime1().getTime() + ", ");

                if (tpt.getTime2() != null) {
                    os.print(tpt.getTime2().getTime() + ", ");
                }

                if (tpt.getTime3() != null) {
                    os.print(tpt.getTime3().getTime() + ", ");
                    double time = (tpt.getTime3().getTime() - tpt.getTime1().getTime())
                    				/ 1000.0;
                    os.print("time (s): " + time +"\n");
                } else {
                    os.print("null\n");
                }
            }
        }

        long end = new Date().getTime();
        os.println(good + "/" + runs + " Tests 'OK' ("
            + ((good * 1.0) / (runs * 1.0)) + ")\n");

        os.println("Total time (s): " + ((end - start) / 1000.0) + " (start: "
            + start + ", end: " + end + ")");
    }

    private static void loadArgs(String[] args) throws IOException {
        if (args.length == 0) {
            return;
        }

        int i = 0;

		//Is this weird nested structure necessary? ch
        while (i < args.length) {
            String key = args[i++];

            if ("-n".equals(key) && (i < args.length)) {
                String val = args[i++];
                runs = Integer.parseInt(val);
            } else {
                if ("-r".equals(key) && (i < args.length)) {
                    String val = args[i++];
                    File f = new File(val);
                    FileReader fr = new FileReader(f);
                    BufferedReader br = new BufferedReader(fr);
                    String t = "";
					//does this ready loop work here?  It doesn't for 
					//the threads... ch
                    while (br.ready())
                        t += br.readLine();

                    req = t;
                    //this also will get a null for the URL, need to 
                    //specify that somewhere... ch
                } else {
                    if ("-u".equals(key) && (i < args.length)) {
                        String val = args[i++];
                        url = new URL(val);
                    } else {
                        if ("-l".equals(key) && (i < args.length)) {
                            String val = args[i++];
                            log = new File(val);
                        } else {
                            if ("-p".equals(key)) {
                                isPost = true;
                            } else { // usage
                                if ("-w".equals(key) && i < args.length){
                                	wait = Integer.parseInt(args[i++]);
                                } else {
                                   usage();
                            	}
                            }
                        }
                    }
                }
            }
        }
    }

    static void usage() {
        System.out.println("USAGE:\n");
        System.out.println("ThreadedBatchTester [-p][-n][-w] [-r | -u]");
        System.out.println(
            "-n\t Optional\t Number of duplicate requests to create and run.");
        System.out.println(
            "-p\t Optional\t Number of duplicate requests to create and run.");
        System.out.println(
            "-r\t Optional\t Mutually Exclusive with -u\t The file containing the request to execute.");
        System.out.println(
            "-u\t Optional\t Mutually Exclusive with -r\t The URL to execute.");
        System.out.println("-l\t Optional\t The Log file.");
		System.out.println("-w\t Optional\t Amount of time to wait between dispatching requests (in ms)");
    }
}
