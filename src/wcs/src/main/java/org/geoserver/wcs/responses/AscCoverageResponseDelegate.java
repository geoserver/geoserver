/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static java.util.Map.entry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
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

    public AscCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                List.of(
                        ARCGRID_COVERAGE_FORMAT,
                        ARCGRID_COMPRESSED_COVERAGE_FORMAT,
                        "ArcGrid",
                        "ArcGrid-GZIP"), // output formats
                Map.ofEntries( // file extensions
                        entry("ArcGrid", ARCGRID_FILE_EXTENSION),
                        entry("ArcGrid-GZIP", ARCGRID_COMPRESSED_FILE_EXTENSION),
                        entry(ARCGRID_MIME_TYPE, ARCGRID_FILE_EXTENSION),
                        entry(ARCGRID_COMPRESSED_MIME_TYPE, ARCGRID_COMPRESSED_FILE_EXTENSION),
                        entry(ARCGRID_COVERAGE_FORMAT, ARCGRID_FILE_EXTENSION),
                        entry(
                                ARCGRID_COMPRESSED_COVERAGE_FORMAT,
                                ARCGRID_COMPRESSED_FILE_EXTENSION)),
                Map.ofEntries( // mime types
                        entry("ArcGrid", ARCGRID_MIME_TYPE),
                        entry("ArcGrid-GZIP", ARCGRID_COMPRESSED_MIME_TYPE),
                        entry(ARCGRID_COVERAGE_FORMAT, ARCGRID_MIME_TYPE),
                        entry(ARCGRID_COMPRESSED_COVERAGE_FORMAT, ARCGRID_COMPRESSED_MIME_TYPE)));
    }

    private boolean isOutputCompressed(String outputFormat) {
        return ARCGRID_COMPRESSED_COVERAGE_FORMAT.equalsIgnoreCase(outputFormat)
                || "application/arcgrid;gzipped=\"true\"".equals(outputFormat)
                || ARCGRID_COMPRESSED_MIME_TYPE.equals(outputFormat);
    }

    // gzipOut is just a wrapper, output closing managed outside
    @Override
    @SuppressWarnings({"PMD.CloseResource", "PMD.UseTryWithResources"})
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
