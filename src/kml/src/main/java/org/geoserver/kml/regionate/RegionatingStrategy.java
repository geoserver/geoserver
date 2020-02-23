/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.map.Layer;
import org.opengis.filter.Filter;

/**
 * Common interface for classes defining a mechanism for regionating KML placemarks.
 *
 * @author David Winslow
 * @author Andrea Aime
 */
public interface RegionatingStrategy {
    /**
     * Given the KML request context, asks the strategy to return a filter matching only the
     * features that have to be included in the output. An SLD based strategy will use the current
     * scale, a tiling based one the area occupied by the requested tile and some criteria to fit in
     * features, and so on.
     */
    public Filter getFilter(WMSMapContent context, Layer layer);

    /**
     * Clear any cached work (indexing, etc.) for a particular feature type's default regionating
     * options.
     */
    public void clearCache(FeatureTypeInfo cfg);
}
