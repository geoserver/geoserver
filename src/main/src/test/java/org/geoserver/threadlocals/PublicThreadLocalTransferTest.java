package org.geoserver.threadlocals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.junit.Test;

public class PublicThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @Test
    public void testRequest()
            throws InterruptedException, ExecutionException, SecurityException,
                    NoSuchFieldException {
        // setup the state
        final Request request = new Request();
        Dispatcher.REQUEST.set(request);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(
                new ThreadLocalTransferCallable(
                        new PublicThreadLocalTransfer(Dispatcher.class, "REQUEST")) {

                    @Override
                    void assertThreadLocalCleaned() {
                        assertNull(Dispatcher.REQUEST.get());
                    }

                    @Override
                    void assertThreadLocalApplied() {
                        assertSame(request, Dispatcher.REQUEST.get());
                    }
                });
    }
}
