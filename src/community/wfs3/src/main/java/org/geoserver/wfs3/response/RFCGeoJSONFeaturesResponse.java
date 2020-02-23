/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;
import static org.geoserver.wfs3.DefaultWebFeatureService30.getAvailableFormats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.TileDataRequest;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Version;
import org.geowebcache.config.DefaultGridsets;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RFCGeoJSONFeaturesResponse extends GeoJSONGetFeatureResponse {

    static final ThreadLocal<String> WFS3_FEATURE_ID = new ThreadLocal<String>();

    /** The MIME type requested by WFS3 for GeoJSON Responses */
    public static final String MIME = "application/geo+json";

    private TileDataRequest tileData;
    private DefaultGridsets gridSets;

    public RFCGeoJSONFeaturesResponse(GeoServer gs) {
        super(gs, MIME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // was it a single feature request?
        String requestFeatureId = getWFS3FeatureId();
        if (requestFeatureId != null) {
            try {
                WFS3_FEATURE_ID.set(requestFeatureId);
                writeSingleFeature((FeatureCollectionResponse) value, output, operation);
            } finally {
                WFS3_FEATURE_ID.remove();
            }
        } else {
            super.write(value, output, operation);
        }
    }

    /** Returns the WFS3 featureId, or null if it's missing or the request is not a WFS3 one */
    private String getWFS3FeatureId() {
        Request dr = Dispatcher.REQUEST.get();
        String featureId = null;
        if (dr != null && (new Version(dr.getVersion()).getMajor().equals(3))) {
            Object featureIdValue = dr.getKvp().get("featureId");
            if (featureIdValue instanceof List) {
                featureId = (String) ((List) featureIdValue).get(0);
            }
        }
        return featureId;
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
        String featureId = WFS3_FEATURE_ID.get();
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
        List<String> formats = getAvailableFormats(FeatureCollectionResponse.class);
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
        String basePath = "wfs3/collections/" + urlEncode(NCNameResourceCodec.encode(featureType));
        for (String format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + urlEncode(featureId);
            }
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            path,
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.equals(MIME)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(jw, linkTitle, format, linkType, href);
        }
        // backpointer to the collection
        for (String format : getAvailableFormats(CollectionDocument.class)) {
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            basePath,
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(jw, linkTitle, format, linkType, href);
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

    @Override
    protected GeoJSONBuilder getGeoJSONBuilder(
            FeatureCollectionResponse featureCollection, Writer outWriter) {
        if (tileData.isTileRequest()) {
            try {
                // get CRS
                CoordinateReferenceSystem crs =
                        CRS.decode(
                                gridSets.getGridSet(tileData.getTilingScheme())
                                        .get()
                                        .getSrs()
                                        .toString());
                // Simplified GeoJson builder
                GeoJsonSimplifiedBuilder jsonWriter = new GeoJsonSimplifiedBuilder(outWriter, crs);
                // backward compatibility with super class behavior
                int numDecimals =
                        getNumDecimals(featureCollection.getFeature(), gs, gs.getCatalog());
                jsonWriter.setNumberOfDecimals(numDecimals);
                return jsonWriter;
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }
        return super.getGeoJSONBuilder(featureCollection, outWriter);
    }

    public TileDataRequest getTileData() {
        return tileData;
    }

    public void setTileData(TileDataRequest tileData) {
        this.tileData = tileData;
    }

    public DefaultGridsets getGridSets() {
        return gridSets;
    }

    public void setGridSets(DefaultGridsets gridSets) {
        this.gridSets = gridSets;
    }
}
