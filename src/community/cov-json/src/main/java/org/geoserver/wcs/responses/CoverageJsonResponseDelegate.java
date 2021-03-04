/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.response.MultidimensionalCoverageResponse;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * {@link CoverageResponseDelegate} implementation for CoverageJson multidimensional Grid output
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageJsonResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate, MultidimensionalCoverageResponse {

    private static final String FILE_EXTENSION = "covjson";

    private static final String MIME_TYPE = "application/prs.coverage+json";

    public CoverageJsonResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList(FILE_EXTENSION, MIME_TYPE), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put(MIME_TYPE, FILE_EXTENSION);
                        put(FILE_EXTENSION, FILE_EXTENSION);
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put(MIME_TYPE, MIME_TYPE);
                        put(FILE_EXTENSION, MIME_TYPE);
                    }
                });
    }

    @Override
    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> encodingParameters,
            OutputStream output)
            throws ServiceException, IOException {

        try {
            CoverageJsonEncoder encoder = new CoverageJsonEncoder(sourceCoverage);
            if (encoder != null) {
                encoder.write(output);
            }

        } finally {
            sourceCoverage.dispose(true);
        }
    }
}
