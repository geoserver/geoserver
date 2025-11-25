/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** A subclass of GeoJSONGetFeatureResponse that encodes a RFC compliant document */
@Component("RFCGeoJSONFeaturesResponse")
public class RFCGeoJSONFeaturesResponse extends GeoJSONGetFeatureResponse {

    @Autowired // Spring is otherwise confused by the presence of the other constructor
    public RFCGeoJSONFeaturesResponse(GeoServer gs) {
        super(gs, OGCAPIMediaTypes.GEOJSON_VALUE);
    }

    /**
     * Courtesy constructor for subclasses extending RCF GeoJSON output
     *
     * @param gs GeoServer
     * @param mimeType MIME type
     */
    protected RFCGeoJSONFeaturesResponse(GeoServer gs, String mimeType) {
        super(gs, mimeType);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return OGCAPIMediaTypes.GEOJSON_VALUE;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // was it a single feature request?
        if (getItemId() != null) {
            writeSingleFeature((FeatureCollectionResponse) value, output, operation);
        } else {
            super.write(value, output, operation);
        }
    }

    /** Returns the featureId, or null if it's missing or the request is not for OGC API Features */
    protected String getItemId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(att -> (String) att.getAttribute(FeatureService.ITEM_ID, RequestAttributes.SCOPE_REQUEST))
                .orElse(null);
    }

    /** Writes a single feature using the facilities provided by the base class */
    private void writeSingleFeature(FeatureCollectionResponse value, OutputStream output, Operation operation)
            throws IOException {
        OutputStreamWriter osw =
                new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
        @SuppressWarnings({"unchecked", "raw"})
        List<FeatureCollection<FeatureType, Feature>> features = (List) value.getFeatures();
        try (BufferedWriter outWriter = new BufferedWriter(osw)) {
            getFeatureWriter(value, operation).writeFeaturesContent(features, osw);
            outWriter.flush();
        }
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            FeatureCollectionResponse response, Operation operation) {
        return new RFCGeoJSONFeatureWriter<>(gs, response, getItemId());
    }

    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            Operation operation) {
        return new RFCGeoJSONFeatureWriter<>(gs, null, getItemId());
    }

    /** capabilities output format string. */
    @Override
    public String getCapabilitiesElementName() {
        return "GeoJSON-RFC";
    }

    @Override
    public boolean canHandle(Operation operation) {
        String operationId = operation.getId();
        if ("GetFeatures".equalsIgnoreCase(operationId)
                || "GetFeature".equalsIgnoreCase(operationId)
                || "GetFeatureWithLock".equalsIgnoreCase(operationId)
                || "getTile".equalsIgnoreCase(operationId)) {
            return operation.getService() != null
                            && "Features".equals(operation.getService().getId())
                    || "WFS".equalsIgnoreCase(operation.getService().getId());
        }
        return false;
    }
}
