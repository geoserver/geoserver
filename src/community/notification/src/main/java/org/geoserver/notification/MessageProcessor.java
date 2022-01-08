/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.common.NotificationProcessor;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/** @author Xandros */
public class MessageProcessor {

    private Logger LOGGER = Logging.getLogger(MessageProcessor.class);

    private ThreadPoolExecutor executorPool;

    private NotificationProcessor processor;

    private Filter filter;

    public void process(Notification notification) {
        if (this.filter == null || this.filter.evaluate(notification)) {
            executorPool.execute(new WorkerThread(notification, this.processor));
        }
    }

    public MessageProcessor(
            int queueSize, int processorThreads, String filter, NotificationProcessor processor) {
        try {
            if (filter != null && !filter.isEmpty()) {
                this.filter = CQL.toFilter(filter);
            }
        } catch (CQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        this.processor = processor;
        // Get the ThreadFactory implementation to use
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // creating the ThreadPoolExecutor
        executorPool =
                new ThreadPoolExecutor(
                        1,
                        processorThreads,
                        10,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(queueSize),
                        threadFactory,
                        new RejectedExecutionHandlerImpl());
    }

    private class WorkerThread implements Runnable {

        private Logger LOGGER = Logging.getLogger(WorkerThread.class);

        private NotificationProcessor processor;

        private Notification notification;

        public WorkerThread(Notification notification, NotificationProcessor processor) {
            this.processor = processor;
            this.notification = notification;
        }

        @Override
        public void run() {
            try {
                this.processor.process(this.notification);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    // RejectedExecutionHandler implementation
    private class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        private Logger LOGGER = Logging.getLogger(RejectedExecutionHandlerImpl.class);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            executor.execute(r);
        }
    }
}
