/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.ows.Request;
import org.junit.Test;

public class SimpleThreadBlockerTest {

    /** A request that timed out does not free a slot when it completes */
    @Test
    public void testCompleteWithoutIncoming() throws Exception {
        SimpleThreadBlocker blocker = new SimpleThreadBlocker(1);
        Request running = new Request();
        assertTrue(blocker.requestIncoming(running, 0));

        // a request that timed out still gets requestComplete from ControlFlowCallback
        Request timedOut = new Request();
        assertFalse(blocker.requestIncoming(timedOut, 10));
        blocker.requestComplete(timedOut);

        assertEquals(1, blocker.getRunningRequestsCount());
        assertFalse(blocker.requestIncoming(new Request(), 10));

        blocker.requestComplete(running);
        assertEquals(0, blocker.getRunningRequestsCount());
        assertTrue(blocker.requestIncoming(new Request(), 10));
        assertEquals(1, blocker.getRunningRequestsCount());
    }
}
