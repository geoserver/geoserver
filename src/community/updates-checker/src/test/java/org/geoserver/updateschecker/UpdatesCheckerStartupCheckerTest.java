/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.platform.ContextLoadedEvent;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class UpdatesCheckerStartupCheckerTest {

    @Test
    public void runsOneStartupCheckOnAShortLivedDaemonThread() throws Exception {
        final AtomicReference<Thread> checkThread = new AtomicReference<>();
        final AtomicReference<UpdatesCheckerService.Trigger> trigger = new AtomicReference<>();

        UpdatesCheckerService service = new UpdatesCheckerService(null, null) {
            @Override
            public UpdatesCheckerStatus check(Trigger t) {
                trigger.set(t);
                checkThread.set(Thread.currentThread());
                return null;
            }
        };

        Thread thread = new UpdatesCheckerStartupChecker(service).start();
        thread.join(10_000);

        assertFalse("the check thread must end by itself", thread.isAlive());
        assertEquals(UpdatesCheckerService.Trigger.STARTUP, trigger.get());
        assertEquals(thread, checkThread.get());
        assertTrue(thread.isDaemon());
        assertEquals(UpdatesCheckerStartupChecker.THREAD_NAME, thread.getName());
    }

    @Test
    public void theContextLoadedListenerReturnsWithoutWaitingForTheFetch() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicBoolean fetchDone = new AtomicBoolean();
        UpdatesCheckerService service = new UpdatesCheckerService(null, null) {
            @Override
            public UpdatesCheckerStatus check(Trigger t) {
                started.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                fetchDone.set(true);
                return null;
            }
        };

        new UpdatesCheckerStartupChecker(service).onApplicationEvent(new ContextLoadedEvent(dummyContext()));

        // a listener that waited for the fetch would only return after the 10 s hold above, with the fetch done
        assertFalse("startup must not wait for the fetch", fetchDone.get());
        assertTrue("the fetch started", started.await(10, TimeUnit.SECONDS));
        release.countDown();
    }

    private static ApplicationContext dummyContext() {
        return (ApplicationContext) java.lang.reflect.Proxy.newProxyInstance(
                UpdatesCheckerStartupCheckerTest.class.getClassLoader(),
                new Class<?>[] {ApplicationContext.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("not needed for this test");
                });
    }
}
