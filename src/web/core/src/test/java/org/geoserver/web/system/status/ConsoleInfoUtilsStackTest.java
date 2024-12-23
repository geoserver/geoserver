/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Test class to ensure the JVM console dumps all the stack elements for each thread. A bit ugly to have name 10 methods
 * named "methodN" but it's an easy way to have a predictable output.
 */
public class ConsoleInfoUtilsStackTest {

    @Test
    public void testThreadsInfo() throws Exception {

        // named thread pool to ensure that the thread name is included in the stack trace
        ExecutorService es = Executors.newFixedThreadPool(1, r -> new Thread(r, "myTestThread"));
        // submit with a latch that will lock the innermost call
        CountDownLatch latch = new CountDownLatch(2);
        es.submit(() -> method1(latch));
        // wait for the innermost call to happen (will reduce the latch down to 1)
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(latch::getCount, CoreMatchers.is(1L));
        // now grab the jstack with the thread in WAITING state and predictable content
        String info = ConsoleInfoUtils.getThreadsInfo(true, true);
        // release the latch to allow the other thread to finish
        latch.countDown();
        es.shutdown();

        // look for the lines of the stack trace of "myTestThread"
        AtomicBoolean inBlock = new AtomicBoolean(false);
        List<String> testStack;
        Predicate<String> traceFinder = l -> {
            if (!inBlock.get()) {
                if (l.startsWith("\"myTestThread\"")) inBlock.set(true);

            } else if (l.trim().isEmpty()) {
                inBlock.set(false);
            }
            return inBlock.get();
        };
        testStack = info.lines().filter(traceFinder).collect(Collectors.toList());

        // we used to only have 8 entries plus the title, now we should have at least 10 + title
        assertThat(testStack.size(), greaterThan(11));
        assertThat(testStack.get(0), allOf(containsString("myTestThread"), containsString("WAITING")));

        // look for line stack line containing "method1(...)" to avoid changes in the stack trace
        // due to e.g. upgrades of Junit
        int idxFirst = -1;
        for (int i = 0; i < testStack.size(); i++) {
            if (testStack.get(i).contains("method1(")) {
                idxFirst = i;
                break;
            }
        }
        assertNotEquals(-1, idxFirst);
        // now check all 10 methods have been called and are in the tracek
        for (int i = 1; i <= 10; i++) {
            assertThat(
                    testStack.get(idxFirst + 1 - i),
                    containsString("at org.geoserver.web.system.status.ConsoleInfoUtilsStackTest.method" + i));
        }
    }

    private static void method1(CountDownLatch latch) {
        method2(latch);
    }

    private static void method2(CountDownLatch latch) {
        method3(latch);
    }

    private static void method3(CountDownLatch latch) {
        method4(latch);
    }

    private static void method4(CountDownLatch latch) {
        method5(latch);
    }

    private static void method5(CountDownLatch latch) {
        method6(latch);
    }

    private static void method6(CountDownLatch latch) {
        method7(latch);
    }

    private static void method7(CountDownLatch latch) {
        method8(latch);
    }

    private static void method8(CountDownLatch latch) {
        method9(latch);
    }

    private static void method9(CountDownLatch latch) {
        method10(latch);
    }

    private static void method10(CountDownLatch latch) {
        // signal we reached the innermost method
        latch.countDown();
        try {
            // wait for the other thread to finish
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
