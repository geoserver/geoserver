/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.Test;

public class RawDataPPIOTest {

    @Test
    public void testInputStreamClosed() throws Exception {
        try (TestInputStream is = new TestInputStream();
                OutputStream os = new ByteArrayOutputStream(); ) {
            RawDataPPIO ppio = buildRawDataPPIOWithMockManager();
            RawData rawData = mockRawDataWithInputStream(is);

            ppio.encode(rawData, os);

            assertTrue(is.isClosed());
        }
    }

    private RawDataPPIO buildRawDataPPIOWithMockManager() {
        WPSResourceManager resourceManager = createMock(WPSResourceManager.class);
        replay(resourceManager);
        return new RawDataPPIO(resourceManager);
    }

    private RawData mockRawDataWithInputStream(TestInputStream is) throws IOException {
        RawData rawData = createMock(RawData.class);
        expect(rawData.getInputStream()).andReturn(is);
        replay(rawData);
        return rawData;
    }

    private class TestInputStream extends ByteArrayInputStream {
        private boolean isClosed = false;

        public TestInputStream() {
            super("Test data".getBytes());
        }

        public boolean isClosed() {
            return isClosed;
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }
    }
}
