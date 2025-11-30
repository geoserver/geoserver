/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.v1.features.CollectionDocument;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ogcapi.v1.features.RFCGeoJSONFeatureWriter;
import org.geoserver.ogcapi.v1.features.RFCGeoJSONFeaturesResponse;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.api.data.FileGroupProvider;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** A subclass of GeoJSONGetFeatureResponse that encodes a RFC compliant document */
@Component
public class STACItemFeaturesResponse extends RFCGeoJSONFeaturesResponse {

    /** The MIME type requested for GeoJSON Responses */
    public static final String MIME = "application/stac+json";

    private final AssetHasher assetHasher;

    public STACItemFeaturesResponse(GeoServer gs, AssetHasher assetLinkHandler) {
        super(gs, MIME);
        this.assetHasher = assetLinkHandler;
    }

    @Override
    protected boolean canHandleInternal(Operation operation) {
        return true;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // was it a single feature request?
        if (getImageId() != null) {
            writeSingleItem((FeatureCollectionResponse) value, output, operation);
        } else {
            super.write(value, output, operation);
        }
    }

    /** Returns the image id, or null if it's missing or the request is not a images API one */
    private String getImageId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(att -> (String) att.getAttribute(ImagesService.IMAGE_ID, RequestAttributes.SCOPE_REQUEST))
                .orElse(null);
    }

    /** Writes a single feature using the facilities provided by the base class */
    private void writeSingleItem(FeatureCollectionResponse value, OutputStream output, Operation operation)
            throws IOException {
        OutputStreamWriter osw =
                new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
        @SuppressWarnings({"unchecked", "raw"})
        List<FeatureCollection<FeatureType, Feature>> features = (List) value.getFeatures();
        try (BufferedWriter outWriter = new BufferedWriter(osw)) {
            getFeatureWriter(operation).writeFeaturesContent(features, osw);
            outWriter.flush();
        }
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            FeatureCollectionResponse response, Operation operation) {
        return new STACGeoJSONFeatureWriter<>(gs, response, getItemId());
    }

    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            Operation operation) {
        return new STACGeoJSONFeatureWriter<>(gs, null, getItemId());
    }

    private void writeAssets(Feature feature, GeoJSONBuilder jw) {
        Object filesCandidate = feature.getUserData().get(GranuleSource.FILES);
        if (!(filesCandidate instanceof FileGroupProvider.FileGroup)) {
            return;
        }
        FileGroupProvider.FileGroup files = (FileGroupProvider.FileGroup) filesCandidate;
        jw.key("assets").array();
        String featureId = feature.getIdentifier().getID();
        writeAssetLink(featureId, files.getMainFile(), jw);
        if (files.getSupportFiles() != null) {
            for (File supportFile : files.getSupportFiles()) {
                writeAssetLink(featureId, supportFile, jw);
            }
        }
        jw.endArray();
    }

    private void writeAssetLink(String featureId, File file, GeoJSONBuilder jw) {
        try {
            jw.object();
            String href = getFileDownloadURL(getParentCollectionId(), featureId, file);
            jw.key("href").value(href);
            jw.key("title").value(file.getName());
            jw.key("type").value(MimeTypeSupport.guessMimeType(file.getName()));
            jw.endObject();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO error while writing assets", e);
        }
    }

    private String getFileDownloadURL(String collectionId, String imageId, File file) throws IOException {
        String fileNameHash = assetHasher.hashFile(file);
        return ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(),
                "ogc/images/v1/collections/"
                        + urlEncode(collectionId)
                        + "/images/"
                        + urlEncode(imageId)
                        + "/assets/"
                        + urlEncode(fileNameHash),
                null,
                URLMangler.URLType.SERVICE);
    }

    private String getParentCollectionId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(a -> a.getAttribute(ImagesService.COLLECTION_ID, RequestAttributes.SCOPE_REQUEST))
                .map(String::valueOf)
                .orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code null}, making {@link WFSGetFeatureOutputFormat#getCapabilitiesElementNames()} not contributing a
     *     result format on the GetCapabilities document for this output format.
     */
    @Override
    public String getCapabilitiesElementName() {
        return null;
    }

    @Override
    public boolean canHandle(Operation operation) {
        if ("GetImages".equalsIgnoreCase(operation.getId()) || "GetImage".equalsIgnoreCase(operation.getId())) {
            // call subclass hook
            return canHandleInternal(operation);
        }
        return false;
    }

    /** RFC compliant GeoJSON feature writer */
    protected class STACGeoJSONFeatureWriter<T extends FeatureType, F extends Feature>
            extends RFCGeoJSONFeatureWriter<T, F> {

        public STACGeoJSONFeatureWriter(GeoServer gs, FeatureCollectionResponse response, String featureId) {
            super(gs, response, featureId);
        }

        @Override
        protected void writeExtraFeatureProperties(Feature feature, GeoJSONBuilder jb) {
            jb.key("stac_version").value("0.8.0");
            jb.key("collection").value(getParentCollectionId());
            writeAssets(feature, jb);
            writeLinks(null, jb, feature.getIdentifier().getID());
        }

        @Override
        protected void writeExtraCollectionProperties(
                List<FeatureCollection<T, F>> featureCollections, GeoJSONBuilder jb) {
            writeLinks(response, jb, null);
        }

        protected void writeLinks(FeatureCollectionResponse response, GeoJSONBuilder jw, String featureId) {
            APIRequestInfo requestInfo = APIRequestInfo.get();
            String baseUrl = requestInfo.getBaseURL();
            String basePath = "ogc/images/v1/collections/" + ResponseUtils.urlEncode(getParentCollectionId());
            jw.key("links");
            jw.array();
            // paging links (only for collections)
            if (response != null && getImageId() == null && featureId == null) {
                if (response.getPrevious() != null) {
                    jw.writeLink(jw, "Previous page", MIME, "prev", response.getPrevious());
                }
                if (response.getNext() != null) {
                    jw.writeLink(jw, "Next page", MIME, "next", response.getNext());
                }
                // links to each an every image at top level (sigh... from spec)
                FeatureCollection fc = response.getFeatures().get(0);
                try (FeatureIterator fi = fc.features()) {
                    while (fi.hasNext()) {
                        Feature next = fi.next();
                        String href = ResponseUtils.buildURL(
                                baseUrl,
                                basePath
                                        + "/images/"
                                        + ResponseUtils.urlEncode(
                                                next.getIdentifier().getID()),
                                Collections.singletonMap("f", MIME),
                                URLMangler.URLType.SERVICE);
                        String linkType = Link.REL_ITEM;
                        jw.writeLink(jw, null, MIME, linkType, href);
                    }
                }
            }
            // alternate/self links
            Collection<MediaType> formats = requestInfo.getProducibleMediaTypes(ImagesResponse.class, true);
            for (MediaType format : formats) {
                String path = basePath + "/images";
                if (featureId != null) {
                    path += "/" + ResponseUtils.urlEncode(featureId);
                }
                String href = ResponseUtils.buildURL(
                        baseUrl, path, Collections.singletonMap("f", format.toString()), URLMangler.URLType.SERVICE);
                String linkType = Link.REL_ALTERNATE;
                String linkTitle = "This document as " + format;
                if (format.toString().equals(MIME)) {
                    linkType = Link.REL_SELF;
                    linkTitle = "This document";
                }
                jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
            }
            // backpointer to the collection, if it was a single feature request, or for the collection
            // only
            if (featureId == null || getImageId() != null) {
                for (MediaType format : requestInfo.getProducibleMediaTypes(ImagesCollectionsDocument.class, true)) {
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

            jw.endArray();
        }

        protected void addLinks(
                FeatureCollectionResponse response,
                GetFeatureRequest request,
                GeoJSONBuilder jw,
                String featureId,
                FeatureTypeInfo featureType) {
            String baseUrl = response.getBaseUrl();
            APIRequestInfo requestInfo = APIRequestInfo.get();

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
                        baseUrl,
                        basePath,
                        Collections.singletonMap("f", format.toString()),
                        URLMangler.URLType.SERVICE);
                String linkType = Link.REL_COLLECTION;
                String linkTitle = "The collection description as " + format;
                jw.writeLink(jw, linkTitle, format.toString(), linkType, href);
            }
        }

        @Override
        protected void writeCollectionBounds(
                boolean featureBounding,
                GeoJSONBuilder jsonWriter,
                List<FeatureCollection<T, F>> resultsList,
                boolean hasGeom) {
            // STAC does not require collection level bounds
        }
    }
}
