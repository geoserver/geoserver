/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.servlet.ReadListener;
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

    public BufferedRequestStream(InputStream inputStream) throws IOException {
        myInputStream = inputStream;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        if (myInputStream == null) {
            throw new IOException("Stream closed");
        }
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

    @Override
    public boolean isFinished() {
        try {
            return available() < 1;
        } catch (IOException e) {
            Logger LOGGER =
                    org.geotools.util.logging.Logging.getLogger(BufferedRequestStream.class);
            LOGGER.finer("Stream is closed");
            return true;
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Not supported with BufferedInputStream");
    }

    @Override
    public int read() throws IOException {
        if (myInputStream == null) {
            throw new IOException("Stream closed");
        }
        return myInputStream.read();
    }

    @Override
    public long skip(long n) throws IOException {
        if (myInputStream == null) {
            throw new IOException("Stream closed");
        }
        return myInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        if (myInputStream == null) {
            throw new IOException("Stream closed");
        }
        return myInputStream.available();
    }

    @Override
    public void close() throws IOException {
        if (myInputStream != null) {
            try {
                myInputStream.close();
            } finally {
                myInputStream = null;
            }
        } else {
            Logger LOGGER =
                    org.geotools.util.logging.Logging.getLogger(BufferedRequestStream.class);
            LOGGER.finer("Stream already closed");
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (myInputStream != null) {
            myInputStream.mark(readlimit);
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        if (myInputStream == null) {
            throw new IOException("Stream closed");
        }
        myInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        if (myInputStream == null) {
            return false;
        }
        return myInputStream.markSupported();
    }
}
