/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.data.FileGroupProvider;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
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
public class STACItemFeaturesResponse extends GeoJSONGetFeatureResponse {

    /** The MIME type requested by WFS3 for GeoJSON Responses */
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
                .map(
                        att ->
                                (String)
                                        att.getAttribute(
                                                ImagesService.IMAGE_ID,
                                                RequestAttributes.SCOPE_REQUEST))
                .orElse(null);
    }

    /** Writes a single feature using the facilities provided by the base class */
    private void writeSingleItem(
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
        jw.key("stac_version").value("0.8.0");
        jw.key("collection").value(getParentCollectionId());
        writeAssets(feature, jw);
        writeLinks(null, operation, jw, feature.getIdentifier().getID());
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

    private String getFileDownloadURL(String collectionId, String imageId, File file)
            throws IOException {
        String fileNameHash = assetHasher.hashFile(file);
        return ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(),
                "ogc/images/collections/"
                        + urlEncode(collectionId)
                        + "/images/"
                        + urlEncode(imageId)
                        + "/assets/"
                        + urlEncode(fileNameHash),
                null,
                URLMangler.URLType.SERVICE);
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
        String baseUrl = requestInfo.getBaseURL();
        String basePath =
                "ogc/images/collections/" + ResponseUtils.urlEncode(getParentCollectionId());
        jw.key("links");
        jw.array();
        // paging links (only for collections)
        if (response != null && getImageId() == null && featureId == null) {
            if (response.getPrevious() != null) {
                writeLink(jw, "Previous page", MIME, "prev", response.getPrevious());
            }
            if (response.getNext() != null) {
                writeLink(jw, "Next page", MIME, "next", response.getNext());
            }
            // links to each an every image at top level (sigh... from spec)
            FeatureCollection fc = response.getFeatures().get(0);
            try (FeatureIterator fi = fc.features()) {
                while (fi.hasNext()) {
                    Feature next = fi.next();
                    String href =
                            ResponseUtils.buildURL(
                                    baseUrl,
                                    basePath
                                            + "/images/"
                                            + ResponseUtils.urlEncode(next.getIdentifier().getID()),
                                    Collections.singletonMap("f", MIME),
                                    URLMangler.URLType.SERVICE);
                    String linkType = Link.REL_ITEM;
                    writeLink(jw, null, MIME, linkType, href);
                }
            }
        }
        // alternate/self links
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(ImagesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/images";
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
        // backpointer to the collection, if it was a single feature request, or for the collection
        // only
        if (featureId == null || getImageId() != null) {
            for (MediaType format :
                    requestInfo.getProducibleMediaTypes(ImagesCollectionsDocument.class, true)) {
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
        }

        jw.endArray();
    }

    private String getParentCollectionId() {
        return String.valueOf(
                RequestContextHolder.getRequestAttributes()
                        .getAttribute(
                                ImagesService.COLLECTION_ID, RequestAttributes.SCOPE_REQUEST));
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canHandle(Operation operation) {
        if ("GetImages".equalsIgnoreCase(operation.getId())) {
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
