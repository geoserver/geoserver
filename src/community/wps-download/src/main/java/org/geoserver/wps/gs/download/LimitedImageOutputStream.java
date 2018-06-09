/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.imageio.stream.output.FilterImageOutputStream;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;

/**
 * An image output stream, which limits its data size. This stream is used if the content length is
 * unknown.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
abstract class LimitedImageOutputStream extends FilterImageOutputStream {
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
    public LimitedImageOutputStream(ImageOutputStream pOut, long pSizeMax) {
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

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        count += len;
        checkLimit();
        super.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        count++;
        checkLimit();
        super.write(b);
    }
}
