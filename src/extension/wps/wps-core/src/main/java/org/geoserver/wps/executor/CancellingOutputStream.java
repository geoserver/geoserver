/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.wps.ProcessDismissedException;
import org.opengis.util.ProgressListener;

/**
 * Wrapper class for output streams that will throw an exception when a process got cancelled during
 * the production of the outputs
 *
 * @author Andrea Aime - GeoSolutions
 */
class CancellingOutputStream extends FilterOutputStream {

    ProgressListener listener;

    public CancellingOutputStream(OutputStream out, ProgressListener listener) {
        super(out);
        this.listener = listener;
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkCancelled();
        out.write(b);
    }

    private void checkCancelled() {
        if (listener.isCanceled()) {
            throw new ProcessDismissedException(listener);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        checkCancelled();
        out.write(b);
    }
}
