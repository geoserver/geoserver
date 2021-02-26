/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

import static org.geoserver.ogcapi.features.FeatureService.ITEM_ID;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.configuration.TemplateConfiguration;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.opengis.referencing.FactoryException;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * OGC API - Features specific version of GeoJsonTemplateGetFeatureResponse, handles additional
 * fields in a different way if the response mime type is application/geo+json
 */
class GeoJSONTemplateGetFeatureResponse
        extends org.geoserver.featurestemplating.wfs.GeoJSONTemplateGetFeatureResponse {

    public GeoJSONTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, TemplateIdentifier identifier) {
        super(gs, configuration, identifier);
    }

    @Override
    protected GeoJSONWriter getOutputWriter(OutputStream output) throws IOException {
        return new GeoJSONAPIWriter(new JsonFactory().createGenerator(output, JsonEncoding.UTF8));
    }

    @Override
    protected void writeAdditionFields(
            GeoJSONWriter writer, FeatureCollectionResponse featureCollection, Operation getFeature)
            throws IOException, FactoryException {
        boolean isGeoJson = identifier.equals(TemplateIdentifier.GEOJSON);
        if (!isGeoJson) {
            super.writeAdditionFields(writer, featureCollection, getFeature);
            return;
        }

        writer.writeNumberReturned();
        writer.writeTimeStamp();
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
