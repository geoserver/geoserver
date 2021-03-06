/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PipeliningTaskQueueTest {
    static final Logger LOGGER = Logging.getLogger(PipeliningTaskQueueTest.class);
    PipeliningTaskQueue<Integer> taskQueue;

    @Before
    public void setUp() throws Exception {
        taskQueue = new PipeliningTaskQueue<>();
        taskQueue.start();
    }

    @After
    public void tearDown() throws Exception {
        taskQueue.stop();
    }

    @Test
    public void test() throws Exception {

        ConcurrentLinkedQueue<Worker> completed = new ConcurrentLinkedQueue<>();
        int groups = 5;
        List<List<Worker>> workers = new ArrayList<>();
        for (int i = 0; i < groups; i++) {
            List<Worker> list = new ArrayList<>();
            for (int j = 0; j < groups; j++) {
                list.add(new Worker(i, j, completed));
            }
            workers.add(list);
        }

        for (int i = 0; i < groups; i++) {
            for (int j = 0; j < groups; j++) {
                Worker w = workers.get(j).get(i);
                taskQueue.execute(w.group, w);
            }
        }

        while (completed.size() < groups * groups) {
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
                Thread.sleep(x * 100);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "", e);
            }

            completed.add(this);
        }
    }
}
