package org.opengeo.gsr.core.geometry;

import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

public final class GeometryEncoder {
    private GeometryEncoder() {
        throw new RuntimeException("Geometry encoder has only static methods, no need to instantiate it.");
    }
    
    public static String toJson(com.vividsolutions.jts.geom.Geometry geom) {
        JSONStringer json = new JSONStringer();
        toJson(geom, json);
        return json.toString();
    }
    
    public static String toJson(com.vividsolutions.jts.geom.Envelope envelope) {
        JSONStringer json = new JSONStringer();
        envelopeToJson(envelope, json);
        return json.toString();
    }
    
    public static void envelopeToJson(com.vividsolutions.jts.geom.Envelope envelope, JSONBuilder json) {
        json.object()
          .key("xmin").value(envelope.getMinX())
          .key("ymin").value(envelope.getMinY())
          .key("xmax").value(envelope.getMaxX())
          .key("ymax").value(envelope.getMaxY())
        .endObject();
    }

    public static void toJson(com.vividsolutions.jts.geom.Geometry geom, JSONBuilder json) {
        // Implementation notes.
        
        // We have only directly provided support for the
        // JTS geometry types that most closely map to those defined in the
        // GeoServices REST API spec. In the future we will need to deal with
        // the remaining JTS geometry types - there's some design work needed to
        // figure out a good tradeoff of information loss (for example, the spec
        // doesn't distinguish between a linestring and a multilinestring) and
        // generality.
   
        // Currently, we explicitly open and close a JSON object in this method.
        // It might be better for extensibility to push this responsibility onto
        // the caller - for example, that would be a nice way to support the
        // optional 'spatialReference' property on all geometries.
        
        if (geom instanceof com.vividsolutions.jts.geom.Point) {
            com.vividsolutions.jts.geom.Point p = (com.vividsolutions.jts.geom.Point) geom;
            json.object()
              .key("x").value(p.getX())
              .key("y").value(p.getY())
            .endObject();
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiPoint) {
            com.vividsolutions.jts.geom.MultiPoint mpoint = (com.vividsolutions.jts.geom.MultiPoint) geom;
            json.object();
            json.key("points");
            json.array();
            for (int i = 0; i < mpoint.getNumPoints(); i++) {
                embeddedPointToJson((com.vividsolutions.jts.geom.Point) mpoint.getGeometryN(i), json);
            }
            json.endArray();
            json.endObject();
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiLineString) {
            com.vividsolutions.jts.geom.MultiLineString mline = (com.vividsolutions.jts.geom.MultiLineString) geom;
            json.object();
            json.key("paths");
            json.array();

            for (int i = 0; i < mline.getNumGeometries(); i++) {
                com.vividsolutions.jts.geom.LineString line = (com.vividsolutions.jts.geom.LineString) mline.getGeometryN(i);
                embeddedLineStringToJson(line, json);
            }

            json.endArray();
            json.endObject();
        } else if (geom instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geom;
            json.object();
            json.key("rings");
            json.array();
            embeddedLineStringToJson(polygon.getExteriorRing(), json);
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                embeddedLineStringToJson(polygon.getInteriorRingN(i), json);
            }
            json.endArray();
            json.endObject();
        } else if (geom instanceof com.vividsolutions.jts.geom.GeometryCollection) {
            com.vividsolutions.jts.geom.GeometryCollection collection = (com.vividsolutions.jts.geom.GeometryCollection) geom;
            String geometryType = determineGeometryType(collection);
            json.object()
              .key("geometryType").value(geometryType)
              .key("geometries").array();
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                toJson(collection.getGeometryN(i), json);
            }
            json.endArray();
            json.endObject();
        } else {
          throw new IllegalStateException("Geometry encoding not yet supported for " + geom.getGeometryType());
        }
    }

    private static void embeddedCoordinateToJson(com.vividsolutions.jts.geom.Coordinate coord, JSONBuilder json) {
        json.array()
          .value(coord.x)
          .value(coord.y)
        .endArray();
    }

    private static void embeddedPointToJson(com.vividsolutions.jts.geom.Point point, JSONBuilder json) {
        embeddedCoordinateToJson(point.getCoordinate(), json);
    }
    
    private static void embeddedLineStringToJson(com.vividsolutions.jts.geom.LineString line, JSONBuilder json) {
        json.array();
        for (com.vividsolutions.jts.geom.Coordinate c : line.getCoordinates()) {
            embeddedCoordinateToJson(c, json);
        }
        json.endArray();
    }
    
    private static String determineGeometryType(com.vividsolutions.jts.geom.GeometryCollection collection) {
        if (collection.getNumGeometries() == 0) {
            return GeometryTypeEnum.POINT.getGeometryType();
        } else {
            String type = collection.getGeometryN(0).getGeometryType();
            for (int i = 1; i < collection.getNumGeometries(); i++) {
                com.vividsolutions.jts.geom.Geometry g = collection.getGeometryN(i);
                if (! type.equals(g.getGeometryType())) {
                    throw new IllegalArgumentException("GeoServices REST API Specification does not support mixed geometry types in geometry collections. (Core 9.8)");
                }
            }
            return type;
        }
    }
}
