/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;

import java.util.Optional;
import java.util.logging.Logger;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.kvp.ClipGeometryParser;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Callback for clipping coverage data based on a "clip" vendor parameter expressing the geometry to clip the coverage
 * data with, using WKT or EWKT syntax
 */
public class WCSCoverageClipCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(WCSCoverageClipCallback.class);
    private Class<?> granuleStackClass = null;

    public WCSCoverageClipCallback() {
        try {
            granuleStackClass = Class.forName("org.geoserver.wcs2_0.response.GranuleStack");
        } catch (ClassNotFoundException e) {
            // fine, WCS 2.0 is not in the classpath
        }
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        // if it's not a GetCoverage WCS request, or it's not a coverage, or it doesn't have a "CLIP" parameter there is
        // nothing to do here
        if (!"WCS".equalsIgnoreCase(request.getService())
                || !"GetCoverage".equalsIgnoreCase(request.getRequest())
                || !Optional.ofNullable(request.getRawKvp())
                        .map(m -> m.containsKey("CLIP"))
                        .orElse(false)) return result;

        // check it's the expected type
        if (!acceptResult(result)) return result;

        try {
            LOGGER.fine("Clipping coverage data");
            GridCoverage coverage = (GridCoverage) result;
            Geometry clip = ClipGeometryParser.readGeometry(
                    (String) request.getRawKvp().get("CLIP"), coverage.getCoordinateReferenceSystem());

            return performClip(coverage, clip);
        } catch (ServiceException e) {
            throw e;
        } catch (EmptyIntersectionException e) {
            throw new ServiceException("Clip polygon does not overlap coverage data", INVALID_PARAMETER_VALUE, "clip");
        } catch (Exception e) {
            throw new ServiceException("Failed to clip coverage", e);
        }
    }

    /**
     * Checks if the result is of the expected type, and if not skips the clipping
     *
     * @param result
     * @return
     */
    protected boolean acceptResult(Object result) {
        if ((granuleStackClass != null) && granuleStackClass.isInstance(result)) return false;
        return result instanceof GridCoverage2D;
    }

    protected GridCoverage performClip(GridCoverage coverage, Geometry clip) {
        return WCSUtils.crop((GridCoverage2D) coverage, clip);
    }
}
