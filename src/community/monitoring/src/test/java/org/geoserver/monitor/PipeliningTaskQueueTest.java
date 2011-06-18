package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.TestCase;

public class PipeliningTaskQueueTest extends TestCase {

    static {
//        java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
//        handler.setLevel(java.util.logging.Level.ALL);
//
//        org.geotools.util.logging.Logging.getLogger("org.geoserver.monitor").setLevel(java.util.logging.Level.ALL);
//        org.geotools.util.logging.Logging.getLogger("org.geoserver.monitor").addHandler(handler);
    }
    
    PipeliningTaskQueue<Integer> taskQueue;
    
    @Override
    protected void setUp() throws Exception {
        taskQueue = new PipeliningTaskQueue();
        taskQueue.start();
    }
    
    @Override
    protected void tearDown() throws Exception {
        taskQueue.stop();
    }
    
    public void test() throws Exception {
        
        ConcurrentLinkedQueue<Worker> completed = new ConcurrentLinkedQueue<Worker>();
        int groups = 5;
        ArrayList<Worker>[] workers = new ArrayList[groups];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new ArrayList();
            for (int j = 0; j < groups; j++) {
                workers[i].add(new Worker(i,j, completed));
            }
        }
        
        for (int i = 0; i < groups; i++) {
            for (int j = 0; j < workers.length; j++) {
                Worker w = workers[j].get(i);
                taskQueue.execute(w.group, w); 
            }
        }
        
        
        while(completed.size() < groups * workers.length) {
            Thread.sleep(1000);
        }
        
        int[] status = new int[groups];
        for (Worker w : completed) {
            assertEquals(status[w.group], w.seq.intValue());
            status[w.group]++;
        }
    
    }
    
    static class Worker implements Runnable {
        
        Integer group;
        Integer seq;
        Queue<Worker> completed;
        
        public Worker(Integer group, Integer seq, Queue<Worker> completed) {
            this.group = group;
            this.seq = seq;
            this.completed = completed;
        }

        public void run() {
            Random r = new Random();
            int x = r.nextInt(10) + 1;
            try {
                Thread.sleep(x*100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            completed.add(this);
        }        
    }
}
