/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.response.WFSResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.Version;
import org.opengis.feature.type.FeatureType;

/**
 * Base class for a response to a WFS GetFeature operation.
 *
 * <p>The result of a GetFeature operation is an instance of {@link FeatureCollectionResponse}.
 * Subclasses are responsible for serializing an instance of this type in {@link
 * #write(FeatureCollectionResponse, OutputStream, Operation)}.
 *
 * <p>Subclasses also need declare the mime-type in which the format is encoded.
 *
 * @author Gabriel Rold?n, Axios Engineering
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class WFSGetFeatureOutputFormat extends WFSResponse {

    /** Based on definition of valid xml element name at http://www.w3.org/TR/xml/#NT-Name */
    static final Pattern XML_ELEMENT =
            Pattern.compile(
                    "[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02ff\\u0370-\\u037d"
                            + "\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f\\u2c00-\\u2fef\\u3001-\\ud7ff"
                            + "\\uf900-\\ufdcf\\ufdf0-\\ufffd\\x10000-\\xEFFFF]"
                            + "[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6"
                            + "\\u00F8-\\u02ff\\u0370-\\u037d\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f"
                            + "\\u2c00-\\u2fef\\u3001-\\udfff\\uf900-\\ufdcf\\ufdf0-\\ufffd\\\\x10000-\\\\xEFFFF\\-\\.0-9"
                            + "\\u00b7\\u0300-\\u036f\\u203f-\\u2040]*\\Z");

    /** logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");
    /**
     * Constructor which sets the outputFormat.
     *
     * @param outputFormat The well-known name of the format, not <code>null</code>
     */
    public WFSGetFeatureOutputFormat(GeoServer gs, String outputFormat) {
        super(gs, FeatureCollectionResponse.class, outputFormat);
    }

    /**
     * Constructor which sets the outputFormats.
     *
     * @param outputFormats Set of well-known name of the format, not <code>null</code>
     */
    public WFSGetFeatureOutputFormat(GeoServer gs, Set<String> outputFormats) {
        super(gs, FeatureCollectionResponse.class, outputFormats);
    }

    /**
     * Returns the mime type <code>text/xml</code>.
     *
     * <p>Subclasses should override this method to provide a diffent output format.
     */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    /**
     * Ensures that the operation being executed is a GetFeature operation.
     *
     * <p>Subclasses may implement
     */
    public boolean canHandle(Operation operation) {
        // GetFeature operation?
        if ("GetFeature".equalsIgnoreCase(operation.getId())
                || "GetFeatureWithLock".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);
            if (req.isResultTypeResults()) {
                // call subclass hook
                return canHandleInternal(operation);
            }
        }

        return false;
    }

    /**
     * Capabilities output format string. Something that's a valid XML element name. This should be
     * overriden in each outputformat subclass, and if it's not a warning will be issued.
     */
    public String getCapabilitiesElementName() {
        String of = getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
        if (of == null) {
            return null;
        }

        // wfs 1.1 form is not a valid xml element, do a check
        if (XML_ELEMENT.matcher(of).matches()) {
            return of;
        } else {
            LOGGER.severe(
                    "ERROR IN "
                            + this.getClass()
                            + " IMPLEMENTATION.  getCapabilitiesElementName() should return a"
                            + "valid XML element name string for use in the WFS 1.0.0 capabilities document.");
            String name = this.getClass().getName();
            if (name.indexOf('.') != -1) {
                name = name.substring(name.lastIndexOf('.') + 1);
            }

            return name;
        }
    }

    /**
     * Returns the list of output format names generated by this format, for inclusion in the WFS
     * 1.0 capabilities document as XML element names
     */
    public List<String> getCapabilitiesElementNames() {
        String name = getCapabilitiesElementName();
        if (name == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(name);
        }
    }

    /**
     * Subclasses can delegate to this method if they want the full list of valid output format
     * element names to be returned in the WFS 1.0 capabilities
     */
    protected List<String> getAllCapabilitiesElementNames() {
        List<String> result = new ArrayList<String>();
        for (String name : getOutputFormats()) {
            if (XML_ELEMENT.matcher(name).matches()) {
                result.add(name);
            }
        }

        // have the output order be independent of the used JDK
        Collections.sort(result);

        return result;
    }

    /**
     * Allows to have version specific output formats. By default a WFS format is allowed on every
     * version, override to filter specific ones
     */
    public boolean canHandle(Version version) {
        return true;
    }

    /**
     * Hook for subclasses to add addtional checks to {@link #canHandle(Operation)}.
     *
     * <p>Subclasses may override this method if need be, the default impelementation returns <code>
     * true</code>
     *
     * @param operation The operation being performed.
     * @return <code>true</code> if the output format can handle the operation, otherwise <code>
     *     false</code>
     */
    protected boolean canHandleInternal(Operation operation) {
        return true;
    }

    /** Calls through to {@link #write(FeatureCollectionResponse, OutputStream, Operation)}. */
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // for WFS 2.0 we changed the input object type to be the request object adapter, but there
        // is other code (like WMS GetFeatureInfo) that passes in the old objects, so do a check
        if (value instanceof FeatureCollectionResponse) {
            write((FeatureCollectionResponse) value, output, operation);
        } else {
            write(FeatureCollectionResponse.adapt(value), output, operation);
        }
    }

    private <T> T getFeatureTypeInfoProperty(
            Catalog catalog, FeatureCollection features, Function<FeatureTypeInfo, T> callback) {
        FeatureTypeInfo fti;
        ResourceInfo meta = null;
        // if it's a complex feature collection get the proper ResourceInfo
        if (features instanceof TypeInfoCollectionWrapper.Complex) {
            TypeInfoCollectionWrapper.Complex fcollection =
                    (TypeInfoCollectionWrapper.Complex) features;
            fti = fcollection.getFeatureTypeInfo();
            meta = catalog.getResourceByName(fti.getName(), ResourceInfo.class);
        } else {
            // no complex, normal behavior
            FeatureType featureType = features.getSchema();
            meta = catalog.getResourceByName(featureType.getName(), ResourceInfo.class);
        }
        if (meta instanceof FeatureTypeInfo) {
            fti = (FeatureTypeInfo) meta;
            return callback.apply(fti);
        }
        return null;
    }

    protected int getNumDecimals(List featureCollections, GeoServer geoServer, Catalog catalog) {
        int numDecimals = -1;
        for (int i = 0; i < featureCollections.size(); i++) {
            Integer ftiDecimals =
                    getFeatureTypeInfoProperty(
                            catalog,
                            (FeatureCollection) featureCollections.get(i),
                            fti -> fti.getNumDecimals());

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
            List featureCollections, GeoServer geoServer, Catalog catalog) {
        boolean padWithZeros = false;
        for (int i = 0; i < featureCollections.size(); i++) {
            Boolean pad =
                    getFeatureTypeInfoProperty(
                            catalog,
                            (FeatureCollection) featureCollections.get(i),
                            fti -> fti.getPadWithZeros());
            if (Boolean.TRUE.equals(pad)) {
                padWithZeros = true;
            }
        }
        return padWithZeros;
    }

    protected boolean getForcedDecimal(
            List featureCollections, GeoServer geoServer, Catalog catalog) {
        boolean forcedDecimal = false;
        for (int i = 0; i < featureCollections.size(); i++) {
            Boolean forced =
                    getFeatureTypeInfoProperty(
                            catalog,
                            (FeatureCollection) featureCollections.get(i),
                            fti -> fti.getForcedDecimal());
            if (Boolean.TRUE.equals(forced)) {
                forcedDecimal = true;
            }
        }
        return forcedDecimal;
    }

    /**
     * Helper method that checks if coordinates measured values should be encoded for the provided
     * feature collections. By default coordinates measures are not encoded.
     *
     * @param featureCollections features collections
     * @param catalog GeoServer catalog
     * @return TRUE if coordinates measures should be encoded, otherwise FALSE
     */
    protected boolean encodeMeasures(List featureCollections, Catalog catalog) {
        boolean encodeMeasures = true;
        for (int i = 0; i < featureCollections.size(); i++) {
            Boolean measures =
                    getFeatureTypeInfoProperty(
                            catalog,
                            (FeatureCollection) featureCollections.get(i),
                            fti -> fti.getEncodeMeasures());
            if (Boolean.FALSE.equals(measures)) {
                // no measures should be encoded
                encodeMeasures = false;
            }
        }
        return encodeMeasures;
    }

    /**
     * Serializes the feature collection in the format declared.
     *
     * @param featureCollection The feature collection.
     * @param output The output stream to serialize to.
     * @param getFeature The GetFeature operation descriptor.
     */
    protected abstract void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException;

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        FeatureCollectionResponse response;
        if (value instanceof FeatureCollectionResponse) {
            response = (FeatureCollectionResponse) value;
        } else {
            response = FeatureCollectionResponse.adapt(value);
        }
        final String fileName;
        if (response.getTypeNames() != null) {
            fileName =
                    response.getTypeNames()
                            .stream()
                            .map(tn -> tn.getLocalPart())
                            .collect(Collectors.joining("_"));
        } else if (response.getTypeName() != null) {
            fileName = response.getTypeName().getLocalPart();
        } else {
            fileName = "features";
        }
        return fileName + "." + getExtension(response);
    }

    /** Sets the rigth extension for the response */
    protected String getExtension(FeatureCollectionResponse response) {
        String mimeType = getMimeType(null, null);
        if (mimeType != null) {
            // guesswork
            if (mimeType.contains("gml")) {
                return "xml";
            } else if (mimeType.contains("json")) {
                return "json";
            }
            // otehrwise use the default
            String[] typeParts = mimeType.split(";");
            return typeParts[0].split("/")[0];
        } else {
            return "bin";
        }
    }
}
