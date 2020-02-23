/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;

public class CoverageCleanerCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(CoverageCleanerCallback.class);

    static final ThreadLocal<List<GridCoverage>> COVERAGES = new ThreadLocal<List<GridCoverage>>();

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        // collect the grid coverages that we'll have to dispose of at the
        // end of the request
        if (result instanceof GridCoverage) {
            addCoverages((GridCoverage) result);
        } else if (result instanceof GridCoverage[]) {
            addCoverages((GridCoverage[]) result);
        }

        return result;
    }

    @Override
    public void finished(Request request) {
        clean();
    }

    /** Mark coverage for cleaning. */
    public static void addCoverages(GridCoverage... coverages) {
        List<GridCoverage> list = COVERAGES.get();
        if (list == null) {
            list = new ArrayList<GridCoverage>();
            COVERAGES.set(list);
        }
        list.addAll(Arrays.asList(coverages));
    }

    /** Cleans up a coverage and its internal rendered image */
    public static void disposeCoverage(GridCoverage coverage) {
        RenderedImage ri = coverage.getRenderedImage();
        if (coverage instanceof GridCoverage2D) {
            ((GridCoverage2D) coverage).dispose(true);
        }
        if (ri instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
        }
    }

    /** Clean up any coverages collected by {@link #addCoverages(GridCoverage...)} */
    public void clean() {
        try {
            List<GridCoverage> coverages = COVERAGES.get();
            if (coverages != null) {
                for (GridCoverage coverage : coverages) {
                    try {
                        disposeCoverage(coverage);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING, "Failed to fully dispose coverage: " + coverage, e);
                    }
                }
            }
        } finally {
            COVERAGES.remove();
        }
    }
}
