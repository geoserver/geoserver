/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.TypeInfoCollectionWrapper;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * A GeoJSON writer that can be used to output features in a variety of cases, like WFS GetFeature and WMS
 * GetFeatureInfo.
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * @author Carsten Klein, DataGis
 */
public abstract class GeoJSONFeatureWriter<T extends FeatureType, F extends Feature> {
    private final Logger LOGGER = Logging.getLogger(this.getClass());

    protected final GeoServer gs;

    /** Constructor to be used by subclasses. */
    protected GeoJSONFeatureWriter(GeoServer gs) {
        this.gs = gs;
    }

    /** Helper method that checks if the results feature collections contain complex features. */
    protected static <T extends FeatureType, F extends Feature> boolean isComplexFeature(
            List<FeatureCollection<T, F>> results) {
        for (FeatureCollection<T, F> featureCollection : results) {
            if (!(featureCollection.getSchema() instanceof SimpleFeatureType)) {
                // this feature collection contains complex features
                return true;
            }
        }
        // all features collections contain only simple features
        return false;
    }

    /**
     * Writes the provided collections as GeoJSON on output
     *
     * @param featureCollections The feature collections to write
     * @param output The target output stream
     * @param jsonp {@code true} if specified format uses JSONP
     */
    public void write(
            List<FeatureCollection<T, F>> featureCollections,
            OutputStream output,
            BigInteger featureCount,
            boolean jsonp)
            throws IOException {
        Charset charset = Charset.forName(gs.getGlobal().getSettings().getCharset());
        OutputStreamWriter osw = new OutputStreamWriter(output, charset);
        write(featureCollections, osw, featureCount, jsonp);
    }

    /**
     * Writes the provided collections as GeoJSON on output
     *
     * @param featureCollections The feature collections to write
     * @param osw The target output writer
     * @param jsonp {@code true} if specified format uses JSONP
     */
    public void write(
            List<FeatureCollection<T, F>> featureCollections,
            OutputStreamWriter osw,
            BigInteger featureCount,
            boolean jsonp)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("about to encode JSON");

        try {
            Writer outWriter = new BufferedWriter(osw);

            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            // encode the features and extract information about the CRS and if geometry exists
            final GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollections, outWriter);
            jsonWriter.object().key("type").value("FeatureCollection");
            jsonWriter.key("features");
            jsonWriter.array();
            boolean isComplex = isComplexFeature(featureCollections);
            FeaturesInfo featuresInfo = writeFeatures(featureCollections, isComplex, jsonWriter);
            jsonWriter.endArray(); // end features
            boolean hasGeom = featuresInfo.hasGeometry;
            CoordinateReferenceSystem crs = featuresInfo.crs;
            long numberReturned = featuresInfo.featureCount;

            // write the set of collection wide informations
            writeCollectionCounts(featureCount, numberReturned, jsonWriter);
            writeCollectionTimeStamp(jsonWriter);
            writeCollectionCRS(jsonWriter, crs);
            writeCollectionBounds(isFeatureBounding(), jsonWriter, featureCollections, hasGeom);
            writeExtraCollectionProperties(featureCollections, jsonWriter);

            jsonWriter.endObject(); // end featurecollection

            if (jsonp) {
                outWriter.write(")");
            }

            outWriter.flush();
        } catch (JSONException jsonException) {
            throw new ServiceException("Error: " + jsonException.getMessage(), jsonException);
        }
    }

    /**
     * Writes just the features without any collection or feature array wrapper
     *
     * @param featureCollections
     * @param osw
     */
    public void writeFeaturesContent(List<FeatureCollection<T, F>> featureCollections, OutputStreamWriter osw) {
        final GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollections, osw);
        boolean isComplex = isComplexFeature(featureCollections);
        writeFeatures(featureCollections, isComplex, jsonWriter);
    }

    /**
     * Allows writing extra properties at the end of the GeoJSON FeatureCollection response. E.g. paging links. By
     * default it does nothing.
     *
     * @param featureCollections
     * @param jsonWriter
     */
    protected void writeExtraCollectionProperties(
            List<FeatureCollection<T, F>> featureCollections, GeoJSONBuilder jsonWriter) {}

    /** Builds, configures and returns {@link GeoJSONBuilder} */
    public GeoJSONBuilder getGeoJSONBuilder(List<FeatureCollection<T, F>> collections, Writer outWriter) {
        final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
        int numDecimals = getNumDecimals(collections, gs, gs.getCatalog());
        jsonWriter.setNumberOfDecimals(numDecimals);
        jsonWriter.setEncodeMeasures(encodeMeasures(collections, gs.getCatalog()));
        return jsonWriter;
    }

    protected int getNumDecimals(
            List<FeatureCollection<T, F>> featureCollections, GeoServer geoServer, Catalog catalog) {
        int numDecimals = -1;
        for (FeatureCollection<T, F> featureCollection : featureCollections) {
            Integer ftiDecimals = getFeatureTypeInfoProperty(catalog, featureCollection, fti -> fti.getNumDecimals());

            // track num decimals, in cases where the query has multiple types we choose the max
            // of all the values (same deal as above, might not be a vector due to GetFeatureInfo
            // reusing this)
            if (ftiDecimals != null && ftiDecimals > 0) {
                numDecimals = numDecimals == -1 ? ftiDecimals : Math.max(numDecimals, ftiDecimals);
            }
        }

        SettingsInfo settings = geoServer.getSettings();

        if (numDecimals == -1) {
            numDecimals = settings.getNumDecimals();
        }

        return numDecimals;
    }

    protected boolean getPadWithZeros(
            List<FeatureCollection<T, F>> featureCollections, GeoServer geoServer, Catalog catalog) {
        boolean padWithZeros = false;
        for (FeatureCollection<T, F> featureCollection : featureCollections) {
            Boolean pad = getFeatureTypeInfoProperty(catalog, featureCollection, FeatureTypeInfo::getPadWithZeros);
            if (Boolean.TRUE.equals(pad)) {
                padWithZeros = true;
            }
        }
        return padWithZeros;
    }

    protected boolean getForcedDecimal(
            List<FeatureCollection<T, F>> featureCollections, GeoServer geoServer, Catalog catalog) {
        boolean forcedDecimal = false;
        for (FeatureCollection<T, F> featureCollection : featureCollections) {
            Boolean forced = getFeatureTypeInfoProperty(catalog, featureCollection, FeatureTypeInfo::getForcedDecimal);
            if (Boolean.TRUE.equals(forced)) {
                forcedDecimal = true;
            }
        }
        return forcedDecimal;
    }

    /**
     * Helper method that checks if coordinates measured values should be encoded for the provided feature collections.
     * By default coordinates measures are not encoded.
     *
     * @param featureCollections features collections
     * @param catalog GeoServer catalog
     * @return TRUE if coordinates measures should be encoded, otherwise FALSE
     */
    protected boolean encodeMeasures(List<FeatureCollection<T, F>> featureCollections, Catalog catalog) {
        boolean encodeMeasures = true;
        for (FeatureCollection<T, F> featureCollection : featureCollections) {
            Boolean measures =
                    getFeatureTypeInfoProperty(catalog, featureCollection, FeatureTypeInfo::getEncodeMeasures);
            if (Boolean.FALSE.equals(measures)) {
                // no measures should be encoded
                encodeMeasures = false;
            }
        }
        return encodeMeasures;
    }

    /** Should the write dump feature and collection bounds? */
    protected abstract boolean isFeatureBounding();

    /** Writes the feature to the output */
    public FeaturesInfo writeFeatures(
            List<FeatureCollection<T, F>> resultsList, boolean isComplex, GeoJSONBuilder jsonWriter) {
        FeaturesInfo featuresInfo;
        if (!isComplex) {
            featuresInfo = encodeSimpleFeatures(jsonWriter, resultsList, isFeatureBounding());
        } else {

            ComplexGeoJsonWriterOptions complexWriterOptions = getComplexGeoJsonWriterOptions(resultsList);
            // encode collection with complex features
            ComplexGeoJsonWriter<T, F> complexWriter = new ComplexGeoJsonWriter<>(jsonWriter, complexWriterOptions) {

                @Override
                protected void writeExtraFeatureProperties(Feature feature, boolean topLevelFeature) {
                    // the various links should be reported only for the top feature, not
                    // for all nested ones
                    if (topLevelFeature) {
                        GeoJSONFeatureWriter.this.writeExtraFeatureProperties(feature, jsonWriter);
                    }
                }
            };
            complexWriter.write(resultsList);
            featuresInfo = new FeaturesInfo(
                    complexWriter.foundCrs(), complexWriter.geometryFound(), complexWriter.getFeaturesCount());
        }
        return featuresInfo;
    }

    private <O> O getFeatureTypeInfoProperty(
            Catalog catalog, FeatureCollection<T, F> features, Function<FeatureTypeInfo, O> callback) {
        FeatureTypeInfo fti;
        ResourceInfo meta = null;
        // if it's a complex feature collection get the proper ResourceInfo
        if (features instanceof TypeInfoCollectionWrapper.Complex<T, F> fcollection) {
            fti = fcollection.getFeatureTypeInfo();
            meta = catalog.getResourceByName(fti.getName(), ResourceInfo.class);
        } else {
            // no complex, normal behavior
            FeatureType featureType = features.getSchema();
            meta = catalog.getResourceByName(featureType.getName(), ResourceInfo.class);
        }
        if (meta instanceof FeatureTypeInfo info) {
            fti = info;
            return callback.apply(fti);
        }
        return null;
    }

    /** Writes a OGC API - Features compliant timeStamp collection attribute */
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
    protected void writeCollectionCounts(BigInteger featureCount, long numberReturned, GeoJSONBuilder jsonWriter) {
        // counts
        if (featureCount != null) {
            jsonWriter.key("totalFeatures").value(featureCount);
            // OGC API - Features suggested name for the same concept as totalFeatures
            jsonWriter.key("numberMatched").value(featureCount);
        } else {
            jsonWriter.key("totalFeatures").value("unknown");
            // in OGC API - Features the suggestion is not to include the element
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    /** Writes the collection bounds */
    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection<T, F>> resultsList,
            boolean hasGeom) {
        // Bounding box for featurecollection
        if (hasGeom && featureBounding) {
            CoordinateReferenceSystem crs = null;
            ReferencedEnvelope e = null;
            for (FeatureCollection collection : resultsList) {
                FeatureType schema = collection.getSchema();
                if (crs == null && schema.getGeometryDescriptor() != null)
                    crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
                if (e == null) {
                    e = collection.getBounds();
                } else {
                    e.expandToInclude(collection.getBounds());
                }
            }

            if (e != null) {
                jsonWriter.setAxisOrder(CRS.getAxisOrder(crs));
                jsonWriter.writeBoundingBox(e);
            }
        }
    }

    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs) throws IOException {
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

    /** Container class for information related with a group of features. */
    public static class FeaturesInfo {

        final CoordinateReferenceSystem crs;
        final boolean hasGeometry;
        public long featureCount;

        protected FeaturesInfo(CoordinateReferenceSystem crs, boolean hasGeometry, long featureCount) {
            this.crs = crs;
            this.hasGeometry = hasGeometry;
            this.featureCount = featureCount;
        }
    }

    protected FeaturesInfo encodeSimpleFeatures(
            GeoJSONBuilder jsonWriter, List<FeatureCollection<T, F>> resultsList, boolean featureBounding) {
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
                    } else if (!id_option.isEmpty()) {
                        // a specific attribute was nominated to be used as id
                        Object value = simpleFeature.getAttribute(id_option);
                        jsonWriter.key("id").value(value);
                    }
                    // set that axis order that should be used to write geometries
                    GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
                    if (defaultGeomType != null) {
                        CoordinateReferenceSystem featureCrs = defaultGeomType.getCoordinateReferenceSystem();
                        jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                        if (crs == null) {
                            crs = featureCrs;
                        }
                    } else {
                        // If we don't know, assume EAST_NORTH so that no swapping occurs
                        jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                    }
                    // start writing the simple feature geometry JSON object
                    Geometry aGeom = (Geometry) simpleFeature.getDefaultGeometry();
                    hasGeom |= aGeom != null;
                    if (aGeom != null || writeNullGeometries()) {
                        writeGeometry(jsonWriter, defaultGeomType, aGeom);
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
                            if (isNaN(value)) {
                                jsonWriter.value(null);
                            } else if (isPositiveInfinity(value)) {
                                jsonWriter.value("Infinity");
                            } else if (isNegativeInfinity(value)) {
                                jsonWriter.value("-Infinity");
                            } else {
                                jsonWriter.value(value);
                            }
                        }
                    }
                    jsonWriter.endObject(); // end the properties

                    // Bounding box for feature in properties
                    ReferencedEnvelope refenv = ReferencedEnvelope.reference(simpleFeature.getBounds());
                    if (featureBounding && !refenv.isEmpty()) {
                        jsonWriter.writeBoundingBox(refenv);
                    }

                    writeExtraFeatureProperties(simpleFeature, jsonWriter);

                    jsonWriter.endObject(); // end the feature
                }
            }
        }
        return new FeaturesInfo(crs, hasGeom, featureCount);
    }

    private boolean isNegativeInfinity(Object value) {
        return (value instanceof Double dv && dv == Double.NEGATIVE_INFINITY)
                || (value instanceof Float fv && fv == Float.NEGATIVE_INFINITY);
    }

    private boolean isPositiveInfinity(Object value) {
        return (value instanceof Double dv && dv == Double.POSITIVE_INFINITY)
                || (value instanceof Float fv && fv == Float.POSITIVE_INFINITY);
    }

    private boolean isNaN(Object value) {
        return (value instanceof Double dv && Double.isNaN(dv)) || (value instanceof Float fv && Float.isNaN(fv));
    }

    /**
     * Writes the "geometry" key and its value. May be overridden by subclasses to encode geometry in a different manner
     *
     * @param jsonWriter the GeoJSONBuilder to write to
     * @param descriptor the geometry descriptor
     * @param aGeom the geometry to write
     */
    protected void writeGeometry(GeoJSONBuilder jsonWriter, GeometryDescriptor descriptor, Geometry aGeom) {
        jsonWriter.key("geometry");
        // Write the geometry, whether it is a null or not
        if (aGeom != null) {
            jsonWriter.writeGeom(aGeom);
        } else {
            jsonWriter.value(null);
        }
        if (descriptor != null) {
            jsonWriter.key("geometry_name").value(descriptor.getLocalName());
        }
    }

    /**
     * By spec the geometry should be there and null. This method allows subclasses to go outside of the spec and save
     * some payload.
     *
     * @return
     */
    protected boolean writeNullGeometries() {
        return true;
    }

    protected String getIdOption() {
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
     * @param jsonWriter The {@link GeoJSONBuilder} being used to write the feature
     */
    protected void writeExtraFeatureProperties(Feature feature, GeoJSONBuilder jsonWriter) {}

    private void writeCrs(final GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs) throws FactoryException {
        if (crs != null) {
            String identifier = SrsSyntax.OGC_URN.getSRS(ResourcePool.lookupIdentifier(crs, true));

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
            if (ids != null && !ids.isEmpty()) {
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

    protected String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    private ComplexGeoJsonWriterOptions getComplexGeoJsonWriterOptions(List<FeatureCollection<T, F>> resultsList) {
        List<ComplexGeoJsonWriterOptions> settings = GeoServerExtensions.extensions(ComplexGeoJsonWriterOptions.class);
        ComplexGeoJsonWriterOptions chosen = null;
        for (ComplexGeoJsonWriterOptions setting : settings) {
            if (setting.canHandle(resultsList)) chosen = setting;
        }
        if (chosen == null) chosen = new DefaultComplexGeoJsonWriterOptions();
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Chosen ComplexGeoJsonWriterOptions " + chosen.getClass());
        return chosen;
    }
}
