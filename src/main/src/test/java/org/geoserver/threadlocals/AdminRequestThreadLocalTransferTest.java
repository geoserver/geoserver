package org.geoserver.threadlocals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;
import org.geoserver.security.AdminRequest;
import org.junit.After;
import org.junit.Test;

public class AdminRequestThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @After
    public void cleanupThreadLocals() {
        AdminRequest.finish();
    }

    @Test
    public void testAdminRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final Object myState = new Object();
        AdminRequest.start(myState);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(
                new ThreadLocalTransferCallable(new AdminRequestThreadLocalTransfer()) {

                    @Override
                    void assertThreadLocalCleaned() {
                        assertNull(AdminRequest.get());
                    }

                    @Override
                    void assertThreadLocalApplied() {
                        assertSame(myState, AdminRequest.get());
                    }
                });
    }
}
