/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridWriter;

/**
 * {@link CoverageResponseDelegate} implementation for Ascii Grids
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class AscCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    @SuppressWarnings("serial")
    public AscCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("ArcGrid", "ArcGrid-GZIP"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("ArcGrid", "asc");
                        put("ArcGrid-GZIP", "asc.gz");
                        put("text/plain", "asc");
                        put("application/x-gzip", "ArcGrid-GZIP");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("ArcGrid", "text/plain");
                        put("ArcGrid-GZIP", "application/x-gzip");
                    }
                });
    }

    private boolean isOutputCompressed(String outputFormat) {
        return "ArcGrid-GZIP".equalsIgnoreCase(outputFormat)
                || "application/arcgrid;gzipped=\"true\"".equals(outputFormat);
    }

    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(
                    new StringBuffer("It seems prepare() has not been called")
                            .append(" or has not succeed")
                            .toString());
        }

        GZIPOutputStream gzipOut = null;
        if (isOutputCompressed(outputFormat)) {
            gzipOut = new GZIPOutputStream(output);
            output = gzipOut;
        }

        ArcGridWriter writer = null;
        try {
            writer = new ArcGridWriter(output);
            writer.write(sourceCoverage, null);

            if (gzipOut != null) {
                gzipOut.finish();
                gzipOut.flush();
            }

        } finally {
            try {
                if (writer != null) writer.dispose();
            } catch (Throwable e) {
                // eating exception
            }
            if (gzipOut != null) IOUtils.closeQuietly(gzipOut);

            sourceCoverage.dispose(true);
        }
    }
}
