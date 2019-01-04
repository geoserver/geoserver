/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.geoserver.wps.ProcessDismissedException;
import org.opengis.util.ProgressListener;

/**
 * Wrapper class for input streams that will throw an exception when a process got cancelled while
 * loading the inputs
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CancellingInputStream extends FilterInputStream {

    ProgressListener listener;

    public CancellingInputStream(InputStream in, ProgressListener listener) {
        super(in);
        this.listener = listener;
    }

    @Override
    public int read() throws IOException {
        checkCancelled();
        return super.read();
    }

    private void checkCancelled() {
        if (listener.isCanceled()) {
            throw new ProcessDismissedException(listener);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkCancelled();
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        return super.read(b, off, len);
    }
}
