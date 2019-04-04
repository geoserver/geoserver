/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.Layer;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Make a best guess as to the appropriate strategy to use for a featuretype and do it
 * automatically. The heuristic is pretty simple; it's based entirely on the default geometry of the
 * featuretype:
 *
 * <ol>
 *   <li>For polygons, use the area.
 *   <li>For line data, use the length.
 *   <li>For point data or mixed geometry types, don't sort at all.
 * </ol>
 *
 * This is applied ONLY when the regionating strategy is 'auto' and no strategy is set by the admin.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public class BestGuessRegionatingStrategy implements RegionatingStrategy {

    GeoServer gs;

    public BestGuessRegionatingStrategy(GeoServer gs) {
        this.gs = gs;
    }

    public Filter getFilter(WMSMapContent context, Layer layer) {
        SimpleFeatureType type = ((SimpleFeatureSource) layer.getFeatureSource()).getSchema();
        Class geomtype = type.getGeometryDescriptor().getType().getBinding();

        if (Point.class.isAssignableFrom(geomtype))
            return new RandomRegionatingStrategy(gs).getFilter(context, layer);

        return new GeometryRegionatingStrategy(gs).getFilter(context, layer);
    }

    public void clearCache(FeatureTypeInfo cfg) {
        new GeometryRegionatingStrategy(gs).clearCache(cfg);
    }
}
