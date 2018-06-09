/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.BaseCoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geotools.coverage.grid.GridCoverage2D;

public final class WCSResponseInterceptor extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    public static final String MIME_TYPE = "application/custom";

    private GridCoverage2D result;

    public WCSResponseInterceptor(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("MyOutput"),
                new HashMap<String, String>() { // file
                    // extensions
                    {
                        put(MIME_TYPE, "zip");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("MyOutput", MIME_TYPE);
                    }
                });
    }

    @Override
    public void encode(
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        this.result = coverage;
    }

    public GridCoverage2D getLastResult() {
        return result;
    }
}
