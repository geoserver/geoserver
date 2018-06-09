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
import java.util.zip.ZipOutputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.gtopo30.GTopo30Writer;
import org.opengis.coverage.grid.GridCoverageWriter;

/**
 * Encoder for gtopo format
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GTopo30CoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    @SuppressWarnings("serial")
    public GTopo30CoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("GTopo30"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("GTopo30", "zip");
                        put("application/gtopo30", "zip");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("GTopo30", "application/gtopo30");
                    }
                });
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#encode(java.io.OutputStream)
     */
    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        // creating a zip outputstream
        final ZipOutputStream outZ = new ZipOutputStream(output);
        output = outZ;

        // creating a writer
        final GridCoverageWriter writer = new GTopo30Writer(outZ);

        try {
            // writing
            writer.write(sourceCoverage, null);
        } finally {
            try {
                // freeing everything
                writer.dispose();
            } catch (Throwable e) {
                // TODO: handle exception
            }
            sourceCoverage.dispose(false);
        }
    }
}
