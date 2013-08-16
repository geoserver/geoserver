package org.geoserver.threadlocals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.ows.LocalLayer;
import org.junit.After;
import org.junit.Test;

public class LocalLayerThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @After
    public void cleanupThreadLocals() {
        LocalLayer.remove();
    }
    
    @Test
    public void testRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final LayerInfo layer = new LayerInfoImpl();
        LocalLayer.set(layer);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(new ThreadLocalTransferCallable(new LocalLayerThreadLocalTransfer()) {
            
            @Override
            void assertThreadLocalCleaned() {
                assertNull(LocalLayer.get());
            }
            
            @Override
            void assertThreadLocalApplied() {
                assertSame(layer, LocalLayer.get());
            }
        });
    }
}
