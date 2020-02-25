/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Stores a coverage and the file system and returns a link to retrieve it back
 *
 * @author Andrea Aime - GeoSolutions
 * @author ETj <etj at geo-solutions.it>
 */
@DescribeProcess(title = "Store Coverage", description = "Stores a raster on the server.")
public class StoreCoverage implements GeoServerProcess {

    private static final GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (we my want to make these configurable in the future
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setCompressionType("LZW");
        DEFAULT_WRITE_PARAMS.setCompressionQuality(0.75F);
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setTiling(512, 512);
    }

    WPSResourceManager resources;

    public StoreCoverage(WPSResourceManager resources) {
        this.resources = resources;
    }

    @DescribeResult(name = "coverageLocation", description = "URL at which raster can be accessed")
    public URL execute(
            @DescribeParameter(name = "coverage", description = "Input raster")
                    GridCoverage2D coverage)
            throws IOException {
        String fileName = coverage.getName().toString() + ".tif";
        final Resource resource = resources.getOutputResource(null, fileName);

        // setting the write parameters for this geotiff
        final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                .setValue(DEFAULT_WRITE_PARAMS);
        final GeneralParameterValue[] wps = params.values().toArray(new GeneralParameterValue[1]);

        // TODO check file prior to writing
        try (OutputStream os = resource.out()) {
            GeoTiffWriter writer = new GeoTiffWriter(os);
            try {
                writer.write(coverage, wps);
            } finally {
                writer.dispose();
            }
        }

        return new URL(resources.getOutputResourceUrl(fileName, "image/tiff"));
    }
}
