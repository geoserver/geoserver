/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

import static org.geoserver.featurestemplating.builders.EncodingHints.isSingleFeatureRequest;
import static org.geoserver.ogcapi.v1.features.FeatureService.ITEM_ID;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * OGC API - Features specific version of GeoJsonTemplateGetFeatureResponse, handles additional
 * fields in a different way if the response mime type is application/geo+json
 */
class GeoJSONTemplateGetFeatureResponse
        extends org.geoserver.featurestemplating.ows.wfs.GeoJSONTemplateGetFeatureResponse {

    public GeoJSONTemplateGetFeatureResponse(
            GeoServer gs, TemplateLoader configuration, TemplateIdentifier identifier) {
        super(gs, configuration, identifier);
    }

    @Override
    protected GeoJSONWriter getOutputWriter(OutputStream output) throws IOException {
        return new GeoJSONAPIWriter(
                new JsonFactory().createGenerator(output, JsonEncoding.UTF8), identifier);
    }

    @Override
    protected void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException {
        boolean isGeoJson = identifier.equals(TemplateIdentifier.GEOJSON);
        if (!isGeoJson) {
            super.writeAdditionalFieldsInternal(
                    writer, featureCollection, getFeature, featureCount, bounds);
            return;
        }
        if (!isSingleFeatureRequest()) {
            writer.writeNumberReturned();
            writer.writeTimeStamp();
        }
        String collId = getFeature.getParameters()[0].toString();
        String name = helper.getFeatureType(collId).prefixedName();
        ((GeoJSONAPIWriter) writer)
                .writeLinks(
                        featureCollection.getPrevious(),
                        featureCollection.getNext(),
                        name,
                        getItemId(),
                        getMimeType(null, null));
    }

    private String getItemId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(att -> (String) att.getAttribute(ITEM_ID, SCOPE_REQUEST))
                .orElse(null);
    }
}
