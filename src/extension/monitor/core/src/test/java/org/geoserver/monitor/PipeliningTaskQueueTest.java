/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

        long submitStart = System.currentTimeMillis();
        for (int i = 0; i < groups; i++) {
            for (int j = 0; j < groups; j++) {
                Worker w = workers.get(j).get(i);
                taskQueue.execute(w.group, w);
            }
        }
        long submitEnd = System.currentTimeMillis();

        int totalTasks = groups * groups;
        try {
            await().atMost(60, SECONDS).until(() -> completed.size() >= totalTasks);
        } catch (Exception e) {
            // Capture diagnostic state on timeout
            long elapsed = System.currentTimeMillis() - submitEnd;
            StringBuilder diag = new StringBuilder();
            diag.append("\n=== PipeliningTaskQueueTest TIMEOUT DIAGNOSTICS ===\n");
            diag.append("Completed: ").append(completed.size()).append("/").append(totalTasks);
            diag.append(" after ").append(elapsed).append("ms\n");
            diag.append("Submit took: ").append(submitEnd - submitStart).append("ms\n");
            diag.append("Completed workers (group.seq @ startMs/endMs):\n");
            for (Worker w : completed) {
                diag.append("  group=")
                        .append(w.group)
                        .append(" seq=")
                        .append(w.seq)
                        .append(" started=")
                        .append(w.startTimeMs.get())
                        .append("ms ended=")
                        .append(w.endTimeMs.get())
                        .append("ms slept=")
                        .append(w.actualSleepMs.get())
                        .append("ms\n");
            }
            // Report which tasks never completed
            diag.append("Missing workers:\n");
            for (int g = 0; g < groups; g++) {
                for (int s = 0; s < groups; s++) {
                    Worker w = workers.get(g).get(s);
                    if (w.endTimeMs.get() == 0) {
                        diag.append("  group=")
                                .append(w.group)
                                .append(" seq=")
                                .append(w.seq)
                                .append(" started=")
                                .append(w.startTimeMs.get())
                                .append("ms\n");
                    }
                }
            }
            fail(diag.toString() + "\nOriginal exception: " + e.getMessage());
        }

        // Verify ordering within each group
        long completionTime = System.currentTimeMillis() - submitEnd;
        int[] status = new int[groups];
        List<Worker> completedList = new ArrayList<>(completed);
        for (int idx = 0; idx < completedList.size(); idx++) {
            Worker w = completedList.get(idx);
            if (status[w.group] != w.seq.intValue()) {
                // Build diagnostic showing the full completion order for this group
                String groupOrder = completedList.stream()
                        .filter(x -> x.group.equals(w.group))
                        .map(x -> "seq=" + x.seq + "@" + x.endTimeMs.get() + "ms")
                        .collect(Collectors.joining(", "));
                fail("Ordering violation at position "
                        + idx
                        + ": group="
                        + w.group
                        + " expected seq="
                        + status[w.group]
                        + " but got seq="
                        + w.seq
                        + ". Total completion time: "
                        + completionTime
                        + "ms. Group "
                        + w.group
                        + " completion order: ["
                        + groupOrder
                        + "]");
            }
            status[w.group]++;
        }
    }

    static class Worker implements Runnable {

        Integer group;
        Integer seq;
        Queue<Worker> completed;
        AtomicLong startTimeMs = new AtomicLong();
        AtomicLong endTimeMs = new AtomicLong();
        AtomicLong actualSleepMs = new AtomicLong();

        public Worker(Integer group, Integer seq, Queue<Worker> completed) {
            this.group = group;
            this.seq = seq;
            this.completed = completed;
        }

        @Override
        public void run() {
            startTimeMs.set(System.currentTimeMillis());
            Random r = new Random();
            int x = r.nextInt(10) + 1;
            try {
                long before = System.currentTimeMillis();
                Thread.sleep(x * 10);
                actualSleepMs.set(System.currentTimeMillis() - before);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "", e);
            }

            completed.add(this);
            endTimeMs.set(System.currentTimeMillis());
        }
    }
}
