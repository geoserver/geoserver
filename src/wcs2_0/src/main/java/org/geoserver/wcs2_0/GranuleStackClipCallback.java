/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.util.List;
import org.geoserver.wcs.WCSCoverageClipCallback;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.wcs2_0.response.GranuleStackImpl;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Geometry;
import org.vfny.geoserver.util.WCSUtils;

public class GranuleStackClipCallback extends WCSCoverageClipCallback {

    @Override
    protected boolean acceptResult(Object result) {
        return result instanceof GranuleStack;
    }

    @Override
    protected GridCoverage performClip(GridCoverage coverage, Geometry clip) {
        GranuleStackImpl stack = (GranuleStackImpl) coverage;
        GranuleStackImpl croppedStack =
                new GranuleStackImpl(stack.getName(), stack.getCoordinateReferenceSystem(), stack.getDimensions());
        List<GridCoverage2D> granules = stack.getGranules();
        for (GridCoverage2D granule : granules) {
            GridCoverage2D cropped = WCSUtils.crop(granule, clip);
            if (cropped != null) croppedStack.addCoverage(cropped);
        }
        return croppedStack;
    }
}
