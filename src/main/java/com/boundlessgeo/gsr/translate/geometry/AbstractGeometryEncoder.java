/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.translate.geometry;

import com.boundlessgeo.gsr.Utils;
import com.boundlessgeo.gsr.model.geometry.*;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract encoder for encoding {@link Geometry JTS Geometries} as
 * {@link com.boundlessgeo.gsr.model.geometry.Geometry GSR Geometries}
 *
 * Also includes a number of static utility methods used encode and decode envelopes and other geometries
 *
 * TODO: While this technically implements converter, the converter part doesn't actually do anything yet. Fix this.
 *
 * @param <T> The coordinate type. Must be a {@link Number}.
 */
public abstract class AbstractGeometryEncoder<T extends Number> implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        return null;
    }

    /**
     * Converts a JTS Envelope to a GSR JSON string
     *
     * @param envelope the envelope
     * @return JSON string representation
     */
    public static String toJson(Envelope envelope) {
        JSONStringer json = new JSONStringer();
        envelopeToJson(envelope, json);
        return json.toString();
    }

    /**
     * Converts a JTS Envelope with a Spatial Reference to a GSR JSON string
     *
     * @param envelope the envelope
     * @param sr the spatial reference
     * @return JSON string representation
     */
    public static void referencedEnvelopeToJson(Envelope envelope, SpatialReference sr, JSONBuilder json) {
        json.object();
        envelopeCoordsToJson(envelope, json);
        json.key("spatialReference");
        SpatialReferenceEncoder.toJson(sr, json);
        json.endObject();
    }

    /**
     * Converts a JTS Envelope to a JSON object
     *
     * @param envelope the envelope
     * @param json The JSONbuilder to add the envelope to
     */
    public static void envelopeToJson(Envelope envelope, JSONBuilder json) {
        json.object();
        envelopeCoordsToJson(envelope, json);
        json.endObject();
    }

    /**
     * Converts a JTS Envelope to a set of x and y keys for an existing JSON object
     *
     * @param envelope the envelope
     * @param json The JSONbuilder to add the keys to
     */
    private static void envelopeCoordsToJson(Envelope envelope, JSONBuilder json) {
        json
          .key("xmin").value(envelope.getMinX())
          .key("ymin").value(envelope.getMinY())
          .key("xmax").value(envelope.getMaxX())
          .key("ymax").value(envelope.getMaxY());
    }

    /**
     * Converts a GeoTools {@link Geometry} to a GSR {@link com.boundlessgeo.gsr.model.geometry.Geometry}
     *
     * @param geom The Geometry to convert
     * @param spatialReference The spatialReference of geom.
     * @return a {@link com.boundlessgeo.gsr.model.geometry.Geometry} or {@link GeometryArray}
     */
    public com.boundlessgeo.gsr.model.geometry.Geometry toRepresentation(
        Geometry geom, SpatialReference spatialReference) {
        // Implementation notes.

        // We have only directly provided support for the
        // JTS geometry types that most closely map to those defined in the
        // GeoServices REST API spec. In the future we will need to deal with
        // the remaining JTS geometry types - there's some design work needed to
        // figure out a good tradeoff of information loss (for example, the spec
        // doesn't distinguish between a linestring and a multilinestring) and
        // generality.

        if (geom instanceof com.vividsolutions.jts.geom.Point) {
            com.vividsolutions.jts.geom.Point p = (com.vividsolutions.jts.geom.Point) geom;
            T[] coords = embeddedPoint(p);

            return new Point(coords[0], coords[1], spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiPoint) {
            com.vividsolutions.jts.geom.MultiPoint mpoint = (com.vividsolutions.jts.geom.MultiPoint) geom;
            List<T[]> points = new ArrayList<>();
            for (int i = 0; i < mpoint.getNumPoints(); i++) {
                points.add(embeddedPoint((com.vividsolutions.jts.geom.Point) mpoint.getGeometryN(i)));
            }
            return new Multipoint(points.toArray(new Number[points.size()][]), spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.LineString) {
            com.vividsolutions.jts.geom.LineString line = (com.vividsolutions.jts.geom.LineString) geom;
            return new Polyline(new Number[][][]{embeddedLineString(line)}, spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiLineString) {
            com.vividsolutions.jts.geom.MultiLineString mline = (com.vividsolutions.jts.geom.MultiLineString) geom;
            List<T[][]> paths = new ArrayList<>();

            for (int i = 0; i < mline.getNumGeometries(); i++) {
                com.vividsolutions.jts.geom.LineString line = (com.vividsolutions.jts.geom.LineString) mline.getGeometryN(i);
                paths.add(embeddedLineString(line));
            }
            return new Polyline(paths.toArray(new Number[paths.size()][][]), spatialReference);

        } else if (geom instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geom;
            List<T[][]> rings = new ArrayList<>();
            rings.add(embeddedLineString(polygon.getExteriorRing()));

            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                rings.add(embeddedLineString(polygon.getInteriorRingN(i)));
            }
            return new Polygon(rings.toArray(new Number[rings.size()][][]), spatialReference);
        } else if (geom instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            com.vividsolutions.jts.geom.MultiPolygon mpoly = (com.vividsolutions.jts.geom.MultiPolygon) geom;

            com.boundlessgeo.gsr.model.geometry.Geometry[] polygons = new com.boundlessgeo.gsr.model.geometry.Geometry[mpoly.getNumGeometries()];

            for (int i = 0; i < mpoly.getNumGeometries(); i++) {
                //for now, assume these are all polygons. that SHOULD be the case anyway
                com.vividsolutions.jts.geom.Polygon geometryN = (com.vividsolutions.jts.geom.Polygon) mpoly
                    .getGeometryN(i);

                List<T[][]> rings = new ArrayList<>();

                //encode the outer ring
                rings.add(embeddedLineString(geometryN.getExteriorRing()));

                for (int j = 0; j < geometryN.getNumInteriorRing(); j++) {
                    rings.add(embeddedLineString(geometryN.getInteriorRingN(j)));
                }

                polygons[i] = new Polygon(rings.toArray(new Number[rings.size()][][]), spatialReference);
            }

            return new GeometryArray(GeometryTypeEnum.POLYGON, polygons, spatialReference);

        } else if (geom instanceof com.vividsolutions.jts.geom.GeometryCollection) {
            com.vividsolutions.jts.geom.GeometryCollection collection = (com.vividsolutions.jts.geom.GeometryCollection) geom;
            GeometryTypeEnum geometryType = determineGeometryType(collection);
            List<com.boundlessgeo.gsr.model.geometry.Geometry> geometries = new ArrayList<>();
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                geometries.add(toRepresentation(collection.getGeometryN(i), spatialReference));
            }
            return new GeometryArray(geometryType, geometries.toArray(new com.boundlessgeo.gsr.model.geometry.Geometry[geometries.size()]), spatialReference);
        } else {
          throw new IllegalStateException("Geometry encoding not yet supported for " + geom.getGeometryType());
        }
    }

    /**
     * Encodes a coordinate.
     *
     * All methods which encode a feature delegate to this method; implementations of {@link AbstractGeometryEncoder}
     * should override it with the applicable implementation.
     *
     * @param coord The Coordinate to encode
     * @return The coordinate as an array.
     */
    protected abstract T[] embeddedCoordinate(com.vividsolutions.jts.geom.Coordinate coord);

    /**
     * Called immediately before a new feature is encoded.
     * Used by subclasses for to handle certain special cases.
     */
    protected abstract void startFeature();

    /**
     * Called immediately after a feature is encoded.
     * Used by subclasses for to handle certain special cases.
     */
    protected abstract void endFeature();

    /**
     * Encodes a point feature
     *
     * @param point the point to encode.
     * @return the encoded point
     */
    protected T[] embeddedPoint(com.vividsolutions.jts.geom.Point point) {
        startFeature();
        T [] p = embeddedCoordinate(point.getCoordinate());
        endFeature();
        return p;
    }

    /**
     * Encodes a linestring feature (this may be a line feature, or one ring of a polygon feature).
     *
     * @param line the linestring to encode
     * @return the encoded linestring
     */
    protected T[][] embeddedLineString(com.vividsolutions.jts.geom.LineString line) {
        List<T[]> points = new ArrayList<>();
        startFeature();
        for (com.vividsolutions.jts.geom.Coordinate c : line.getCoordinates()) {
            points.add(embeddedCoordinate(c));
        }
        endFeature();
        return (T[][])points.toArray(new Number[points.size()][]);
    }

    /**
     * Determines the geometry type of geometries in a geometry collection.
     *
     * @param collection The geometry collection
     * @return The type of all geometries in the collection, or {@link GeometryTypeEnum#POINT} if the collection is
     *         empty.
     * @throws IllegalArgumentException if the gemetry collection contains multiple geometry types
     */
    protected static GeometryTypeEnum determineGeometryType(com.vividsolutions.jts.geom.GeometryCollection collection) {
        if (collection.getNumGeometries() == 0) {
            return GeometryTypeEnum.POINT;
        } else {
            String type = collection.getGeometryN(0).getGeometryType();
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                Geometry g = collection.getGeometryN(i);
                if (! type.equals(g.getGeometryType())) {
                    throw new IllegalArgumentException("GeoServices REST API Specification does not support mixed geometry types in geometry collections. (Core 9.8)");
                }
            }
            return GeometryTypeEnum.forJTSClass(collection.getGeometryN(0).getClass());
        }
    }

    /**
     * Determines the geometry type of geometries in a geometry array.
     *
     * @param geometries The geometry array
     * @return The type of all geometries in the collection, or {@link GeometryTypeEnum#POINT} if the collection is
     *         empty.
     * @throws IllegalArgumentException if the gemetry collection contains multiple geometry types
     */
    protected static GeometryTypeEnum determineGeometryType(com.boundlessgeo.gsr.model.geometry.Geometry[] geometries) {
        if (geometries.length == 0) {
            return GeometryTypeEnum.POINT;
        } else {
            GeometryTypeEnum type = geometries[0].getGeometryType();
            for (int i = 0; i < geometries.length; i++) {
                com.boundlessgeo.gsr.model.geometry.Geometry g = geometries[i];
                if (! type.equals(g.getGeometryType())) {
                    throw new IllegalArgumentException("GeoServices REST API Specification does not support mixed geometry types in geometry collections. (Core 9.8)");
                }
            }
            return type;
        }
    }

    protected static Number[] jsonArrayToPointArray(JSONArray array) {
        if (array.size() != 2) {
            throw new JSONException("Coordinate JSON must be an array with exactly two elements");
        }
        return new Number[]{array.getDouble(0), array.getDouble(1)};
    }

    protected static Number[][] jsonArrayToPointsArray(JSONArray array) {
        Number[][] points = new Number[array.size()][];
        for (int i = 0; i < array.size(); i++) {
            points[i] = jsonArrayToPointArray(array.getJSONArray(i));
        }
        return points;
    }
    protected static Number[][][] jsonArrayToLinesArray(JSONArray array) {
        Number[][][] lines = new Number[array.size()][][];
        for (int i = 0; i < array.size(); i++) {
            lines[i] = jsonArrayToPointsArray(array.getJSONArray(i));
        }
        return lines;
    }

    protected static com.vividsolutions.jts.geom.Coordinate jsonArrayToCoordinate(JSONArray array) {
        if (array.size() != 2) {
            throw new JSONException("Coordinate JSON must be an array with exactly two elements");
        }
        return new com.vividsolutions.jts.geom.Coordinate(array.getDouble(0), array.getDouble(1));
    }

    protected static com.vividsolutions.jts.geom.Coordinate[] jsonArrayToCoordinates(JSONArray array) {
        com.vividsolutions.jts.geom.Coordinate[] coordinates = new com.vividsolutions.jts.geom.Coordinate[array.size()];
        for (int i = 0; i < array.size(); i++) {
            coordinates[i] = jsonArrayToCoordinate(array.getJSONArray(i));
        }
        return coordinates;
    }

    protected static com.vividsolutions.jts.geom.Coordinate arrayToCoordinate(Number[] array) {
        if (array.length != 2) {
            throw new JSONException("Coordinate must be an array with exactly two elements");
        }
        return new com.vividsolutions.jts.geom.Coordinate(array[0].doubleValue(), array[1].doubleValue());
    }

    protected static com.vividsolutions.jts.geom.Coordinate[] arrayToCoordinates(Number[][] array) {
        com.vividsolutions.jts.geom.Coordinate[] coordinates = new com.vividsolutions.jts.geom.Coordinate[array.length];
        for (int i = 0; i < array.length; i++) {
            coordinates[i] = arrayToCoordinate(array[i]);
        }
        return coordinates;
    }

    /**
     * Converts a JSON object to an envelope
     *
     * @param json the json object representing an envelope
     * @return the envelope
     */
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

    /**
     * Converts a JSON object to a {@link Geometry}
     *
     * @param json the json object representing a geometry
     * @return the geometry
     *
     * @see #jsonToJtsGeometry(JSON)
     * @see #toJts(com.boundlessgeo.gsr.model.geometry.Geometry)
     */
    public static Geometry jsonToJtsGeometry(net.sf.json.JSON json) {
        return toJts(jsonToGeometry(json));
    }

    /**
     * Converts a JSON object to a {@link com.boundlessgeo.gsr.model.geometry.Geometry}
     *
     * @param json the json object representing a geometry
     * @return the geometry
     */
    public static com.boundlessgeo.gsr.model.geometry.Geometry jsonToGeometry(net.sf.json.JSON json) {
        JSONObject obj = (JSONObject) json;

        SpatialReference spatialReference = null;
        if (obj.containsKey("spatialReference")) {
            spatialReference = SpatialReferenceEncoder.fromJson(obj.getJSONObject("spatialReference"));
        }

        if (obj.containsKey("geometries")) {
            JSONArray nestedGeometries = obj.getJSONArray("geometries");
            com.boundlessgeo.gsr.model.geometry.Geometry[] parsedGeometries = new com.boundlessgeo.gsr.model.geometry.Geometry[nestedGeometries.size()];
            for (int i = 0; i < nestedGeometries.size(); i++) {
                parsedGeometries[i] = jsonToGeometry(nestedGeometries.getJSONObject(i));
            }
            return new GeometryArray(determineGeometryType(parsedGeometries),parsedGeometries,spatialReference);
        } else if (obj.containsKey("x") && obj.containsKey("y") || geometryTypeEquals(obj, GeometryTypeEnum.POINT)) {
            double x = obj.getDouble("x");
            double y = obj.getDouble("y");
            return new Point(x, y, spatialReference);
        } else if (obj.containsKey("points") || geometryTypeEquals(obj, GeometryTypeEnum.MULTIPOINT)) {
            JSONArray points = obj.getJSONArray("points");
            return new Multipoint(jsonArrayToPointsArray(points), spatialReference);
        } else if (obj.containsKey("paths") || geometryTypeEquals(obj, GeometryTypeEnum.POLYLINE)) {
            JSONArray paths = obj.getJSONArray("paths");
            return new Polyline(jsonArrayToLinesArray(paths), spatialReference);
        } else if (obj.containsKey("rings") || geometryTypeEquals(obj, GeometryTypeEnum.POLYGON)) {

            JSONArray rings = obj.getJSONArray("rings");
            if (rings.size() < 1) {
                throw new JSONException("Polygon must have at least one ring");
            }
            return new Polygon(jsonArrayToLinesArray(rings), spatialReference);
        } else {
            throw new JSONException("Could not parse Geometry from " + json);
        }
    }

    /**
     * Convert a {@link com.boundlessgeo.gsr.model.geometry.Geometry GSR Geometry} to a {@link Geometry JTS Geometry}
     * @param geometry GSR Geometry
     * @return JTS Geometry
     */
    public static Geometry toJts(com.boundlessgeo.gsr.model.geometry.Geometry geometry) {
        GeometryFactory geometries = new GeometryFactory();

        if (geometry instanceof Point) {
            Point p = (Point)geometry;

            double x = p.getX().doubleValue();
            double y = p.getY().doubleValue();
            return geometries.createPoint(new com.vividsolutions.jts.geom.Coordinate(x, y));
        } else if (geometry instanceof Multipoint) {
            Multipoint mp = (Multipoint)geometry;

            Number[][] points = mp.getPoints();
            return geometries.createMultiPoint(arrayToCoordinates(points));
        } else if (geometry instanceof Polyline) {

            Polyline pl = (Polyline)geometry;

            Number[][][] paths = pl.getPaths();

            com.vividsolutions.jts.geom.LineString[] lines = new com.vividsolutions.jts.geom.LineString[paths.length];
            for (int i = 0; i < paths.length; i++) {
                com.vividsolutions.jts.geom.Coordinate[] coords = arrayToCoordinates(paths[i]);
                lines[i] = geometries.createLineString(coords);
            }
            return geometries.createMultiLineString(lines);
        } else if (geometry instanceof Polygon) {

            Polygon pg = (Polygon)geometry;

            Number[][][] rings = pg.getRings();
            if (rings.length < 1) {
                throw new JSONException("Polygon must have at least one ring");
            }
            com.vividsolutions.jts.geom.LinearRing shell =
                    geometries.createLinearRing(arrayToCoordinates(rings[0]));
            com.vividsolutions.jts.geom.LinearRing[] holes = new com.vividsolutions.jts.geom.LinearRing[rings.length - 1];
            for (int i = 1; i < rings.length; i++) {
                holes[i - 1] = geometries.createLinearRing(arrayToCoordinates(rings[i]));
            }
            return geometries.createPolygon(shell, holes);
        } else if (geometry instanceof GeometryArray) {

            GeometryArray ga = (GeometryArray)geometry;
            com.boundlessgeo.gsr.model.geometry.Geometry[] nestedGeometries = ga.getGeometries();
            Geometry[] parsedGeometries = new Geometry[nestedGeometries.length];
            for (int i = 0; i < nestedGeometries.length; i++) {
                parsedGeometries[i] = toJts(nestedGeometries[i]);
            }
            return geometries.createGeometryCollection(parsedGeometries);
        } else {
            throw new IllegalArgumentException("Could not convert " + geometry.getGeometryType() + " to JTS");
        }
    }

    private static boolean geometryTypeEquals(JSONObject obj, GeometryTypeEnum type) {
        return obj.containsKey("geometryType") && obj.getString("geometryType").equals(type.value());
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Geometry.class.isAssignableFrom(clazz) ||
                Envelope.class.isAssignableFrom(clazz);
    }
}
