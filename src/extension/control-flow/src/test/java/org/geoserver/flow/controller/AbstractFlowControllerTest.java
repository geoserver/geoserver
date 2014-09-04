/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.*;

import java.lang.Thread.State;

/**
 * Base class providing utilities to test flow controllers
 * @author Andrea Aime - OpenGeo
 *
 */
public abstract class AbstractFlowControllerTest {

    /**
     * Waits until the thread enters in WAITING or TIMED_WAITING state
     * 
     * @param t
     *            the thread
     * @param maxWait
     *            max amount of time we'll wait
     */
    void waitBlocked(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.WAITING && t.getState() != State.TIMED_WAITING) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be blocked more than maxWait: " + maxWait);
                Thread.currentThread().sleep(10);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }
    
    /**
     * Waits until the thread is terminated
     * 
     * @param t
     *            the thread
     * @param maxWait
     *            max amount of time we'll wait
     */
    void waitTerminated(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.TERMINATED) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be terminated more than maxWait: " + maxWait);
                Thread.currentThread().sleep(20);
            }
        } catch (Exception e) {
            System.out.println("Could not terminate thread " + t);
        }
    }

    
    /**
     * Waits maxWait for the thread to finish by itself, then forcefully kills it
     * @param t
     * @param maxWait
     */
    void waitAndKill(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.isAlive()) {
                if (System.currentTimeMillis() > (start + maxWait)) {
                    // forcefully destroy the thread
                    t.interrupt();
                }

                Thread.currentThread().sleep(20);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }
}
