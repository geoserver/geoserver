/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wcs2_0.response.GranuleStack;

/** Fallback factory encoding NetCDFs "as-is", without any coverage remapping */
public class DefaultNetCDFEncoderFactory implements NetCDFEncoderFactory, ExtensionPriority {

    @Override
    public NetCDFEncoder getEncoderFor(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException {
        return new DefaultNetCDFEncoder(granuleStack, file, encodingParameters, outputFormat);
    }

    @Override
    public int getPriority() {
        // lowest possible priority
        return Integer.MAX_VALUE;
    }
}
