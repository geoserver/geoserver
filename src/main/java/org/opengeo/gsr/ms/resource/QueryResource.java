package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengeo.gsr.core.feature.FeatureEncoder;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengeo.gsr.core.geometry.SpatialReferenceEncoder;
import org.opengeo.gsr.core.geometry.SpatialRelationship;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class QueryResource extends Resource {
    public static Variant JSON = new Variant(MediaType.APPLICATION_JAVASCRIPT);
    public QueryResource(Context context, Request request, Response response, Catalog catalog, String format) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        getVariants().add(JSON);
    }
    
    private final Catalog catalog;
    private final String format;
    private static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();
    
    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
            String workspace = (String) getRequest().getAttributes().get("workspace");
            String layerOrTableName = (String) getRequest().getAttributes().get("layerOrTable");
            FeatureTypeInfo featureType = catalog.getFeatureTypeByName(workspace, layerOrTableName);
            if (null == featureType) {
                throw new NoSuchElementException("No known table or layer with qualified name \"" + workspace + ":" + layerOrTableName + "\"");
            }

            final String geometryProperty;
            final CoordinateReferenceSystem nativeCRS;
            try {
                GeometryDescriptor geometryDescriptor = featureType.getFeatureType().getGeometryDescriptor();
                nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
                geometryProperty = geometryDescriptor.getName().getLocalPart();
            } catch (IOException e) {
                throw new RuntimeException("Unable to determine geometry type for query request");
            }
            
            Form form = getRequest().getResourceRef().getQueryAsForm();
            if (!(form.getNames().contains("geometryType") && form.getNames().contains("geometry"))) {
                throw new IllegalArgumentException("'geometry' and 'geometryType' parameters are mandatory");
            }
            
            String inSRText = form.getFirstValue("inSR");
            String outSRText = form.getFirstValue("outSR");
            final CoordinateReferenceSystem inSR = parseSpatialReference(inSRText);
            final CoordinateReferenceSystem outSR = parseSpatialReference(outSRText);
            
            String spatialRelText = form.getFirstValue("spatialRel", "SpatialRelIntersects");
            SpatialRelationship spatialRel = SpatialRelationship.fromRequestString(spatialRelText);
            
            String objectIdsText = form.getFirstValue("objectIds");
            Filter objectIdFilter = parseObjectIdFilter(objectIdsText);

            String geometryTypeName = form.getFirstValue("geometryType", "GeometryPoint");
            String geometryText = form.getFirstValue("geometry");
            String relatePattern = form.getFirstValue("relationParam");
            Filter filter = buildGeometryFilter(geometryTypeName, geometryProperty, geometryText, spatialRel, relatePattern, inSR, nativeCRS);

            if (form.getNames().contains("text")) {
                throw new UnsupportedOperationException("Text filter not implemented");
            }
            
            if (form.getNames().contains("maxAllowableOffsets")) {
                throw new UnsupportedOperationException("Generalization (via 'maxAllowableOffsets' parameter) not implemented");
            }
            
            if (form.getNames().contains("where")) {
                String whereClause = form.getFirstValue("where");
                final Filter whereFilter;
                try {
                    whereFilter = ECQL.toFilter(whereClause);
                } catch (CQLException e) {
                    throw new IllegalArgumentException("'where' parameter must be valid CQL", e);
                }
                List<Filter> children = Arrays.asList(filter, whereFilter, objectIdFilter);
                filter = FILTERS.and(children);
            }
            
            String returnGeometryText = form.getFirstValue("returnGeometry", "true");
            final boolean returnGeometry;
            if ("true".equalsIgnoreCase(returnGeometryText)) {
                returnGeometry = true;
            } else if ("false".equalsIgnoreCase(returnGeometryText)) {
                returnGeometry = false;
            } else {
                throw new IllegalArgumentException("Unrecognized value for returnGeometry parameter: " + returnGeometryText);
            }
            
            String outFieldsText = form.getFirstValue("outFields", "*");
            String[] properties = parseOutFields(outFieldsText);
            
            String returnIdsOnlyText = form.getFirstValue("returnIdsOnly", "false");
            boolean returnIdsOnly;
            if ("true".equalsIgnoreCase(returnIdsOnlyText)) {
                returnIdsOnly = true;
            } else if ("false".equalsIgnoreCase(returnIdsOnlyText)) {
                returnIdsOnly = false;
            } else {
                throw new IllegalArgumentException("Unrecognized value for returnIdsOnly parameter: " + returnIdsOnlyText);
            }

            return new JsonQueryRepresentation(featureType, filter, returnIdsOnly, returnGeometry, properties, outSR);
        }
        return super.getRepresentation(variant);
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

    private static class JsonQueryRepresentation extends OutputRepresentation {
        private final FeatureTypeInfo featureType;
        private final Filter geometryFilter;
        private final boolean returnIdsOnly;
        private final boolean returnGeometry;
        private final CoordinateReferenceSystem outCRS;
        private final String[] properties;
        
        public JsonQueryRepresentation(FeatureTypeInfo featureType, Filter geometryFilter, boolean returnIdsOnly, boolean returnGeometry, String[] properties, CoordinateReferenceSystem outCRS) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.featureType = featureType;
            this.geometryFilter = geometryFilter;
            this.returnIdsOnly = returnIdsOnly;
            this.returnGeometry = returnGeometry;
            this.outCRS = outCRS;
            this.properties = properties;
        }
        
        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            FeatureSource<? extends FeatureType, ? extends Feature> source =
                    featureType.getFeatureSource(null, null);
            Query query = properties == null ? new Query(featureType.getName(), geometryFilter) : new Query(featureType.getName(), geometryFilter, properties);
            query.setCoordinateSystemReproject(outCRS);
            
            if (returnIdsOnly) {
                FeatureEncoder.featureIdSetToJson(source.getFeatures(query), json);
            } else {
                final boolean reallyReturnGeometry = returnGeometry || properties == null;
                FeatureEncoder.featuresToJson(source.getFeatures(query), json, reallyReturnGeometry);
            }
            writer.flush();
            writer.close();
        }
    }
    
    private static Filter buildGeometryFilter(String geometryType, String geometryProperty, String geometryText, SpatialRelationship spatialRel, String relationPattern, CoordinateReferenceSystem requestCRS, CoordinateReferenceSystem nativeCRS) {
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
        
        if ("GeometryEnvelope".equals(geometryType)) {
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
        } else if ("GeometryPoint".equals(geometryType)) {
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
}
