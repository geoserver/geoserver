/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.TypeInfoCollectionWrapper;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.json.WFSGeoJSONFeatureWriter;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.springframework.http.MediaType;

/** RFC compliant GeoJSON feature writer */
public class RFCGeoJSONFeatureWriter<T extends FeatureType, F extends Feature> extends WFSGeoJSONFeatureWriter<T, F> {

    protected final String featureId;

    public RFCGeoJSONFeatureWriter(GeoServer gs, FeatureCollectionResponse response, String featureId) {
        super(gs, OGCAPIMediaTypes.GEOJSON_VALUE, response);
        this.featureId = featureId;
    }

    @Override
    protected void writeExtraFeatureProperties(Feature feature, GeoJSONBuilder jb) {
        if (featureId != null) {
            writeLinks(jb, featureId);
        }
    }

    @Override
    protected void writeCollectionCounts(BigInteger featureCount, long numberReturned, GeoJSONBuilder jsonWriter) {
        if (featureCount != null) {
            jsonWriter.key("numberMatched").value(featureCount);
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    @Override
    protected void writeExtraCollectionProperties(List<FeatureCollection<T, F>> featureCollections, GeoJSONBuilder jb) {
        writeLinks(jb, null);
    }

    @Override
    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection<T, F>> resultsList,
            boolean hasGeom) {
        // OGC API - Features does not require collection level bounds
    }

    /**
     * Subclasses can override to adapt links to another service
     *
     * @param jw
     * @param featureId
     */
    protected void writeLinks(GeoJSONBuilder jw, String featureId) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        if (null == requestInfo) {
            // request comes from WFS, not from ogcapi
            return;
        }
        FeatureCollection collection = response.getFeatures().get(0);
        if (collection instanceof TypeInfoCollectionWrapper wrapper) {
            FeatureTypeInfo featureType = wrapper.getFeatureTypeInfo();
            jw.key("links");
            jw.array();
            addLinks(response, jw, featureId, featureType);
            jw.endArray();
        }
    }

    protected void addLinks(
            FeatureCollectionResponse response, GeoJSONBuilder jw, String featureId, FeatureTypeInfo featureType) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        String baseUrl = requestInfo.getBaseURL();

        // paging links
        if (response != null) {
            if (response.getPrevious() != null) {
                jw.writeLink(jw, "Previous page", OGCAPIMediaTypes.GEOJSON_VALUE, "prev", response.getPrevious());
            }
            if (response.getNext() != null) {
                jw.writeLink(jw, "Next page", OGCAPIMediaTypes.GEOJSON_VALUE, "next", response.getNext());
            }
        }
        // alternate/self links
        String basePath = "ogc/features/v1/collections/" + ResponseUtils.urlEncode(featureType.prefixedName());
        Collection<MediaType> formats = requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + ResponseUtils.urlEncode(featureId);
            }
            String href = ResponseUtils.buildURL(
                    baseUrl, path, Collections.singletonMap("f", format.toString()), URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(OGCAPIMediaTypes.GEOJSON_VALUE)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        // back-pointer to the collection
        for (MediaType format : requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href = ResponseUtils.buildURL(
                    baseUrl, basePath, Collections.singletonMap("f", format.toString()), URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
    }
}
