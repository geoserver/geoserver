/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * This class extends the JSONBuilder to be able to write out geometric types. It is coded against
 * the draft 5 version of the spec on http://geojson.org
 *
 * @author Chris Holmes, The Open Planning Project
 * @version $Id$
 */
public class GeoJSONBuilder extends JSONBuilder {

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    private int numDecimals = 6;

    private boolean encodeMeasures = false;

    public GeoJSONBuilder(Writer w) {
        super(w);
    }

    /**
     * Writes any geometry object. This class figures out which geometry representation to write and
     * calls subclasses to actually write the object.
     *
     * @param geometry The geometry to be encoded
     * @return The JSONBuilder with the new geometry
     * @throws JSONException If anything goes wrong
     */
    public JSONBuilder writeGeom(Geometry geometry) throws JSONException {
        this.object();
        this.key("type");
        this.value(getGeometryName(geometry));

        final int geometryType = getGeometryType(geometry);

        if (geometryType != MULTIGEOMETRY) {
            this.key("coordinates");

            switch (geometryType) {
                case POINT:
                    Point point = (Point) geometry;
                    writeCoordinate(point);
                    break;
                case LINESTRING:
                    writeCoordinates(((LineString) geometry).getCoordinateSequence());
                    break;
                case MULTIPOINT:
                    this.array();
                    for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
                        writeCoordinate((Point) geometry.getGeometryN(i));
                    }
                    this.endArray();
                    break;
                case POLYGON:
                    writePolygon((Polygon) geometry);

                    break;

                case MULTILINESTRING:
                    this.array();

                    for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
                        writeCoordinates(
                                ((LineString) geometry.getGeometryN(i)).getCoordinateSequence());
                    }

                    this.endArray();

                    break;

                case MULTIPOLYGON:
                    this.array();

                    for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
                        writePolygon((Polygon) geometry.getGeometryN(i));
                    }

                    this.endArray();

                    break;
            }
        } else {
            writeGeomCollection((GeometryCollection) geometry);
        }

        return this.endObject();
    }

    private JSONBuilder writeGeomCollection(GeometryCollection collection) {
        this.key("geometries");
        this.array();

        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            writeGeom(collection.getGeometryN(i));
        }

        return this.endArray();
    }

    /**
     * Helper method that encodes a {@see Point} coordinate to the JSON output. This method will
     * respect the configured axis order. If activated, coordinates measures (M) will be encoded,
     * otherwise measures will be ignored.
     *
     * @param point the point whose coordinate will be encoded
     * @return the JSON builder instance, this allow chained calls
     */
    private JSONBuilder writeCoordinate(Point point) throws JSONException {
        CoordinateSequence coordinates = point.getCoordinateSequence();
        // let's see if we need to encode measures, NaN values will not be encoded
        double m = encodeMeasures ? coordinates.getM(0) : Double.NaN;
        return writeCoordinate(coordinates.getX(0), coordinates.getY(0), coordinates.getZ(0), m);
    }

    /**
     * Helper method that encodes a sequence of coordinates to the JSON output as an array. This
     * method will respect the configured axis order. If activated, coordinates measures (M) will be
     * encoded, otherwise measures will be ignored.
     *
     * @param coordinates the coordinates sequence that will be encoded
     * @return the JSON builder instance, this allow chained calls
     */
    private JSONBuilder writeCoordinates(CoordinateSequence coordinates) throws JSONException {
        // start encoding the JSON array of coordinates
        this.array();
        // each coordinate will be encoded has an array of ordinates
        for (int i = 0; i < coordinates.size(); i++) {
            // let's see if we need to encode measures, NaN values will not be encoded
            double m = encodeMeasures ? coordinates.getM(i) : Double.NaN;
            // encode the coordinate ordinates to the JSON output
            writeCoordinate(coordinates.getX(i), coordinates.getY(i), coordinates.getZ(i), m);
        }
        // we are done with the array
        return this.endArray();
    }

    /**
     * Helper method that will encode the provided coordinate values. The order the {@code X} and
     * {@code Y} coordinates will be encoded will depend on the configured axis order.
     *
     * <p>If both provided {@code Z} or {@code M} values are {@code NaN} they will not be encoded.
     * If a valid {@code M} value was provided but {@code Z} is {@code NaN}, zero (0) will be used
     * for {@code Z}.
     *
     * @param x X ordinate
     * @param y X ordinate
     * @param z Z ordinate, can be {@code NaN}
     * @param m M ordinate, can be {@code NaN}
     * @return the JSON builder instance, this allow chained calls
     */
    private JSONBuilder writeCoordinate(double x, double y, double z, double m) {
        // start encoding JSON array
        this.array();
        // adjust the order of X and Y ordinates if needed
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            // encode latitude first and then longitude
            if (!Double.isNaN(y)) { // for 1d linear referencing cases
                roundedValue(y);
            }
            roundedValue(x);
        } else {
            // encode longitude first and then latitude
            roundedValue(x);
            if (!Double.isNaN(y)) { // for 1d linear referencing cases
                roundedValue(y);
            }
        }
        // if Z value is not available but we have a measure, we set Z value to zero
        z = Double.isNaN(z) && !Double.isNaN(m) ? 0 : z;
        // encode Z value if available
        if (!Double.isNaN(z)) {
            roundedValue(z);
        }
        // encode M value if available
        if (!Double.isNaN(m)) {
            roundedValue(m);
        }
        // we are done with the array
        return this.endArray();
    }

    private void roundedValue(double value) {
        super.value(RoundingUtil.round(value, numDecimals));
    }

    /**
     * Turns an envelope into an array [minX,minY,maxX,maxY]
     *
     * @param env envelope representing bounding box
     * @return this
     */
    protected JSONBuilder writeBoundingBox(Envelope env) {
        this.key("bbox");
        this.array();
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            roundedValue(env.getMinY());
            roundedValue(env.getMinX());
            roundedValue(env.getMaxY());
            roundedValue(env.getMaxX());
        } else {
            roundedValue(env.getMinX());
            roundedValue(env.getMinY());
            roundedValue(env.getMaxX());
            roundedValue(env.getMaxY());
        }
        return this.endArray();
    }

    /**
     * Writes a polygon
     *
     * @param geometry The polygon to write
     */
    private void writePolygon(Polygon geometry) throws JSONException {
        this.array();
        writeCoordinates(geometry.getExteriorRing().getCoordinateSequence());

        for (int i = 0, ii = geometry.getNumInteriorRing(); i < ii; i++) {
            writeCoordinates(geometry.getInteriorRingN(i).getCoordinateSequence());
        }

        this.endArray(); // end the linear ring
        // this.endObject(); //end the
    }

    /** Internal representation of OGC SF Point */
    protected static final int POINT = 1;

    /** Internal representation of OGC SF LineString */
    protected static final int LINESTRING = 2;

    /** Internal representation of OGC SF Polygon */
    protected static final int POLYGON = 3;

    /** Internal representation of OGC SF MultiPoint */
    protected static final int MULTIPOINT = 4;

    /** Internal representation of OGC SF MultiLineString */
    protected static final int MULTILINESTRING = 5;

    /** Internal representation of OGC SF MultiPolygon */
    protected static final int MULTIPOLYGON = 6;

    /** Internal representation of OGC SF MultiGeometry */
    protected static final int MULTIGEOMETRY = 7;

    public static String getGeometryName(Geometry geometry) {
        if (geometry instanceof Point) {
            return "Point";
        } else if (geometry instanceof LineString) {
            return "LineString";
        } else if (geometry instanceof Polygon) {
            return "Polygon";
        } else if (geometry instanceof MultiPoint) {
            return "MultiPoint";
        } else if (geometry instanceof MultiLineString) {
            return "MultiLineString";
        } else if (geometry instanceof MultiPolygon) {
            return "MultiPolygon";
        } else if (geometry instanceof GeometryCollection) {
            return "GeometryCollection";
        } else {
            throw new IllegalArgumentException("Unknown geometry type " + geometry.getClass());
        }
    }

    /**
     * Gets the internal representation for the given Geometry
     *
     * @param geometry a Geometry
     * @return int representation of Geometry
     */
    public static int getGeometryType(Geometry geometry) {
        // LOGGER.entering("GMLUtils", "getGeometryType", geometry);
        if (geometry instanceof Point) {
            // LOGGER.finest("found point");
            return POINT;
        } else if (geometry instanceof LineString) {
            // LOGGER.finest("found linestring");
            return LINESTRING;
        } else if (geometry instanceof Polygon) {
            // LOGGER.finest("found polygon");
            return POLYGON;
        } else if (geometry instanceof MultiPoint) {
            // LOGGER.finest("found multiPoint");
            return MULTIPOINT;
        } else if (geometry instanceof MultiLineString) {
            return MULTILINESTRING;
        } else if (geometry instanceof MultiPolygon) {
            return MULTIPOLYGON;
        } else if (geometry instanceof GeometryCollection) {
            return MULTIGEOMETRY;
        } else {
            throw new IllegalArgumentException(
                    "Unable to determine geometry type " + geometry.getClass());
        }
    }

    /**
     * Write a java.util.List out as a JSON Array. The values of the array will be converted using
     * ike standard primitive conversions. If the list contains List or Map objects, they will be
     * serialized as JSON Arrays and JSON Objects respectively.
     *
     * @param list a java.util.List to be serialized as JSON Array
     */
    public JSONBuilder writeList(final List list) {
        this.array();
        for (final Object o : list) {
            this.value(o);
        }
        return this.endArray();
    }

    /**
     * Write a java.util.Map out as a JSON Object. Keys are serialized using the toString method of
     * the object and values are serialized using primitives conversions. If a value in the map is a
     * List or Map object, it will be serialized as JSON Array or JSON Object respectively.
     *
     * @param map a java.util.Map object to be serialized as a JSON Object
     */
    public JSONBuilder writeMap(final Map map) {
        this.object();
        for (final Object k : map.keySet()) {
            this.key(k.toString());
            this.value(map.get(k));
        }
        return this.endObject();
    }

    /**
     * Overrides handling of specialized types.
     *
     * <p>Overrides the encoding {@code java.util.Date} and its date/time/timestamp descendants, as
     * well as {@code java.util.Calendar} instances as ISO 8601 strings. In addition handles
     * rounding numbers to the specified number of decimal points.
     *
     * <p>Overrides the handling of java.util.Map, java.util.List, and Geometry objects as well.
     *
     * @see net.sf.json.util.JSONBuilder#value(java.lang.Object)
     */
    @Override
    public GeoJSONBuilder value(Object value) {
        if (value == null) {
            super.value(value);
        } else if (value instanceof Geometry) {
            this.writeGeom((Geometry) value);
        } else if (value instanceof List) {
            this.writeList((List) value);
        } else if (value instanceof Map) {
            this.writeMap((Map) value);
        } else {
            if (value instanceof java.util.Date || value instanceof Calendar) {
                value = Converters.convert(value, String.class);
            }
            super.value(value);
        }
        return this;
    }

    /**
     * Set the axis order to assume all input will be provided in. Has no effect on geometries that
     * have already been written.
     */
    public void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }

    public void setNumberOfDecimals(int numberOfDecimals) {
        this.numDecimals = numberOfDecimals;
    }

    /**
     * Sets if coordinates measures (M) should be encoded.
     *
     * @param encodeMeasures TRUE if coordinates measures should be encoded, otherwise FALSE
     */
    public void setEncodeMeasures(boolean encodeMeasures) {
        this.encodeMeasures = encodeMeasures;
    }
}
