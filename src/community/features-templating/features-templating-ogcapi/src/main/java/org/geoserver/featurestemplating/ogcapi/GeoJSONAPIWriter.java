/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

import static org.geoserver.featurestemplating.builders.EncodingHints.isSingleFeatureRequest;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.v1.features.CollectionDocument;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.MediaType;

public class GeoJSONAPIWriter extends GeoJSONWriter {

    public GeoJSONAPIWriter(JsonGenerator generator, TemplateIdentifier identifier) {
        super(generator, identifier);
    }

    @Override
    public void startTemplateOutput(EncodingHints encodingHints) throws IOException {
        boolean isGeoJSON = identifier.equals(TemplateIdentifier.GEOJSON);
        if (isSingleFeatureRequest() && isGeoJSON) startObject(null, encodingHints);
        else super.startTemplateOutput(encodingHints);
    }

    public void writeLinks(
            String previous, String next, String prefixedName, String featureId, String mimeType)
            throws IOException {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        startArray("links", null);
        // paging links
        if (previous != null) {
            writeLink(previous, "prev", mimeType, "Previous page", null);
        }
        if (next != null) {
            writeLink(next, "next", mimeType, "Next page", null);
        }
        // alternate/self links
        String basePath = "ogc/features/v1/collections/" + ResponseUtils.urlEncode(prefixedName);
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + ResponseUtils.urlEncode(featureId);
            }
            String href =
                    ResponseUtils.buildURL(
                            requestInfo.getBaseURL(),
                            path,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(mimeType)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(href, linkType, format.toString(), linkTitle, null);
        }
        // backpointer to the collection
        for (MediaType format :
                requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href =
                    ResponseUtils.buildURL(
                            requestInfo.getBaseURL(),
                            basePath,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(href, linkType, format.toString(), linkTitle, null);
        }
        endArray(null, null);
    }

    @Override
    public void startObject(String name, EncodingHints encodingHints) throws IOException {
        if (!skipObjectWriting(encodingHints)) super.startObject(name, encodingHints);
    }

    @Override
    public void endObject(String name, EncodingHints encodingHints) throws IOException {
        if (!skipObjectWriting(encodingHints)) super.endObject(name, encodingHints);
    }
}
