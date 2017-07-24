/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.geometry;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

public final class GeometryEncoder implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        return null;
    }

    public static String toJson(com.vividsolutions.jts.geom.Envelope envelope) {
        JSONStringer json = new JSONStringer();
        envelopeToJson(envelope, json);
        return json.toString();
    }

    public static void referencedEnvelopeToJson(com.vividsolutions.jts.geom.Envelope envelope, SpatialReference sr, JSONBuilder json) {
        json.object();
        envelopeCoordsToJson(envelope, json);
        json.key("spatialReference");
        SpatialReferenceEncoder.toJson(sr, json);
        json.endObject();
    }

    public static void envelopeToJson(com.vividsolutions.jts.geom.Envelope envelope, JSONBuilder json) {
        json.object();
        envelopeCoordsToJson(envelope, json);
        json.endObject();
    }

    private static void envelopeCoordsToJson(com.vividsolutions.jts.geom.Envelope envelope, JSONBuilder json) {
        json
          .key("xmin").value(envelope.getMinX())
          .key("ymin").value(envelope.getMinY())
          .key("xmax").value(envelope.getMaxX())
          .key("ymax").value(envelope.getMaxY());
    }

    /**
     * Converts a GeoTools {@link Geometry} to a GSR {@link com.boundlessgeo.gsr.core.geometry.Geometry}
     *
     * @param geom
     * @return a {@link com.boundlessgeo.gsr.core.geometry.Geometry} or {@link GeometryArray}
     */
    public static com.boundlessgeo.gsr.core.geometry.Geometry toRepresentation(com.vividsolutions.jts.geom.Geometry geom) {
        // Implementation notes.

        // We have only directly provided support for the
        // JTS geometry types that most closely map to those defined in the
        // GeoServices REST API spec. In the future we will need to deal with
        // the remaining JTS geometry types - there's some design work needed to
        // figure out a good tradeoff of information loss (for example, the spec
        // doesn't distinguish between a linestring and a multilinestring) and
        // generality.
        SpatialReference spatialReference = new SpatialReferenceWKID(geom.getSRID());

        if (geom instanceof com.vividsolutions.jts.geom.Point) {
            com.vividsolutions.jts.geom.Point p = (com.vividsolutions.jts.geom.Point) geom;
            return new Point(p.getX(), p.getY(), spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiPoint) {
            com.vividsolutions.jts.geom.MultiPoint mpoint = (com.vividsolutions.jts.geom.MultiPoint) geom;
            List<double[]> points = new ArrayList<>();
            for (int i = 0; i < mpoint.getNumPoints(); i++) {
                points.add(embeddedPoint((com.vividsolutions.jts.geom.Point) mpoint.getGeometryN(i)));
            }
            return new Multipoint(points.toArray(new double[points.size()][]), spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.LineString) {
            com.vividsolutions.jts.geom.LineString line = (com.vividsolutions.jts.geom.LineString) geom;
            return new Polyline(new double[][][]{embeddedLineString(line)}, spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiLineString) {
            com.vividsolutions.jts.geom.MultiLineString mline = (com.vividsolutions.jts.geom.MultiLineString) geom;
            List<double[][]> paths = new ArrayList<>();

            for (int i = 0; i < mline.getNumGeometries(); i++) {
                com.vividsolutions.jts.geom.LineString line = (com.vividsolutions.jts.geom.LineString) mline.getGeometryN(i);
                paths.add(embeddedLineString(line));
            }
            return new Polyline(paths.toArray(new double[paths.size()][][]), spatialReference);

        } else if (geom instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geom;
            List<double[][]> rings = new ArrayList<>();
            rings.add(embeddedLineString(polygon.getExteriorRing()));

            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                rings.add(embeddedLineString(polygon.getInteriorRingN(i)));
            }
            return new Polygon(rings.toArray(new double[rings.size()][][]), spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            com.vividsolutions.jts.geom.MultiPolygon mpoly = (com.vividsolutions.jts.geom.MultiPolygon) geom;
            return toRepresentation(mpoly.getGeometryN(0));
        } else if (geom instanceof com.vividsolutions.jts.geom.GeometryCollection) {
            com.vividsolutions.jts.geom.GeometryCollection collection = (com.vividsolutions.jts.geom.GeometryCollection) geom;
            GeometryTypeEnum geometryType = determineGeometryType(collection);
            List<com.boundlessgeo.gsr.core.geometry.Geometry> geometries = new ArrayList<>();
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                geometries.add(toRepresentation(collection.getGeometryN(i)));
            }
            return new GeometryArray(geometryType, geometries.toArray(new com.boundlessgeo.gsr.core.geometry.Geometry[geometries.size()]), spatialReference);
        } else {
          throw new IllegalStateException("Geometry encoding not yet supported for " + geom.getGeometryType());
        }
    }

    private static double[] embeddedCoordinate(com.vividsolutions.jts.geom.Coordinate coord) {
        return new double[] {coord.x, coord.y};
    }

    private static double[] embeddedPoint(com.vividsolutions.jts.geom.Point point) {
        return embeddedCoordinate(point.getCoordinate());
    }

    private static double[][] embeddedLineString(com.vividsolutions.jts.geom.LineString line) {
        List<double[]> points = new ArrayList<>();
        for (com.vividsolutions.jts.geom.Coordinate c : line.getCoordinates()) {
            points.add(embeddedCoordinate(c));
        }
        return points.toArray(new double[points.size()][]);
    }

    private static GeometryTypeEnum determineGeometryType(com.vividsolutions.jts.geom.GeometryCollection collection) {
        if (collection.getNumGeometries() == 0) {
            return GeometryTypeEnum.POINT;
        } else {
            String type = collection.getGeometryN(0).getGeometryType();
            for (int i = 1; i < collection.getNumGeometries(); i++) {
                com.vividsolutions.jts.geom.Geometry g = collection.getGeometryN(i);
                if (! type.equals(g.getGeometryType())) {
                    throw new IllegalArgumentException("GeoServices REST API Specification does not support mixed geometry types in geometry collections. (Core 9.8)");
                }
            }
            return GeometryTypeEnum.forJTSClass(collection.getGeometryN(0).getClass());
        }
    }

    private static com.vividsolutions.jts.geom.Coordinate jsonArrayToCoordinate(JSONArray array) {
        if (array.size() != 2) {
            throw new JSONException("Coordinate JSON must be an array with exactly two elements");
        }
        return new com.vividsolutions.jts.geom.Coordinate(array.getDouble(0), array.getDouble(1));
    }

    private static com.vividsolutions.jts.geom.Coordinate[] jsonArrayToCoordinates(JSONArray array) {
        com.vividsolutions.jts.geom.Coordinate[] coordinates = new com.vividsolutions.jts.geom.Coordinate[array.size()];
        for (int i = 0; i < array.size(); i++) {
            coordinates[i] = jsonArrayToCoordinate(array.getJSONArray(i));
        }
        return coordinates;
    }

    public static Envelope jsonToEnvelope(net.sf.json.JSON json) {
        if (!(json instanceof JSONObject)) {
            throw new JSONException("An envelope must be encoded as a JSON Object");
        }
        JSONObject obj = (JSONObject) json;
        double minx = obj.getDouble("xmin");
        double miny = obj.getDouble("ymin");
        double maxx = obj.getDouble("xmax");
        double maxy = obj.getDouble("ymax");
        return new Envelope(minx, maxx, miny, maxy);
    }

    public static Geometry jsonToGeometry(net.sf.json.JSON json) {
        if (!(json instanceof JSONObject)) {
            throw new JSONException("A geometry must be encoded as a JSON Object");
        }
        JSONObject obj = (JSONObject) json;
        GeometryFactory geometries = new GeometryFactory();

        if (obj.containsKey("x") && obj.containsKey("y")) {
            double x = obj.getDouble("x");
            double y = obj.getDouble("y");
            return geometries.createPoint(new com.vividsolutions.jts.geom.Coordinate(x, y));
        } else if (obj.containsKey("points")) {
            JSONArray points = obj.getJSONArray("points");
            return geometries.createMultiPoint(jsonArrayToCoordinates(points));
        } else if (obj.containsKey("paths")) {
            JSONArray paths = obj.getJSONArray("paths");
            com.vividsolutions.jts.geom.LineString[] lines = new com.vividsolutions.jts.geom.LineString[paths.size()];
            for (int i = 0; i < paths.size(); i++) {
                com.vividsolutions.jts.geom.Coordinate[] coords = jsonArrayToCoordinates(paths.getJSONArray(i));
                lines[i] = geometries.createLineString(coords);
            }
            return geometries.createMultiLineString(lines);
        } else if (obj.containsKey("rings")) {
            JSONArray rings = obj.getJSONArray("rings");
            if (rings.size() < 1) {
                throw new JSONException("Polygon must have at least one ring");
            }
            com.vividsolutions.jts.geom.LinearRing shell =
                    geometries.createLinearRing(jsonArrayToCoordinates(rings.getJSONArray(0)));
            com.vividsolutions.jts.geom.LinearRing[] holes = new com.vividsolutions.jts.geom.LinearRing[rings.size() - 1];
            for (int i = 1; i < rings.size(); i++) {
                holes[i] = geometries.createLinearRing(jsonArrayToCoordinates(rings.getJSONArray(i)));
            }
            return geometries.createPolygon(shell, holes);
        } else if (obj.containsKey("geometries")) {
            JSONArray nestedGeometries = obj.getJSONArray("geometries");
            com.vividsolutions.jts.geom.Geometry[] parsedGeometries = new com.vividsolutions.jts.geom.Geometry[nestedGeometries.size()];
            for (int i = 0; i < nestedGeometries.size(); i++) {
                parsedGeometries[i] = jsonToGeometry(nestedGeometries.getJSONObject(i));
            }
            return geometries.createGeometryCollection(parsedGeometries);
        } else {
            throw new JSONException("Could not parse Geometry from " + json);
        }
    }

    @Override
    public boolean canConvert(Class clazz) {
        return com.vividsolutions.jts.geom.Geometry.class.isAssignableFrom(clazz) ||
                com.vividsolutions.jts.geom.Envelope.class.isAssignableFrom(clazz);
    }
}
