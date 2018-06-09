/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import org.geotools.geometry.jts.coordinatesequence.CoordinateSequences;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;

/**
 * This class extends the JSONBuilder to be able to write out geometric types. It is coded against
 * the draft 5 version of the spec on http://geojson.org
 *
 * @author Chris Holmes, The Open Planning Project
 * @version $Id$
 */
public class GeoJSONBuilder extends JSONBuilder {

    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    private int numDecimals = 6;

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
                    Coordinate c = point.getCoordinate();
                    writeCoordinate(c.x, c.y, c.z);
                    break;
                case LINESTRING:
                    writeCoordinates(((LineString) geometry).getCoordinateSequence());
                    break;
                case MULTIPOINT:
                    writeCoordinates(geometry.getCoordinates());
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

    private JSONBuilder writeCoordinates(Coordinate[] coords) throws JSONException {
        return writeCoordinates(new CoordinateArraySequence(coords));
    }

    /**
     * Write the coordinates of a geometry
     *
     * @param coords The coordinates to write
     * @return this
     * @throws JSONException
     */
    private JSONBuilder writeCoordinates(CoordinateSequence coords) throws JSONException {
        this.array();

        // guess the dimension of the coordinate sequence
        int dim = CoordinateSequences.coordinateDimension(coords);

        final int coordCount = coords.size();
        for (int i = 0; i < coordCount; i++) {
            if (dim > 2) {
                writeCoordinate(coords.getX(i), coords.getY(i), coords.getOrdinate(i, 2));
            } else {
                writeCoordinate(coords.getX(i), coords.getY(i));
            }
        }

        return this.endArray();
    }

    private JSONBuilder writeCoordinate(double x, double y) {
        return writeCoordinate(x, y, Double.NaN);
    }

    private JSONBuilder writeCoordinate(double x, double y, double z) {
        this.array();
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            roundedValue(y);
            roundedValue(x);
        } else {
            roundedValue(x);
            roundedValue(y);
        }
        if (!Double.isNaN(z)) {
            roundedValue(z);
        }

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
     * @throws JSONException
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
     *
     * @param axisOrder
     */
    public void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }

    public void setNumberOfDecimals(int numberOfDecimals) {
        this.numDecimals = numberOfDecimals;
    }
}
