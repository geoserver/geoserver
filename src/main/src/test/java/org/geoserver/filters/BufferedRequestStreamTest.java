/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

/**
 * Wrap a String up as a ServletInputStream so we can read it multiple times.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class BufferedRequestStreamTest {
    BufferedRequestStream myBRS;
    String myTestString;

    @Before
    public void setUpInternal() throws Exception {
        myTestString = "Hello, this is a test";
        myBRS = new BufferedRequestStream(myTestString.getBytes());
    }

    @Test
    public void testReadLine() throws Exception {
        byte[] b = new byte[1024];
        int off = 0;
        int len = 1024;
        int amountRead = myBRS.readLine(b, off, len);
        String s = new String(b, 0, amountRead);
        assertEquals(myTestString, s);
    }

    @Test
    public void closeClose() throws IOException {
        byte[] b = new byte[1024];
        int off = 0;
        int len = 1024;
        int amountRead = myBRS.readLine(b, off, len);
        String s = new String(b, 0, amountRead);
        assertEquals(myTestString, s);
        myBRS.reset();

        amountRead = myBRS.readLine(b, off, len);
        String s2 = new String(b, 0, amountRead);
        assertEquals(myTestString, s2);

        myBRS.close();

        try {
            amountRead = myBRS.readLine(b, off, len);
            String s3 = new String(b, 0, amountRead);
            assertEquals(myTestString, s3);
            fail("Buffered Request Stream should already be closed");
        } catch (IOException closed) {
            assertEquals("Stream closed", closed.getMessage());
        }

        try {
            myBRS.close();
        } catch (Throwable t) {
            fail("Calling close a second time should log a message but not produce an exception");
        }
    }
}
