/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.geoserver.api.APIBBoxParser;
import org.geotools.dggs.DGGSFilterTransformer;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

/**
 * Parses geometry specifications and returns the corresponding filter and geometry for JSON
 * inclusion.
 */
class DGGSGeometryFilterParser {

    Filter filter = Filter.INCLUDE;
    Geometry geometry;
    FilterFactory2 ff;
    DGGSInstance dggs;

    public DGGSGeometryFilterParser(FilterFactory2 ff, DGGSInstance dggs) {
        this.ff = ff;
        this.dggs = dggs;
    }

    public void setBBOX(String bbox) throws FactoryException {
        if (bbox != null && !bbox.isEmpty()) {
            this.filter = APIBBoxParser.toFilter(bbox);
            this.geometry = APIBBoxParser.toGeometry(bbox);
        }
    }

    public void setWKT(String wkt) throws ParseException {
        if (wkt != null && !wkt.isEmpty()) {
            this.geometry = new WKTReader().read(wkt);
            if (!(geometry instanceof Polygon)) {
                throw new IllegalArgumentException(
                        "The geom parameter accepts only a POLYGON specification");
            }
            this.filter = ff.intersects(ff.property(""), ff.literal(geometry));
        }
    }

    public void setZoneIds(String zoneIds, int resolution) {
        if (zoneIds != null && !zoneIds.isEmpty()) {
            String[] identifiers = zoneIds.split("\\s*,\\s*");
            this.geometry = null;
            Iterator<Zone> zoneIterator =
                    Arrays.stream(identifiers).map(id -> dggs.getZone(id)).iterator();
            this.filter = DGGSFilterTransformer.getFilterFrom(dggs, zoneIterator, resolution);
            this.geometry =
                    CascadedPolygonUnion.union(
                            Arrays.stream(identifiers)
                                    .map(id -> dggs.getZone(id).getBoundary())
                                    .collect(Collectors.toList()));
        }
    }

    public Filter getFilter() {
        return filter;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
