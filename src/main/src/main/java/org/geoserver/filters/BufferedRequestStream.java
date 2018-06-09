/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

/**
 * Wrap a String up as a ServletInputStream so we can read it multiple times.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class BufferedRequestStream extends ServletInputStream {
    InputStream myInputStream;

    public BufferedRequestStream(byte[] buff) throws IOException {
        myInputStream = new ByteArrayInputStream(buff);
        myInputStream.mark(16);
        myInputStream.read();
        myInputStream.reset();
    }

    public int readLine(byte[] b, int off, int len) throws IOException {
        int read;
        int index = off;
        int end = off + len;

        while (index < end && (read = myInputStream.read()) != -1) {
            b[index] = (byte) read;
            index++;
            if (((char) read) == '\n') {
                break;
            }
        }

        return index - off;
    }

    public int read() throws IOException {
        return myInputStream.read();
    }

    public int available() throws IOException {
        return myInputStream.available();
    }
}
