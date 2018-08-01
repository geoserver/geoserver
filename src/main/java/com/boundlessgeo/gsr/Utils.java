package com.boundlessgeo.gsr;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

import com.boundlessgeo.gsr.translate.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.translate.geometry.SpatialReferenceEncoder;
import com.boundlessgeo.gsr.model.geometry.SpatialRelationship;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Utility methods needed throughout GSR
 */
public class Utils {

    protected static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();

    private static final Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.global");

    /**
     * Constructs a geometry {@link Filter} based on the provided parameters
     *
     *@param geometryType The type of geometry specified by the geometry parameter. Values:
     *                    esriGeometryPoint | esriGeometryMultipoint | esriGeometryPolyline | esriGeometryPolygon |
     *                    esriGeometryEnvelope
     * @param geometryText A geometry representing a spatial filter to filter the features by
     * @param requestCRS The spatial reference of the input geometry. If the inSR is not specified, the geometry is
     *                   assumed to be in the spatial reference of the map.
     * @param spatialRel The spatial relationship to be applied on the input geometry while performing the query.
     *                   Values: esriSpatialRelIntersects | esriSpatialRelContains | esriSpatialRelCrosses |
     *                   esriSpatialRelEnvelopeIntersects | esriSpatialRelIndexIntersects | esriSpatialRelOverlaps |
     *                   esriSpatialRelTouches | esriSpatialRelWithin
     * @param geometryProperty The name of the geometry property from the schema the filter is being applied to
     * @param relationPattern The spatial relate function that can be applied while performing the query operation. An
     *                        example for this spatial relate function is "FFFTTT***"
     * @param nativeCRS The native CRS of the schema the filter is being applied to
     * @return
     */
    public static Filter buildGeometryFilter(String geometryType, String geometryProperty, String geometryText,
        SpatialRelationship spatialRel, String relationPattern, CoordinateReferenceSystem requestCRS,
        CoordinateReferenceSystem nativeCRS) {
        LOG.info("Transforming geometry filter: " + requestCRS + " => " + nativeCRS);
        final MathTransform mathTx;
        if (requestCRS != null) {
            try {
                mathTx = CRS.findMathTransform(requestCRS, nativeCRS, true);
            } catch (FactoryException e) {
                throw new IllegalArgumentException(
                    "Unable to translate between input and native coordinate reference systems", e);
            }
        } else {
            mathTx = null;
        }
        if ("esriGeometryEnvelope".equals(geometryType)) {
            Envelope e = parseShortEnvelope(geometryText);
            if (e == null) {
                e = parseJsonEnvelope(geometryText);
            }
            if (e != null) {
                if (mathTx != null) {
                    try {
                        e = JTS.transform(e, mathTx);
                    } catch (TransformException e1) {
                        throw new IllegalArgumentException(
                            "Error while converting envelope from input to native coordinate system", e1);
                    }
                }
                return spatialRel.createEnvelopeFilter(geometryProperty, e, relationPattern);
            }
        } else if ("esriGeometryPoint".equals(geometryType)) {
            com.vividsolutions.jts.geom.Point p = parseShortPoint(geometryText);
            if (p == null) {
                p = parseJsonPoint(geometryText);
            }
            if (p != null) {
                if (mathTx != null) {
                    try {
                        p = (com.vividsolutions.jts.geom.Point) JTS.transform(p, mathTx);
                    } catch (TransformException e) {
                        throw new IllegalArgumentException(
                            "Error while converting point from input to native coordinate system", e);
                    }
                }
                return spatialRel.createGeometryFilter(geometryProperty, p, relationPattern);
            } // else fall through to the catch-all exception at the end
        } else {
            try {
                net.sf.json.JSON json = JSONSerializer.toJSON(geometryText);
                com.vividsolutions.jts.geom.Geometry g = GeometryEncoder.jsonToJtsGeometry(json);
                if (mathTx != null) {
                    g = JTS.transform(g, mathTx);
                }
                return spatialRel.createGeometryFilter(geometryProperty, g, relationPattern);
            } catch (JSONException e) {
                // fall through here to the catch-all exception at the end
            } catch (TransformException e) {
                throw new IllegalArgumentException(
                    "Error while converting geometry from input to native coordinate system", e);
            }
        }
        throw new IllegalArgumentException(
            "Can't determine geometry filter from GeometryType \"" + geometryType + "\" and geometry \"" + geometryText
                + "\"");
    }

    private static Envelope parseShortEnvelope(String text) {
        String[] parts = text.split(",");
        if (parts.length != 4)
            return null;
        double[] coords = new double[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            final double coord;
            try {
                coord = Double.valueOf(part);
            } catch (NumberFormatException e) {
                return null;
            }
            coords[i] = coord;
        }
        // Indices are non-sequential here - JTS and GeoServices disagree on the
        // order of coordinates in an envelope.
        return new Envelope(coords[0], coords[2], coords[1], coords[3]);
    }

    private static Envelope parseJsonEnvelope(String text) {
        net.sf.json.JSON json = JSONSerializer.toJSON(text);
        try {
            return GeometryEncoder.jsonToEnvelope(json);
        } catch (JSONException e) {
            return null;
        }
    }

    private static com.vividsolutions.jts.geom.Point parseShortPoint(String text) {
        String[] parts = text.split(",");
        if (parts.length != 2)
            return null;
        double[] coords = new double[2];
        for (int i = 0; i < 2; i++) {
            String part = parts[i];
            final double coord;
            try {
                coord = Double.valueOf(part);
            } catch (NumberFormatException e) {
                return null;
            }
            coords[i] = coord;
        }
        GeometryFactory factory = new GeometryFactory();
        return factory.createPoint(new Coordinate(coords[0], coords[1]));
    }

    private static com.vividsolutions.jts.geom.Point parseJsonPoint(String text) {
        net.sf.json.JSON json = JSONSerializer.toJSON(text);
        try {
            com.vividsolutions.jts.geom.Geometry geometry = GeometryEncoder.jsonToJtsGeometry(json);
            if (geometry instanceof com.vividsolutions.jts.geom.Point) {
                return (com.vividsolutions.jts.geom.Point) geometry;
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    public static CoordinateReferenceSystem parseSpatialReference(String srText) {
        if (srText == null) {
            return null;
        } else {
            try {
                int srid = Integer.parseInt(srText);
                return CRS.decode("EPSG:" + srid);
            } catch (NumberFormatException e) {
                // fall through - it may be a JSON representation
            } catch (FactoryException e) {
                // this means we successfully parsed the integer, but it is not
                // a valid SRID. Raise it up the stack.
                throw new NoSuchElementException("Could not find spatial reference for ID " + srText);
            }

            try {
                net.sf.json.JSON json = JSONSerializer.toJSON(srText);
                return SpatialReferenceEncoder.coordinateReferenceSystemFromJSON(json);
            } catch (JSONException e) {
                throw new IllegalArgumentException("Failed to parse JSON spatial reference: " + srText);
            }
        }
    }

    /**
     * Read the input spatial reference. This may be specified as an attribute
     * of the geometry (if the geometry is sent as JSON) or else in the 'inSR'
     * query parameter. If both are provided, the JSON property wins.
     */
    public static CoordinateReferenceSystem parseSpatialReference(String srText, String geometryText) {
        try {
            JSONObject jsonObject = JSONObject.fromObject(geometryText);
            Object sr = jsonObject.get("spatialReference");
            if (sr instanceof JSONObject)
                return SpatialReferenceEncoder.coordinateReferenceSystemFromJSON((JSONObject) sr);
            else
                return parseSpatialReference(srText);
        } catch (JSONException e) {
            return parseSpatialReference(srText);
        }
    }

    /**
     * Constructs a temporal {@link Filter} from the provided ESRI temportal filter text
     * @param temporalProperty The temporal property to filter on
     * @param filterText The filter text
     * @return The resultant Filter
     */
    public static Filter parseTemporalFilter(String temporalProperty, String filterText) {
        if (null == temporalProperty || null == filterText || filterText.equals("")) {
            return Filter.INCLUDE;
        } else {
            String[] parts = filterText.split(",");
            if (parts.length == 2) {
                Date d1 = parseDate(parts[0]);
                Date d2 = parseDate(parts[1]);
                if (d1 == null && d2 == null) {
                    throw new IllegalArgumentException("TIME may not have NULL for both start and end times");
                } else if (d1 == null) {
                    return FILTERS.before(FILTERS.property(temporalProperty), FILTERS.literal(d2));
                } else if (d2 == null) {
                    return FILTERS.after(FILTERS.property(temporalProperty), FILTERS.literal(d1));
                } else {
                    Instant start = new DefaultInstant(new DefaultPosition(d1));
                    Instant end = new DefaultInstant(new DefaultPosition(d2));
                    Period p = new DefaultPeriod(start, end);
                    return FILTERS.toverlaps(FILTERS.property(temporalProperty), FILTERS.literal(p));
                }
            } else if (parts.length == 1) {
                Date d = parseDate(parts[0]);
                if (d == null) {
                    throw new IllegalArgumentException("TIME may not have NULL for single-instant filter");
                }
                return FILTERS.tequals(FILTERS.property(temporalProperty), FILTERS.literal(d));
            } else {
                throw new IllegalArgumentException("TIME parameter must comply to POSINT/NULL (, POSINT/NULL)");
            }
        }
    }

    /**
     * Converts a UNIX Epoch time string into a {@link Date}
     * @param timestamp
     * @return The Date, or null if time is null "NULL".
     * @throws IllegalArgumentException if there was an error converting the timestamp
     */
    private static Date parseDate(String timestamp) {
        if ("NULL".equals(timestamp)) {
            return null;
        } else {
            try {
                Long time = Long.parseLong(timestamp);
                return new Date(time);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "TIME parameter must be specified in milliseconds since Jan 1 1970 or NULL; was '" + timestamp
                        + "' instead.");
            }
        }
    }
}
