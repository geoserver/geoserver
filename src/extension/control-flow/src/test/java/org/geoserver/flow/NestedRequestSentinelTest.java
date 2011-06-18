package org.geoserver.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

public class NestedRequestSentinelTest extends TestCase {

    public void testBasicNesting() {
        NestedRequestSentinel sentinel = new NestedRequestSentinel();
        // no nesting
        assertTrue(sentinel.isOutermostRequest());
        // one level, outermost
        sentinel.start();
        assertTrue(sentinel.isOutermostRequest());
        // two levels
        sentinel.start();
        assertFalse(sentinel.isOutermostRequest());
        // three levels
        sentinel.start();
        assertFalse(sentinel.isOutermostRequest());
        // back to two levels
        sentinel.stop();
        assertFalse(sentinel.isOutermostRequest());
        // back to two outermost 
        sentinel.stop();
        assertTrue(sentinel.isOutermostRequest());
        sentinel.stop();
    }
    
    public void testMTNesting() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Throwable>> results = new ArrayList<Future<Throwable>>();
        for (int i = 0; i < 200; i++) {
            Future<Throwable> f = executor.submit(new Callable<Throwable>() {

                public Throwable call() throws Exception {
                    try {
                        testBasicNesting();
                    } catch(Throwable t) {
                        t.printStackTrace();
                        return t;
                    }
                    return null;
                }
            });
            results.add(f);
        }
        
        for (Future<Throwable> future : results) {
            assertNull(future.get());
        }
    }
}
