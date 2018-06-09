/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for {@link ThreadLocalTransfer} tests. Just implement a ThreadLocalTransferCallable
 * and call {@link #testThreadLocalTransfer(ThreadLocalTransferCallable)} to have the thread local
 * transfer be tested for proper transfer and cleanup.
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractThreadLocalTransferTest {

    protected ExecutorService executor;

    @Before
    public void setupExecutor() {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void stopExecutor() {
        executor.shutdown();
    }

    public void testThreadLocalTransfer(ThreadLocalTransferCallable callable)
            throws InterruptedException, ExecutionException {
        Future<Void> future = executor.submit(callable);
        future.get();
    }

    protected abstract static class ThreadLocalTransferCallable implements Callable<Void> {

        Thread originalThread;

        ThreadLocalTransfer transfer;

        Map<String, Object> storage = new HashMap<String, Object>();

        public ThreadLocalTransferCallable(ThreadLocalTransfer transfer) {
            this.originalThread = Thread.currentThread();
            this.transfer = transfer;
            this.transfer.collect(storage);
        }

        @Override
        public Void call() throws Exception {
            // this is the the main thread, we are actually running inside the thread pool
            assertNotEquals(originalThread, Thread.currentThread());

            // apply the thread local, check it has been applied correctly
            transfer.apply(storage);
            assertThreadLocalApplied();

            // clean up, check the therad local is now empty
            transfer.cleanup();
            assertThreadLocalCleaned();

            return null;
        }

        abstract void assertThreadLocalCleaned();

        abstract void assertThreadLocalApplied();
    };
}
