/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream, which limits its data size. This stream is used if the content length is
 * unknown.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
abstract class LimitedOutputStream extends FilterOutputStream {
    /** The maximum size of an item, in bytes. */
    private long sizeMax;

    /** The current number of bytes. */
    private long count;

    /** Whether this stream is already closed. */
    private boolean closed;

    /**
     * Creates a new instance.
     *
     * @param pOut The input stream, which shall be limited.
     * @param pSizeMax The limit; no more than this number of bytes shall be returned by the source
     *     stream.
     */
    public LimitedOutputStream(OutputStream pOut, long pSizeMax) {
        super(pOut);
        sizeMax = pSizeMax;
    }

    /**
     * Called to indicate, that the input streams limit has been exceeded.
     *
     * @param pSizeMax The input streams limit, in bytes.
     * @param pCount The actual number of bytes.
     * @throws IOException The called method is expected to raise an IOException.
     */
    protected abstract void raiseError(long pSizeMax, long pCount) throws IOException;

    /**
     * Called to check, whether the input streams limit is reached.
     *
     * @throws IOException The given limit is exceeded.
     */
    private void checkLimit() throws IOException {
        if (count > sizeMax) {
            raiseError(sizeMax, count);
        }
    }

    /**
     * Writes the next byte of data from this input stream. The value byte is returned as an <code>
     * int</code> in the range <code>0</code> to <code>255</code>. If no byte is available because
     * the end of the stream has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected, or an exception is
     * thrown.
     *
     * <p>This method simply performs <code>in.read()</code> and returns the result.
     *
     * @param b the b
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public void write(int b) throws IOException {
        out.write(b);
        count++;
        checkLimit();
    }

    /**
     * Writes up to <code>len</code> bytes of data from this input stream into an array of bytes. If
     * <code>len</code> is not zero, the method blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * <p>This method simply performs <code>in.read(b, off, len)</code> and returns the result.
     *
     * @param b the buffer into which the data is read.
     * @param off The start offset in the destination array <code>b</code>.
     * @param len the maximum number of bytes read.
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        if (len > 0) {
            count += len;
            checkLimit();
        }
    }

    /**
     * Returns, whether this stream is already closed.
     *
     * @return True, if the stream is closed, otherwise false.
     * @throws IOException An I/O error occurred.
     */
    public boolean isClosed() throws IOException {
        return closed;
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream. This
     * method simply performs <code>in.close()</code>.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public void close() throws IOException {
        closed = true;
        super.close();
    }
}
