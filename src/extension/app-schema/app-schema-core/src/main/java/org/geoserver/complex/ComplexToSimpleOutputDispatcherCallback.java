/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.complex;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.ComplexFeatureAwareFormat;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * GeoServer dispatcher callback implementation for complex to simple features transformation. Will
 * work only on simple features output marked formats.
 */
@Service
public class ComplexToSimpleOutputDispatcherCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER =
            Logging.getLogger(ComplexToSimpleOutputDispatcherCallback.class);

    private static final String GET_FEATURE = "GetFeature";
    private static final String WFS = "WFS";

    private final GeoServer geoServer;

    @Autowired
    public ComplexToSimpleOutputDispatcherCallback(@Qualifier("geoServer") GeoServer geoServer) {
        super();
        this.geoServer = geoServer;
        LOGGER.config("ComplexToSimpleOutputDispatcherCallback instance is activated");
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        try {
            logRequest(request, operation, result);
            if (request == null
                    || result == null
                    || !isSupported(request, result, operation)
                    || !isConvertActivated(request)) return null;
            LOGGER.log(Level.FINE, () -> "Support found for request: " + request);
            Catalog catalog = geoServer.getCatalog();

            ComplexToSimpleOutputHandler handler =
                    new ComplexToSimpleOutputHandler(
                            request, (FeatureCollectionResponse) result, catalog);
            return handler.execute();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error executing the dispatcher callback", ex);
        }
        return null;
    }

    private void logRequest(Request request, Operation operation, Object result) {
        LOGGER.log(
                Level.FINE,
                () ->
                        "Checking support for request: "
                                + request
                                + " | operation: "
                                + operation
                                + " | result: "
                                + result);
    }

    private boolean isConvertActivated(Request request) {
        // check the service
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        if (wfsInfo.isSimpleConversionEnabled()) return true;
        // check the layer configuration
        QName layerName = ComplexToSimpleOutputCommons.getLayerName(request);
        if (layerName != null) {
            LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(new NameImpl(layerName));
            return layerInfo != null && layerInfo.getResource().isSimpleConversionEnabled();
        }
        return false;
    }

    private boolean isSupported(Request request, Object result, Operation operation) {
        return isRequestSupported(request, operation, result) && isSupportedResult(result);
    }

    private boolean isSupportedResult(Object result) {
        if (!(result instanceof FeatureCollectionResponse)) return false;
        FeatureCollectionResponse fcResponse = (FeatureCollectionResponse) result;
        List<FeatureCollection> featureCollectionList = fcResponse.getFeature();
        // there is only support for single collection responses
        if (featureCollectionList == null || featureCollectionList.size() > 1) return false;
        // finally check the feature type support
        FeatureCollection featureCollection = featureCollectionList.get(0);
        if (featureCollection == null) return false;
        FeatureType featureType = featureCollection.getSchema();
        return isSupported(featureType);
    }

    private boolean isRequestSupported(Request request, Operation operation, Object result) {
        // check service is WFS and request is GetFeature
        return (WFS.equalsIgnoreCase(request.getService()))
                && (GET_FEATURE.equalsIgnoreCase(request.getRequest())
                        && isOutputFormatSupported(request.getOutputFormat(), operation, result));
    }

    private boolean isOutputFormatSupported(
            String outputFormat, Operation operation, Object result) {
        if (StringUtils.isBlank(outputFormat)) return false;
        return isSupported(outputFormat, result, operation);
    }

    /**
     * Checks if the provided feature type is supported. It must be a complex feature type.
     *
     * @param featureType the feature type to check
     * @return true is the feature type is supported (it is a complex type)
     */
    private boolean isSupported(FeatureType featureType) {
        return !(featureType instanceof SimpleFeatureType);
    }

    /** Checks if the provided format is supported by complex to simple features conversion. */
    private boolean isSupported(String format, Object value, Operation operation) {
        if (StringUtils.isBlank(format)) return false;
        // get a list of output format that don't support complex features
        List<WFSGetFeatureOutputFormat> formats =
                GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class).stream()
                        .filter(of -> !supportsComplexFeatures(of, value, operation))
                        .collect(Collectors.toList());
        // check if format is on the resulting list
        for (WFSGetFeatureOutputFormat outputFormat : formats) {
            if (format.equalsIgnoreCase(outputFormat.getCapabilitiesElementName())
                    || format.equalsIgnoreCase(outputFormat.getMimeType(value, operation)))
                return true;
        }
        return false;
    }

    private boolean supportsComplexFeatures(
            WFSGetFeatureOutputFormat outputFormat, Object value, Operation operation) {
        if (!(outputFormat instanceof ComplexFeatureAwareFormat)) return false;
        return ((ComplexFeatureAwareFormat) outputFormat).supportsComplexFeatures(value, operation);
    }
}
