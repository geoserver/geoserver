/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.util.concurrent.CountDownLatch;
import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;

public class FlowControllerTestingThread extends Thread {
    enum ThreadState {
        STARTED,
        TIMED_OUT,
        PROCESSING,
        COMPLETE
    };

    FlowController[] controllers;
    boolean proceed;
    Request request;
    long timeout;
    long processingDelay;
    ThreadState state;
    Throwable error;
    CountDownLatch waitLatch;

    public FlowControllerTestingThread(
            Request request, long timeout, long processingDelay, FlowController... controllers) {
        this.controllers = controllers;
        this.request = request;
        this.timeout = timeout;
        this.processingDelay = processingDelay;
    }

    public void setWaitLatch(CountDownLatch waitLatch) {
        this.waitLatch = waitLatch;
    }

    @Override
    public void run() {
        state = ThreadState.STARTED;
        try {
            // System.out.println(this + " calling requestIncoming");
            for (FlowController controller : controllers) {
                if (!controller.requestIncoming(request, timeout)) {
                    state = ThreadState.TIMED_OUT;
                    return;
                }
            }
        } catch (Throwable t) {
            this.error = t;
        }
        state = ThreadState.PROCESSING;

        try {
            // wait on wait latch if available
            if (waitLatch != null) {
                // System.out.println(this + " waiting on wait latch");
                waitLatch.await();
            }
            // System.out.println(this + " waiting");
            if (processingDelay > 0) {
                sleep(processingDelay);
            }
        } catch (InterruptedException e) {
            // System.out.println(e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        }

        try {
            // System.out.println(this + " calling requestComplete");
            for (FlowController controller : controllers) {
                controller.requestComplete(request);
            }
        } catch (Throwable t) {
            this.error = t;
        }
        state = ThreadState.COMPLETE;
        // System.out.println(this + " done");
    }
}
