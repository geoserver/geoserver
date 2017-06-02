package com.boundlessgeo.gsr.api.service.map;

import com.boundlessgeo.gsr.core.feature.FeatureEncoder;
import com.boundlessgeo.gsr.core.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.core.geometry.SpatialReferenceEncoder;
import com.boundlessgeo.gsr.core.geometry.SpatialRelationship;
import com.boundlessgeo.gsr.model.map.LayerOrTable;
import com.boundlessgeo.gsr.model.map.LayersAndTables;
import com.boundlessgeo.gsr.model.map.QueryResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.restlet.data.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@RestController
public class QueryController extends AbstractMapServiceController {

    private static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();
    private static final Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.global");

    @Autowired
    public QueryController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/{layerId}/query")
    public QueryResponse queryGet(@PathVariable String workspaceName, @PathVariable Integer layerId,
                         @RequestParam(name = "geometryType", defaultValue = "GeometryPoint") String geometryTypeName,
                         @RequestParam(name = "geometry") String geometryText,
                         @RequestParam(name = "inSR") String inSRText,
                         @RequestParam(name = "outSR") String outSRText,
                         @RequestParam(name = "spatialRel", defaultValue = "SpatialRelIntersects") String spatialRelText,
                         @RequestParam(name = "objectIds") String objectIdsText,
                         @RequestParam(name = "relationPattern") String relatePattern,
                         @RequestParam(name = "time", required = false) String time,
                         @RequestParam(name = "text", required = false) String text,
                         @RequestParam(name = "maxAllowableOffsets", required = false) String maxAllowableOffsets,
                         @RequestParam(name = "where", required = false) String whereClause,
                         @RequestParam(name = "returnGeometry", defaultValue = "true") Boolean returnGeometry,
                         @RequestParam(name = "outFields", defaultValue = "*") String outFieldsText,
                         @RequestParam(name = "returnIdsOnly", defaultValue = "false") boolean returnIdsOnly



        ) throws IOException {

        LayersAndTables layersAndTables = LayersAndTables.find(catalog, workspaceName);

        LayerInfo l = null;
        for (LayerOrTable layerOrTable : layersAndTables.layers) {
            if (layerOrTable.id == layerId) {
                l = layerOrTable.layer;
                break;
            }
        }

        if (l == null) {
            for (LayerOrTable layerOrTable : layersAndTables.tables) {
                if (layerOrTable.id == layerId) {
                    l = layerOrTable.layer;
                    break;
                }
            }
        }

        if (null == l) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + " for id " + layerId + "\" of " + layersAndTables);
        }

        FeatureTypeInfo featureType = (FeatureTypeInfo) l.getResource();
        if (null == featureType) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + " for id " + layerId + "\" of " + layersAndTables);
        }

        final String geometryProperty;
        final String temporalProperty;
        final CoordinateReferenceSystem nativeCRS;
        try {
            GeometryDescriptor geometryDescriptor = featureType.getFeatureType().getGeometryDescriptor();
            nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
            geometryProperty = geometryDescriptor.getName().getLocalPart();
            DimensionInfo timeInfo = featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (timeInfo == null || !timeInfo.isEnabled()) {
                temporalProperty = null;
            } else {
                temporalProperty = timeInfo.getAttribute();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to determine geometry type for query request");
        }


        //Query Parameters
        final CoordinateReferenceSystem outSR = parseSpatialReference(outSRText);
        SpatialRelationship spatialRel = SpatialRelationship.fromRequestString(spatialRelText);
        Filter objectIdFilter = parseObjectIdFilter(objectIdsText);

        final CoordinateReferenceSystem inSR = parseSpatialReference(inSRText, geometryText);
        Filter filter = buildGeometryFilter(geometryTypeName, geometryProperty, geometryText, spatialRel, relatePattern, inSR, nativeCRS);

        if (time != null) {
            filter = FILTERS.and(filter, parseTemporalFilter(temporalProperty, time));
        }
        if (text != null) {
            throw new UnsupportedOperationException("Text filter not implemented");
        }
        if (maxAllowableOffsets != null) {
            throw new UnsupportedOperationException("Generalization (via 'maxAllowableOffsets' parameter) not implemented");
        }
        if (whereClause != null) {
            final Filter whereFilter;
            try {
                whereFilter = ECQL.toFilter(whereClause);
            } catch (CQLException e) {
                throw new IllegalArgumentException("'where' parameter must be valid CQL; was " + whereClause, e);
            }
            List<Filter> children = Arrays.asList(filter, whereFilter, objectIdFilter);
            filter = FILTERS.and(children);
        }
        String[] properties = parseOutFields(outFieldsText);

        //TODO: Split into objects depending upon returnIdsOnly
        /*
            if (returnIdsOnly) {
                FeatureEncoder.featureIdSetToJson(source.getFeatures(query), null json);
            } else {
                final boolean reallyReturnGeometry = returnGeometry || properties == null;
                FeatureEncoder.featuresToJson(source.getFeatures(query), null json, reallyReturnGeometry);
            }
         */
        return new QueryResponse(featureType, filter, returnIdsOnly, returnGeometry, properties, outSR);

    }

    private String[] parseOutFields(String outFieldsText) {
        if ("*".equals(outFieldsText)) {
            return null;
        } else {
            return outFieldsText.split(",");
        }
    }

    private Filter parseObjectIdFilter(String objectIdsText) {
        if (null == objectIdsText) {
            return Filter.INCLUDE;
        } else {
            String[] parts = objectIdsText.split(",");
            Set<FeatureId> fids = new HashSet<FeatureId>();
            for (String part : parts) {
                fids.add(FILTERS.featureId(part));
            }
            return FILTERS.id(fids);
        }
    }

    private static Filter buildGeometryFilter(String geometryType, String geometryProperty, String geometryText, SpatialRelationship spatialRel, String relationPattern, CoordinateReferenceSystem requestCRS, CoordinateReferenceSystem nativeCRS) {
        LOG.info("Transforming geometry filter: " + requestCRS + " => " + nativeCRS);
        final MathTransform mathTx;
        if (requestCRS != null) {
            try {
                mathTx = CRS.findMathTransform(requestCRS, nativeCRS, true);
            } catch (FactoryException e) {
                throw new IllegalArgumentException("Unable to transform between input and native coordinate reference systems", e);
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
                        throw new IllegalArgumentException("Error while converting envelope from input to native coordinate system", e1);
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
                        throw new IllegalArgumentException("Error while converting point from input to native coordinate system", e);
                    }
                }
                return spatialRel.createGeometryFilter(geometryProperty, p, relationPattern);
            } // else fall through to the catch-all exception at the end
        } else {
            try {
                net.sf.json.JSON json = JSONSerializer.toJSON(geometryText);
                com.vividsolutions.jts.geom.Geometry g = GeometryEncoder.jsonToGeometry(json);
                if (mathTx != null) {
                    g = JTS.transform(g, mathTx);
                }
                return spatialRel.createGeometryFilter(geometryProperty, g, relationPattern);
            } catch (JSONException e) {
                // fall through here to the catch-all exception at the end
            } catch (TransformException e) {
                throw new IllegalArgumentException("Error while converting geometry from input to native coordinate system", e);
            }
        }
        throw new IllegalArgumentException(
                "Can't determine geometry filter from GeometryType \""
                        + geometryType + "\" and geometry \"" + geometryText
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
        GeometryFactory factory = new com.vividsolutions.jts.geom.GeometryFactory();
        return factory.createPoint(new Coordinate(coords[0], coords[1]));
    }

    private static com.vividsolutions.jts.geom.Point parseJsonPoint(String text) {
        net.sf.json.JSON json = JSONSerializer.toJSON(text);
        try {
            com.vividsolutions.jts.geom.Geometry geometry = GeometryEncoder.jsonToGeometry(json);
            if (geometry instanceof com.vividsolutions.jts.geom.Point) {
                return (com.vividsolutions.jts.geom.Point) geometry;
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    private static CoordinateReferenceSystem parseSpatialReference(String srText) {
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
    private static CoordinateReferenceSystem parseSpatialReference(String srText, String geometryText) {
        try {
            JSONObject jsonObject = JSONObject.fromObject(geometryText);
            Object sr = jsonObject.get("spatialReference");
            if (sr instanceof JSONObject)
                return SpatialReferenceEncoder.fromJson((JSONObject) sr);
            else
                return parseSpatialReference(srText);
        } catch (JSONException e) {
            return parseSpatialReference(srText);
        } catch (FactoryException e) {
            throw new NoSuchElementException("Could not find spatial reference for id " + srText);
        }
    }

    private static Filter parseTemporalFilter(String temporalProperty, String filterText) {
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

    private static Date parseDate(String timestamp) {
        if ("NULL".equals(timestamp)) {
            return null;
        } else {
            try {
                Long time = Long.parseLong(timestamp);
                return new Date(time);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("TIME parameter must be specified in milliseconds since Jan 1 1970 or NULL; was '" + timestamp + "' instead.");
            }
        }
    }

}
