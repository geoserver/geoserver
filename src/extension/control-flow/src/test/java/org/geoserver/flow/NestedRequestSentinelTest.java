/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class NestedRequestSentinelTest {

    @Test
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
    
    @Test
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
