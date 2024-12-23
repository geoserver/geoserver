/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import static org.apache.commons.io.IOUtils.EOF;

import java.io.InputStream;
import org.apache.commons.io.input.ProxyInputStream;
import org.geoserver.wps.WPSException;

/**
 * Input stream wrapper that ensures we won't read more than maxSize bytes for a given input
 *
 * @author Andrea Aime - GeoSolutions
 */
class MaxSizeInputStream extends ProxyInputStream {

    private long maxSize;

    private String inputId;

    private long count;

    protected MaxSizeInputStream(InputStream in, String inputId, long maxSize) {
        super(in);
        this.inputId = inputId;
        this.maxSize = maxSize;
    }

    @Override
    protected synchronized void afterRead(int n) {
        if (n != EOF) {
            count += n;
        }
        if (count > maxSize) {
            throw new WPSException(
                    "Exceeded maximum input size of " + maxSize + " bytes while reading input " + inputId,
                    "NoApplicableCode",
                    inputId);
        }
    }
}
