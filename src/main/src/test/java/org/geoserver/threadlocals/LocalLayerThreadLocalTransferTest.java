/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.ows.LocalPublished;
import org.junit.After;
import org.junit.Test;

public class LocalLayerThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @After
    public void cleanupThreadLocals() {
        LocalPublished.remove();
    }

    @Test
    public void testRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final LayerInfo layer = new LayerInfoImpl();
        LocalPublished.set(layer);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(
                new ThreadLocalTransferCallable(new LocalPublishedThreadLocalTransfer()) {

                    @Override
                    void assertThreadLocalCleaned() {
                        assertNull(LocalPublished.get());
                    }

                    @Override
                    void assertThreadLocalApplied() {
                        assertSame(layer, LocalPublished.get());
                    }
                });
    }
}
