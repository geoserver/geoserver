/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIException;
import org.geotools.dggs.DGGSFilterTransformer;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpStatus;

/**
 * Parses geometry specifications and returns the corresponding filter and geometry for JSON
 * inclusion.
 */
class DGGSGeometryFilterParser {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    static final Logger LOGGER = Logging.getLogger(DGGSGeometryFilterParser.class);

    Filter filter = Filter.INCLUDE;
    Geometry geometry;
    FilterFactory2 ff;
    DGGSInstance dggs;
    Class<? extends Geometry> geometryType = Polygon.class;

    public DGGSGeometryFilterParser(FilterFactory2 ff, DGGSInstance dggs) {
        this.ff = ff;
        this.dggs = dggs;
    }

    public DGGSGeometryFilterParser(
            FilterFactory2 ff, DGGSInstance dggs, Class<? extends Geometry> geometryType) {
        this.ff = ff;
        this.dggs = dggs;
        this.geometryType = geometryType;
    }

    public void setBBOX(String bbox) throws FactoryException {
        if (bbox != null && !bbox.isEmpty()) {
            this.filter = APIBBoxParser.toFilter(bbox);
            this.geometry = APIBBoxParser.toGeometry(bbox);
        }
    }

    public void setGeometry(String geom) {
        if (geom != null && !geom.isEmpty()) {
            try {
                this.geometry = new WKTReader().read(geom);
            } catch (ParseException e) {
                this.geometry = parseAsPoint(geom);
            }

            if (geom == null)
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Geometry specification can be either a WKT, or a point specified as x,y",
                        HttpStatus.BAD_REQUEST);

            if (!geometryType.isInstance(geometry)) {
                throw new IllegalArgumentException(
                        "The geom parameter accepts only "
                                + geometryType.getSimpleName()
                                + " specifications");
            }
            this.filter = ff.intersects(ff.property(""), ff.literal(geometry));
        }
    }

    private Geometry parseAsPoint(String coordinates) {
        String[] coord = coordinates.split("\\s*,\\s*");
        if (coord.length != 2) return null;

        try {
            double x = Double.parseDouble(coord[0]);
            double y = Double.parseDouble(coord[1]);
            return GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to parse geometry", e);
        }
        return null;
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
