/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
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

    /** The MIME type requested by WFS3 for GeoJSON Responses */
    public static final String MIME = "application/geo+json";

    public RFCGeoJSONFeaturesResponse(GeoServer gs) {
        super(gs, MIME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // was it a single feature request?
        if (getItemId() != null) {
            writeSingleFeature((FeatureCollectionResponse) value, output, operation);
        } else {
            super.write(value, output, operation);
        }
    }

    /** Returns the WFS3 featureId, or null if it's missing or the request is not a WFS3 one */
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
        BufferedWriter outWriter = new BufferedWriter(osw);
        FeatureCollectionResponse featureCollection = value;
        boolean isComplex = isComplexFeature(featureCollection);
        GeoJSONBuilder jsonWriter = getGeoJSONBuilder(featureCollection, outWriter);
        writeFeatures(featureCollection.getFeatures(), operation, isComplex, jsonWriter);
        outWriter.flush();
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

    private void writeLinks(
            FeatureCollectionResponse response,
            Operation operation,
            GeoJSONBuilder jw,
            String featureId) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        FeatureTypeInfo featureType = getFeatureType(request);
        String baseUrl = request.getBaseUrl();
        jw.key("links");
        jw.array();
        // paging links
        if (response != null) {
            if (response.getPrevious() != null) {
                writeLink(jw, "Previous page", MIME, "prev", response.getPrevious());
            }
            if (response.getNext() != null) {
                writeLink(jw, "Next page", MIME, "next", response.getNext());
            }
        }
        // alternate/self links
        String basePath =
                "ogc/features/collections/"
                        + ResponseUtils.urlEncode(NCNameResourceCodec.encode(featureType));
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
            if (format.toString().equals(MIME)) {
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
                            Collections.singletonMap("f", formats.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        jw.endArray();
    }

    private FeatureTypeInfo getFeatureType(GetFeatureRequest request) {
        // a WFS3 always has a collection reference, so one query
        Query query = request.getQueries().get(0);
        QName typeName = query.getTypeNames().get(0);
        return gs.getCatalog()
                .getFeatureTypeByName(
                        new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }

    @Override
    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs)
            throws IOException {
        // write the CRS block only if needed
        if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            super.writeCollectionCRS(jsonWriter, crs);
        }
    }

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
        // not needed in WFS3
    }

    /** capabilities output format string. */
    public String getCapabilitiesElementName() {
        return "GeoJSON-RFC";
    }

    @Override
    public boolean canHandle(Operation operation) {
        if ("GetFeature".equalsIgnoreCase(operation.getId())
                || "GetFeatureWithLock".equalsIgnoreCase(operation.getId())
                || "getTile".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);
            if (req.isResultTypeResults()) {
                // call subclass hook
                return canHandleInternal(operation);
            }
        }
        return false;
    }
}
