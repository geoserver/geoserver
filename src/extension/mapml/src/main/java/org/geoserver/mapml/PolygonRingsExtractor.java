/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFilter;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/** Extracts the rings from a polygon, both external (shell) and internal (holes) */
public class PolygonRingsExtractor implements GeometryFilter {
    private List<LinearRing> rings;

    public PolygonRingsExtractor(List<LinearRing> rings) {
        this.rings = rings;
    }

    /** Filters out all linework for polygonal elements */
    @Override
    public void filter(Geometry g) {
        if (g instanceof Polygon) {
            Polygon poly = (Polygon) g;
            rings.add(poly.getExteriorRing());
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                rings.add(poly.getInteriorRingN(i));
            }
        }
    }
}
