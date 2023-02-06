/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** A subclass of GeoJSONGetFeatureResponse that encodes a RFC compliant document */
@Component
public class RFCGeoJSONFeaturesResponse extends GeoJSONGetFeatureResponse {

    public RFCGeoJSONFeaturesResponse(GeoServer gs) {
        super(gs, OGCAPIMediaTypes.GEOJSON_VALUE);
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
    private String getItemId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(
                        att ->
                                (String)
                                        att.getAttribute(
                                                FeatureService.ITEM_ID,
                                                RequestAttributes.SCOPE_REQUEST))
                .orElse(null);
    }

    /** Writes a single feature using the facilities provided by the base class */
    private void writeSingleFeature(
            FeatureCollectionResponse value, OutputStream output, Operation operation)
            throws IOException {
        OutputStreamWriter osw =
                new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
        try (BufferedWriter outWriter = new BufferedWriter(osw)) {
            FeatureCollectionResponse featureCollection = value;
            boolean isComplex = isComplexFeature(featureCollection);
            GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollection, outWriter);
            writeFeatures(featureCollection.getFeatures(), operation, isComplex, jsonWriter);
            outWriter.flush();
        }
    }

    @Override
    protected void writeExtraFeatureProperties(
            Feature feature, Operation operation, GeoJSONBuilder jw) {
        String featureId = getItemId();
        if (featureId != null) {
            writeLinks(null, operation, jw, featureId);
        }
    }

    @Override
    protected void writePagingLinks(
            FeatureCollectionResponse response, Operation operation, GeoJSONBuilder jw) {
        // we have more than just paging links here
        writeLinks(response, operation, jw, null);
    }

    /**
     * Subclasses can override to adapt links to another service
     *
     * @param response
     * @param operation
     * @param jw
     * @param featureId
     */
    protected void writeLinks(
            FeatureCollectionResponse response,
            Operation operation,
            GeoJSONBuilder jw,
            String featureId) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        if (null == requestInfo) {
            // request comes from WFS, not from ogcapi
            return;
        }
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        FeatureTypeInfo featureType = getFeatureType(request);
        String baseUrl = request.getBaseUrl();
        jw.key("links");
        jw.array();
        // paging links
        if (response != null) {
            if (response.getPrevious() != null) {
                writeLink(
                        jw,
                        "Previous page",
                        OGCAPIMediaTypes.GEOJSON_VALUE,
                        "prev",
                        response.getPrevious());
            }
            if (response.getNext() != null) {
                writeLink(
                        jw,
                        "Next page",
                        OGCAPIMediaTypes.GEOJSON_VALUE,
                        "next",
                        response.getNext());
            }
        }
        // alternate/self links
        String basePath =
                "ogc/features/v1/collections/"
                        + ResponseUtils.urlEncode(featureType.prefixedName());
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + ResponseUtils.urlEncode(featureId);
            }
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            path,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(OGCAPIMediaTypes.GEOJSON_VALUE)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        // backpointer to the collection
        for (MediaType format :
                requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            basePath,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        jw.endArray();
    }

    protected FeatureTypeInfo getFeatureType(GetFeatureRequest request) {
        // OGC API Features always have a collection reference, so one query
        return Optional.ofNullable(request.getQueries())
                .filter(qs -> qs.size() > 0)
                .map(qs -> qs.get(0))
                .map(q -> q.getTypeNames())
                .filter(tns -> tns.size() > 0)
                .map(tns -> tns.get(0))
                .map(tn -> new NameImpl(tn.getNamespaceURI(), tn.getLocalPart()))
                .map(tn -> gs.getCatalog().getFeatureTypeByName(tn))
                .orElse(null);
    }

    @Override
    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs)
            throws IOException {
        // write the CRS block only if needed
        if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            super.writeCollectionCRS(jsonWriter, crs);
        }
    }

    @Override
    protected void writeCollectionCounts(
            BigInteger featureCount, long numberReturned, GeoJSONBuilder jsonWriter) {
        // counts
        if (featureCount != null) {
            jsonWriter.key("numberMatched").value(featureCount);
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    @Override
    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList,
            boolean hasGeom) {
        // not needed in OGC API for Features
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
                    && "Features".equals(operation.getService().getId());
        }
        return false;
    }
}
