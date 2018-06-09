/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import org.geoserver.wcs.CoverageCleanerCallback;
import org.opengis.coverage.grid.GridCoverage;

/**
 * A resource managing the lifecycle of a {@link GridCoverage} and disposing of it when the process
 * is complete
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GridCoverageResource implements WPSResource {

    private GridCoverage coverage;

    public GridCoverageResource(GridCoverage coverage) {
        this.coverage = coverage;
    }

    @Override
    public void delete() throws Exception {
        CoverageCleanerCallback.disposeCoverage(coverage);
    }

    @Override
    public String getName() {
        return "Coverage - " + coverage;
    }
}
