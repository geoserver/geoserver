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
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
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

    public static final String ARCGRID_COVERAGE_FORMAT = "ARCGRID";
    public static final String ARCGRID_COMPRESSED_COVERAGE_FORMAT =
            ARCGRID_COVERAGE_FORMAT + "-GZIP";
    private static final String ARCGRID_MIME_TYPE = "text/plain";
    private static final String ARCGRID_COMPRESSED_MIME_TYPE = "application/x-gzip";
    private static final String ARCGRID_FILE_EXTENSION = "asc";
    private static final String ARCGRID_COMPRESSED_FILE_EXTENSION = "asc.gz";

    @SuppressWarnings("serial")
    public AscCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList(
                        ARCGRID_COVERAGE_FORMAT,
                        ARCGRID_COMPRESSED_COVERAGE_FORMAT,
                        "ArcGrid",
                        "ArcGrid-GZIP"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("ArcGrid", ARCGRID_FILE_EXTENSION);
                        put("ArcGrid-GZIP", ARCGRID_COMPRESSED_FILE_EXTENSION);
                        put(ARCGRID_MIME_TYPE, ARCGRID_FILE_EXTENSION);
                        put(ARCGRID_COMPRESSED_MIME_TYPE, ARCGRID_COMPRESSED_FILE_EXTENSION);
                        put(ARCGRID_COVERAGE_FORMAT, ARCGRID_FILE_EXTENSION);
                        put(ARCGRID_COMPRESSED_COVERAGE_FORMAT, ARCGRID_COMPRESSED_FILE_EXTENSION);
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("ArcGrid", ARCGRID_MIME_TYPE);
                        put("ArcGrid-GZIP", ARCGRID_COMPRESSED_MIME_TYPE);
                        put(ARCGRID_COVERAGE_FORMAT, ARCGRID_MIME_TYPE);
                        put(ARCGRID_COMPRESSED_COVERAGE_FORMAT, ARCGRID_COMPRESSED_MIME_TYPE);
                    }
                });
    }

    private boolean isOutputCompressed(String outputFormat) {
        return ARCGRID_COMPRESSED_COVERAGE_FORMAT.equalsIgnoreCase(outputFormat)
                || "application/arcgrid;gzipped=\"true\"".equals(outputFormat)
                || ARCGRID_COMPRESSED_MIME_TYPE.equals(outputFormat);
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
                            .append(" or has not succeeded")
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
