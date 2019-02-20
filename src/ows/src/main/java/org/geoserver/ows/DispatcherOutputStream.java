/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper for a Dispatcher destination output stream that signals {@link IOException}s thrown
 * while writing to the underlying destination as ignorable for OWS exception reporting, by throwing
 * a {@link ClientStreamAbortedException}.
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 1.6.x
 */
public final class DispatcherOutputStream extends OutputStream {
    private final OutputStream real;

    public DispatcherOutputStream(OutputStream real) {
        this.real = real;
    }

    /** @see OutputStream#flush() */
    public void flush() throws ClientStreamAbortedException {
        try {
            real.flush();
        } catch (IOException e) {
            throw new ClientStreamAbortedException(e);
        }
    }

    /** @see OutputStream#write(byte[], int, int) */
    public void write(byte b[], int off, int len) throws ClientStreamAbortedException {
        try {
            real.write(b, off, len);
        } catch (IOException e) {
            throw new ClientStreamAbortedException(e);
        }
    }

    /** @see OutputStream#write(int) */
    public void write(int b) throws ClientStreamAbortedException {
        try {
            real.write(b);
        } catch (IOException e) {
            throw new ClientStreamAbortedException(e);
        }
    }

    /** @see OutputStream#close() */
    public void close() throws ClientStreamAbortedException {
        try {
            real.close();
        } catch (IOException e) {
            throw new ClientStreamAbortedException(e);
        }
    }
}
