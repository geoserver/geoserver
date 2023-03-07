/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Query;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "dggs-id", "resolutions", "links"})
public class CollectionDocument extends AbstractCollectionDocument<FeatureTypeInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);
    private final DGGSFeatureSource fs;

    FeatureTypeInfo featureType;
    String mapPreviewURL;

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType) throws IOException {
        super(featureType);
        // basic info
        String collectionId = featureType.prefixedName();
        this.id = collectionId;
        this.title = featureType.getTitle();
        this.description = featureType.getAbstract();
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        DateRange timeExtent = TimeExtentCalculator.getTimeExtent(featureType);
        setExtent(new CollectionExtents(bbox, timeExtent));
        this.featureType = featureType;

        String baseUrl = APIRequestInfo.get().getBaseURL();

        // zones links
        Collection<MediaType> zoneFormats =
                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : zoneFormats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/dggs/collections/" + collectionId + "/zones",
                            Collections.singletonMap("f", format.toString()),
                            SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            "zones",
                            format.toString(),
                            collectionId + " items as " + format.toString(),
                            "zones"));
        }

        // DAPA links, if time is available
        DimensionInfo time = featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time != null) {
            Collection<MediaType> dapaFormats =
                    APIRequestInfo.get().getProducibleMediaTypes(CollectionDAPA.class, true);
            for (MediaType format : dapaFormats) {
                String dapaURL =
                        ResponseUtils.buildURL(
                                baseUrl,
                                "ogc/dggs/collections/" + collectionId + "/processes",
                                Collections.singletonMap("f", format.toString()),
                                SERVICE);
                addLink(
                        new Link(
                                dapaURL,
                                "ogc-dapa-processes",
                                format.toString(),
                                "DAPA for " + collectionId + " as " + format.toString(),
                                "ogc-dapa-processes"));
            }
            Collection<MediaType> variablesFormats =
                    APIRequestInfo.get().getProducibleMediaTypes(DAPAVariables.class, true);
            for (MediaType format : variablesFormats) {
                String variablesURL =
                        ResponseUtils.buildURL(
                                baseUrl,
                                "ogc/dggs/collections/" + collectionId + "/variables",
                                Collections.singletonMap("f", format.toString()),
                                SERVICE);
                addLink(
                        new Link(
                                variablesURL,
                                "ogc-dapa-variables",
                                format.toString(),
                                "DAPA variables for " + collectionId + " as " + format.toString(),
                                "ogc-dapa-variables"));
            }
        }

        addSelfLinks("ogc/dggs/v1/collections/" + id);

        // map preview if available
        if (isWMSAvailable(geoServer)) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", featureType.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL = ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, SERVICE);
        }

        // setup resolutions
        DGGSStore dggsStore = (DGGSStore) featureType.getStore().getDataStore(null);
        this.fs = dggsStore.getDGGSFeatureSource(featureType.getNativeName());
    }

    private boolean isWMSAvailable(GeoServer geoServer) {
        ServiceInfo si =
                geoServer.getServices().stream()
                        .filter(s -> "WMS".equals(s.getId()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JsonIgnore
    public FeatureType getSchema() {
        try {
            return featureType.getFeatureType();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed to compute feature type", e);
            return null;
        }
    }

    @JsonIgnore
    public String getMapPreviewURL() {
        return mapPreviewURL;
    }

    public int[] getResolutions() throws IOException {
        UniqueVisitor visitor = new UniqueVisitor(DGGSStore.RESOLUTION);
        fs.getFeatures(Query.ALL).accepts(visitor, null);
        @SuppressWarnings("unchecked")
        List<Integer> list = visitor.getResult().toList();
        int[] resolutions = list.stream().mapToInt(v -> v).toArray();
        return resolutions;
    }

    @JsonProperty("dggs-id")
    public String getDggsId() {
        return fs.getDGGS().getIdentifier();
    }
}
