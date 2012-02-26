package org.geoserver.flow.controller;

import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;

public class FlowControllerTestingThread extends Thread {
    enum ThreadState {STARTED, TIMED_OUT, PROCESSING, COMPLETE}; 
    
    FlowController controller;
    boolean proceed;
    Request request;
    long timeout;
    long processingDelay;
    ThreadState state;
    Throwable error;
    
    
    public FlowControllerTestingThread(FlowController controller, Request request, long timeout, long processingDelay) {
        this.controller = controller;
        this.request = request;
        this.timeout = timeout;
        this.processingDelay = processingDelay;
    }

    @Override
    public void run() {
        state = ThreadState.STARTED;
        try {
            System.out.println(this + " calling requestIncoming");
            if(!controller.requestIncoming(request, timeout)) {
                state = ThreadState.TIMED_OUT;
                return;
            }
        } catch(Throwable t) {
            this.error = t;
        }
        state = ThreadState.PROCESSING;
        
        try {
            System.out.println(this + " waiting");
            if(processingDelay > 0)
                sleep(processingDelay);
        } catch(InterruptedException e) {
            System.out.println(e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        }
        
        try {
            System.out.println(this + " calling requestComplete");
            controller.requestComplete(request);
        } catch(Throwable t) {
            this.error = t;
        }
        state = ThreadState.COMPLETE;
        System.out.println(this + " done");
    }
}
