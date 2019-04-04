/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.geoserver.wps.WPSException;

/**
 * Input stream wrapper that ensures we won't read more than maxSize bytes for a given input
 *
 * @author Andrea Aime - GeoSolutions
 */
class MaxSizeInputStream extends CountingInputStream {

    private long maxSize;

    private String inputId;

    protected MaxSizeInputStream(InputStream in, String inputId, long maxSize) {
        super(in);
        this.inputId = inputId;
        this.maxSize = maxSize;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        checkSize();

        return result;
    }

    @Override
    public int read(byte[] bts) throws IOException {
        int result = super.read(bts);
        checkSize();

        return result;
    }

    @Override
    public int read(byte[] bts, int off, int len) throws IOException {
        int result = super.read(bts, off, len);
        checkSize();

        return result;
    }

    private void checkSize() {
        if (getByteCount() > maxSize) {
            throw new WPSException(
                    "Exceeded maximum input size of "
                            + maxSize
                            + " bytes while reading input "
                            + inputId,
                    "NoApplicableCode",
                    inputId);
        }
    }
}
