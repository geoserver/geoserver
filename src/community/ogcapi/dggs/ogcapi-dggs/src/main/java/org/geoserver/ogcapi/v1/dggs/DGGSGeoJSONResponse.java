/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.TypeInfoCollectionWrapper;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ogcapi.v1.features.RFCGeoJSONFeatureWriter;
import org.geoserver.ogcapi.v1.features.RFCGeoJSONFeaturesResponse;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** A Geo+JSON response adapted to the DGGS service needs */
@Component
public class DGGSGeoJSONResponse extends RFCGeoJSONFeaturesResponse {

    public DGGSGeoJSONResponse(GeoServer gs) {
        super(gs);
    }

    @Override
    public boolean canHandle(Operation operation) {
        return operation.getService() != null
                && "DGGS".equals(operation.getService().getId());
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            FeatureCollectionResponse response, Operation operation) {
        return new DGGSGeoJSONWriter<>(gs, operation, response, getItemId());
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            Operation operation) {
        return new DGGSGeoJSONWriter<>(gs, operation, null, getItemId());
    }

    static class DGGSGeoJSONWriter<T extends FeatureType, F extends Feature> extends RFCGeoJSONFeatureWriter<T, F> {
        public DGGSGeoJSONWriter(
                GeoServer gs, Operation operation, FeatureCollectionResponse response, String featureId) {
            super(gs, response, featureId);
        }

        @Override
        protected void writeLinks(GeoJSONBuilder jw, String featureId) {
            APIRequestInfo requestInfo = APIRequestInfo.get();
            if (null == requestInfo) {
                // request comes from WFS, not from ogcapi
                return;
            }
            String baseUrl = APIRequestInfo.get().getBaseURL();
            jw.key("links");
            jw.array();
            // paging links
            if (response != null) {
                if (response.getPrevious() != null) {
                    jw.writeLink(jw, "Previous page", OGCAPIMediaTypes.GEOJSON_VALUE, "prev", response.getPrevious());
                }
                if (response.getNext() != null) {
                    jw.writeLink(jw, "Next page", OGCAPIMediaTypes.GEOJSON_VALUE, "next", response.getNext());
                }
            }
            Collection<MediaType> formats = requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
            for (MediaType format : formats) {
                Map<String, String> kvp = APIRequestInfo.get().getSimpleQueryMap();
                kvp.put("f", format.toString());
                String href = ResponseUtils.buildURL(
                        baseUrl, APIRequestInfo.get().getRequestPath(), kvp, URLMangler.URLType.SERVICE);
                String linkType = Link.REL_ALTERNATE;
                String linkTitle = "This document as " + format;
                if (format.toString().equals(OGCAPIMediaTypes.GEOJSON_VALUE)) {
                    linkType = Link.REL_SELF;
                    linkTitle = "This document";
                }
                jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
            }
            // backpointer to the collection
            if (response != null && response.getFeatures().get(0) instanceof TypeInfoCollectionWrapper wrapper) {
                FeatureTypeInfo featureType = wrapper.getFeatureTypeInfo();
                if (featureType != null) {
                    String basePath = "ogc/dggs/v1/collections/" + ResponseUtils.urlEncode(featureType.prefixedName());
                    for (MediaType format : requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
                        String href = ResponseUtils.buildURL(
                                baseUrl,
                                basePath,
                                Collections.singletonMap("f", format.toString()),
                                URLMangler.URLType.SERVICE);
                        String linkType = Link.REL_COLLECTION;
                        String linkTitle = "The collection description as " + format;
                        jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
                    }
                }
            }
            jw.endArray();
        }
    }
}
