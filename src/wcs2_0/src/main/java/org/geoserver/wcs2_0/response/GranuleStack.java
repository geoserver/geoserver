/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.util.List;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;

/**
 * A stack of GridCoverage2D instances (granules). This class may be used to deal with
 * multidimensional outputs.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public interface GranuleStack extends GridCoverage {

    // the list of dimensions
    public List<DimensionBean> getDimensions();

    // the list of granules composing this stack
    public List<GridCoverage2D> getGranules();
}
