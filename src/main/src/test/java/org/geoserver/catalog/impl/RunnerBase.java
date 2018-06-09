/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class RunnerBase implements Runnable {

    private CountDownLatch ready;
    private CountDownLatch done;
    private Exception problem;

    protected RunnerBase(CountDownLatch ready, CountDownLatch done) {
        this.ready = ready;
        this.done = done;
    }

    public void run() {
        boolean readied = false;
        try {
            doBeforeReady();
            if (ready != null) {
                ready.countDown();
                readied = true;
                ready.await();
            }
            runInternal();
        } catch (Exception e) {
            if (ready != null && !readied) {
                try {
                    ready.countDown();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            setProblem(e);
        }
        if (done != null) {
            done.countDown();
        }
    }

    protected abstract void runInternal() throws Exception;

    protected void doBeforeReady() {
        ;
    }

    private synchronized void setProblem(Exception problem) {
        this.problem = problem;
    }

    public synchronized Exception getProblem() {
        return problem;
    }

    public static void checkForRunnerException(RunnerBase runner) throws Exception {
        Exception problem = runner.getProblem();
        if (problem != null) {
            throw problem;
        }
    }

    public static void checkForRunnerExceptions(List<? extends RunnerBase> runners)
            throws Exception {
        for (RunnerBase runner : runners) {
            checkForRunnerException(runner);
        }
    }
}
