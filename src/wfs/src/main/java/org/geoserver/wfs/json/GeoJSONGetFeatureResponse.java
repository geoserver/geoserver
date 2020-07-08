/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a
 * GetFeatureInfo request.
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 */
public class GeoJSONGetFeatureResponse extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    // store the response type
    protected final boolean jsonp;

    public GeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format);
        jsonp = JSONType.isJsonpMimeType(format);
    }

    /** capabilities output format string. */
    public String getCapabilitiesElementName() {
        return JSONType.getJSONType(
                        getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next())
                .toString();
    }

    /** Returns the mime type */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (jsonp) {
            return JSONType.JSONP.getMimeType();
        } else {
            return JSONType.JSON.getMimeType();
        }
    }

    /** Helper method that checks if the results feature collections contain complex features. */
    protected static boolean isComplexFeature(FeatureCollectionResponse results) {
        for (FeatureCollection featureCollection : results.getFeatures()) {
            if (!(featureCollection.getSchema() instanceof SimpleFeatureType)) {
                // this feature collection contains complex features
                return true;
            }
        }
        // all features collections contain only simple features
        return false;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation operation)
            throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) LOGGER.info("about to encode JSON");

        // prepare to write out
        OutputStreamWriter osw = null;
        Writer outWriter = null;

        // get feature count for request
        BigInteger totalNumberOfFeatures = featureCollection.getTotalNumberOfFeatures();
        BigInteger featureCount =
                (totalNumberOfFeatures != null && totalNumberOfFeatures.longValue() < 0)
                        ? null
                        : totalNumberOfFeatures;

        try {
            osw = new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
            outWriter = new BufferedWriter(osw);

            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            // currently complex features count always return zero
            boolean isComplex = isComplexFeature(featureCollection);
            if (featureCount != null && isComplex && featureCount.equals(BigInteger.ZERO)) {
                // a zero count when dealing with complex features means that features count is not
                // supported
                featureCount = null;
            }

            // encode the features and extract information about the CRS and if geometry exists
            final GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollection, outWriter);
            jsonWriter.object().key("type").value("FeatureCollection");
            jsonWriter.key("features");
            jsonWriter.array();
            List<FeatureCollection> resultsList = featureCollection.getFeature();
            FeaturesInfo featuresInfo =
                    writeFeatures(resultsList, operation, isComplex, jsonWriter);
            jsonWriter.endArray(); // end features
            boolean hasGeom = featuresInfo.hasGeometry;
            CoordinateReferenceSystem crs = featuresInfo.crs;
            long numberReturned = featuresInfo.featureCount;

            // write the set of collection wide informations
            writeCollectionCounts(featureCount, numberReturned, jsonWriter);
            writeCollectionTimeStamp(jsonWriter);
            writePagingLinks(featureCollection, operation, jsonWriter);
            writeCollectionCRS(jsonWriter, crs);
            writeCollectionBounds(isFeatureBounding(), jsonWriter, resultsList, hasGeom);
            writeExtraCollectionProperties(featureCollection, operation, jsonWriter);

            jsonWriter.endObject(); // end featurecollection

            if (jsonp) {
                outWriter.write(")");
            }

            outWriter.flush();

        } catch (JSONException jsonException) {
            ServiceException serviceException =
                    new ServiceException("Error: " + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }

    /** Builds, configures and returns {@link GeoJSONBuilder} */
    protected GeoJSONBuilder getGeoJSONBuilder(
            FeatureCollectionResponse featureCollection, Writer outWriter) {
        final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
        int numDecimals = getNumDecimals(featureCollection.getFeature(), gs, gs.getCatalog());
        jsonWriter.setNumberOfDecimals(numDecimals);
        jsonWriter.setEncodeMeasures(
                encodeMeasures(featureCollection.getFeature(), gs.getCatalog()));
        return jsonWriter;
    }

    /** Is WFS configured to return feature and collection bounds? */
    protected boolean isFeatureBounding() {
        WFSInfo wfs = getInfo();
        return wfs.isFeatureBounding();
    }

    /** Writes the feature to the output */
    public FeaturesInfo writeFeatures(
            List<FeatureCollection> resultsList,
            Operation operation,
            boolean isComplex,
            GeoJSONBuilder jsonWriter) {
        FeaturesInfo featuresInfo;
        if (!isComplex) {
            featuresInfo =
                    encodeSimpleFeatures(jsonWriter, resultsList, isFeatureBounding(), operation);
        } else {
            // encode collection with complex features
            ComplexGeoJsonWriter complexWriter =
                    new ComplexGeoJsonWriter(jsonWriter) {

                        @Override
                        protected void writeExtraFeatureProperties(
                                Feature feature, boolean topLevelFeature) {
                            // the various links should be reported only for the top feature, not
                            // for all nested ones
                            if (topLevelFeature) {
                                GeoJSONGetFeatureResponse.this.writeExtraFeatureProperties(
                                        feature, operation, jsonWriter);
                            }
                        }
                    };
            complexWriter.write(resultsList);
            featuresInfo =
                    new FeaturesInfo(
                            complexWriter.foundCrs(),
                            complexWriter.geometryFound(),
                            complexWriter.getFeaturesCount());
        }
        return featuresInfo;
    }

    /** Writes a WFS3 compliant timeStamp collection attribute */
    protected void writeCollectionTimeStamp(GeoJSONBuilder jw) {
        jw.key("timeStamp").value(new ISO8601Formatter().format(new Date()));
    }

    /**
     * Writes the collection counts (if available):
     *
     * <ul>
     *   <li>GeoServer legacy's totalFeatures
     *   <li>WFS 3 numberMatched (same as totalFeatures)
     *   <li>WFS 3 numberReturned
     * </ul>
     */
    protected void writeCollectionCounts(
            BigInteger featureCount, long numberReturned, GeoJSONBuilder jsonWriter) {
        // counts
        if (featureCount != null) {
            jsonWriter.key("totalFeatures").value(featureCount);
            // WFS3 suggested name for the same concept as totalFeatures
            jsonWriter.key("numberMatched").value(featureCount);
        } else {
            jsonWriter.key("totalFeatures").value("unknown");
            // in WFS3 the suggestion is not to include the element
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    /** Writes the collection bounds */
    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList,
            boolean hasGeom) {
        // Bounding box for featurecollection
        if (hasGeom && featureBounding) {
            ReferencedEnvelope e = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = resultsList.get(i);
                if (e == null) {
                    e = collection.getBounds();
                } else {
                    e.expandToInclude(collection.getBounds());
                }
            }

            if (e != null) {
                jsonWriter.setAxisOrder(CRS.getAxisOrder(e.getCoordinateReferenceSystem()));
                jsonWriter.writeBoundingBox(e);
            }
        }
    }

    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs)
            throws IOException {
        // Coordinate Reference System
        try {
            if ("true".equals(GeoServerExtensions.getProperty("GEOSERVER_GEOJSON_LEGACY_CRS"))) {
                // This is wrong, but GeoServer used to do it this way.
                writeCrsLegacy(jsonWriter, crs);
            } else {
                writeCrs(jsonWriter, crs);
            }
        } catch (FactoryException e) {
            throw (IOException) new IOException("Error looking up crs identifier").initCause(e);
        }
    }

    /** Writes WFS3 compliant paging links */
    protected void writePagingLinks(
            FeatureCollectionResponse response, Operation operation, GeoJSONBuilder jw) {

        if (response.getPrevious() != null || response.getNext() != null) {
            jw.key("links");
            jw.array();
            String mimeType = getMimeType(response, operation);
            writeLink(jw, "previous page", mimeType, "previous", response.getPrevious());
            writeLink(jw, "next page", mimeType, "next", response.getNext());
            jw.endArray();
        }
    }

    protected void writeLink(
            GeoJSONBuilder jw, String title, String mimeType, String rel, String href) {
        if (href != null) {
            jw.object();
            jw.key("title").value(title);
            jw.key("type").value(mimeType);
            jw.key("rel").value(rel);
            jw.key("href").value(href);
            jw.endObject();
        }
    }

    /**
     * Allows sub-classes to write extra collection attributes as needs be
     *
     * @param response The response object, list of features being returned
     * @param operation The operation, with access to the request object
     * @param jw The {@link GeoJSONBuilder} to be used when writing the output
     */
    protected void writeExtraCollectionProperties(
            FeatureCollectionResponse response, Operation operation, GeoJSONBuilder jw) {}

    /** Container class for information related with a group of features. */
    private class FeaturesInfo {

        final CoordinateReferenceSystem crs;
        final boolean hasGeometry;
        public long featureCount;

        private FeaturesInfo(
                CoordinateReferenceSystem crs, boolean hasGeometry, long featureCount) {
            this.crs = crs;
            this.hasGeometry = hasGeometry;
            this.featureCount = featureCount;
        }
    }

    private FeaturesInfo encodeSimpleFeatures(
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList,
            boolean featureBounding,
            Operation operation) {
        String id_option = getIdOption();

        CoordinateReferenceSystem crs = null;
        boolean hasGeom = false;
        long featureCount = 0;
        for (FeatureCollection collection : resultsList) {
            try (FeatureIterator iterator = collection.features()) {
                SimpleFeatureType fType;
                List<AttributeDescriptor> types;
                // encode each simple feature
                while (iterator.hasNext()) {
                    // get next simple feature
                    SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                    featureCount++;
                    // start writing the JSON feature object
                    jsonWriter.object();
                    jsonWriter.key("type").value("Feature");
                    fType = simpleFeature.getFeatureType();
                    types = fType.getAttributeDescriptors();
                    // write the simple feature id
                    if (id_option == null) {
                        // no specific attribute nominated, use the simple feature id
                        jsonWriter.key("id").value(simpleFeature.getID());
                    } else if (id_option.length() != 0) {
                        // a specific attribute was nominated to be used as id
                        Object value = simpleFeature.getAttribute(id_option);
                        jsonWriter.key("id").value(value);
                    }
                    // set that axis order that should be used to write geometries
                    GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
                    if (defaultGeomType != null) {
                        CoordinateReferenceSystem featureCrs =
                                defaultGeomType.getCoordinateReferenceSystem();
                        jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                        if (crs == null) {
                            crs = featureCrs;
                        }
                    } else {
                        // If we don't know, assume EAST_NORTH so that no swapping occurs
                        jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                    }
                    // start writing the simple feature geometry JSON object
                    jsonWriter.key("geometry");
                    Geometry aGeom = (Geometry) simpleFeature.getDefaultGeometry();
                    // Write the geometry, whether it is a null or not
                    if (aGeom != null) {
                        jsonWriter.writeGeom(aGeom);
                        hasGeom = true;
                    } else {
                        jsonWriter.value(null);
                    }
                    if (defaultGeomType != null) {
                        jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());
                    }
                    // start writing feature properties JSON object
                    jsonWriter.key("properties");
                    jsonWriter.object();
                    for (int j = 0; j < types.size(); j++) {
                        Object value = simpleFeature.getAttribute(j);
                        AttributeDescriptor ad = types.get(j);
                        if (id_option != null && id_option.equals(ad.getLocalName())) {
                            continue; // skip this value as it is used as the id
                        }
                        if (ad instanceof GeometryDescriptor) {
                            // This is an area of the spec where they
                            // decided to 'let convention evolve',
                            // that is how to handle multiple
                            // geometries. My take is to print the
                            // geometry here if it's not the default.
                            // If it's the default that you already
                            // printed above, so you don't need it here.
                            if (!ad.equals(defaultGeomType)) {
                                if (value == null) {
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.value(null);
                                } else {
                                    // if it was the default geometry, it has been written above
                                    // already
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.writeGeom((Geometry) value);
                                }
                            }
                        } else if (Date.class.isAssignableFrom(ad.getType().getBinding())
                                && TemporalUtils.isDateTimeFormatEnabled()) {
                            // Temporal types print handling
                            jsonWriter.key(ad.getLocalName());
                            jsonWriter.value(TemporalUtils.printDate((Date) value));
                        } else {
                            jsonWriter.key(ad.getLocalName());
                            if ((value instanceof Double && Double.isNaN((Double) value))
                                    || value instanceof Float && Float.isNaN((Float) value)) {
                                jsonWriter.value(null);
                            } else if ((value instanceof Double
                                            && ((Double) value) == Double.POSITIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.POSITIVE_INFINITY) {
                                jsonWriter.value("Infinity");
                            } else if ((value instanceof Double
                                            && ((Double) value) == Double.NEGATIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.NEGATIVE_INFINITY) {
                                jsonWriter.value("-Infinity");
                            } else {
                                jsonWriter.value(value);
                            }
                        }
                    }
                    // Bounding box for feature in properties
                    ReferencedEnvelope refenv =
                            ReferencedEnvelope.reference(simpleFeature.getBounds());
                    if (featureBounding && !refenv.isEmpty()) {
                        jsonWriter.writeBoundingBox(refenv);
                    }
                    jsonWriter.endObject(); // end the properties

                    writeExtraFeatureProperties(simpleFeature, operation, jsonWriter);

                    jsonWriter.endObject(); // end the feature
                }
            }
        }
        return new FeaturesInfo(crs, hasGeom, featureCount);
    }

    private String getIdOption() {
        // include fid?
        String id_option = null; // null - default, "" - none, or "property"
        Request request = Dispatcher.REQUEST.get();
        if (request != null) {
            id_option = JSONType.getIdPolicy(request.getKvp());
        }
        return id_option;
    }

    /**
     * Hook for subclasses to write extra single feature properties
     *
     * @param feature The feature being written
     * @param operation The operation causing this output to be written
     * @param jsonWriter The {@link GeoJSONBuilder} being used to write the feature
     */
    protected void writeExtraFeatureProperties(
            Feature feature, Operation operation, GeoJSONBuilder jsonWriter) {}

    private void writeCrs(final GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs)
            throws FactoryException {
        if (crs != null) {
            String identifier = null;
            Integer code = CRS.lookupEpsgCode(crs, true);
            if (code != null) {
                if (code != null) {
                    identifier = SrsSyntax.OGC_URN.getPrefix() + code;
                }
            } else {
                identifier = CRS.lookupIdentifier(crs, true);
            }

            jsonWriter.key("crs");
            jsonWriter.object();
            jsonWriter.key("type").value("name");
            jsonWriter.key("properties");
            jsonWriter.object();
            jsonWriter.key("name");
            jsonWriter.value(identifier);
            jsonWriter.endObject(); // end properties
            jsonWriter.endObject(); // end crs
        } else {
            jsonWriter.key("crs");
            jsonWriter.value(null);
        }
    }

    // Doesn't follow spec, but GeoServer used to do this.
    private void writeCrsLegacy(final GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs) {
        // Coordinate Reference System, currently only if the namespace is
        // EPSG
        if (crs != null) {
            Set<ReferenceIdentifier> ids = crs.getIdentifiers();
            // WKT defined crs might not have identifiers at all
            if (ids != null && ids.size() > 0) {
                NamedIdentifier namedIdent = (NamedIdentifier) ids.iterator().next();
                String csStr = namedIdent.getCodeSpace().toUpperCase();

                if (csStr.equals("EPSG")) {
                    jsonWriter.key("crs");
                    jsonWriter.object();
                    jsonWriter.key("type").value(csStr);
                    jsonWriter.key("properties");
                    jsonWriter.object();
                    jsonWriter.key("code");
                    jsonWriter.value(namedIdent.getCode());
                    jsonWriter.endObject(); // end properties
                    jsonWriter.endObject(); // end crs
                }
            }
        }
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    @Override
    public String getCharset(Operation operation) {
        return gs.getGlobal().getSettings().getCharset();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return super.getAttachmentFileName(value, operation);
    }
}
