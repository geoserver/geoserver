/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection", namespace = "http://www.opengis.net/wfs/3.0")
public class CollectionDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    String id;
    String title;
    String description;
    WFSExtents extent;
    FeatureTypeInfo featureType;
    String mapPreviewURL;

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType) {
        // basic info
        String collectionId = NCNameResourceCodec.encode(featureType);
        setId(collectionId);
        setTitle(featureType.getTitle());
        setDescription(featureType.getAbstract());
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new WFSExtents(bbox));
        this.featureType = featureType;

        // links
        Collection<MediaType> formats =
                APIRequestInfo.get().getProducibleMediaTypes(FeatureCollectionResponse.class, true);
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/features/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            Link.REL_ITEM,
                            format.toString(),
                            collectionId + " items as " + format.toString(),
                            "items"));
        }

        // map preview
        if (isWMSAvailable(geoServer)) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", featureType.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL =
                    ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, URLMangler.URLType.SERVICE);
        }
    }

    private boolean isWMSAvailable(GeoServer geoServer) {
        ServiceInfo si =
                geoServer
                        .getServices()
                        .stream()
                        .filter(s -> "WMS".equals(s.getId()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JacksonXmlProperty(localName = "Id")
    public String getId() {
        return id;
    }

    public void setId(String collectionId) {
        id = collectionId;
    }

    @JacksonXmlProperty(localName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JacksonXmlProperty(localName = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WFSExtents getExtent() {
        return extent;
    }

    public void setExtent(WFSExtents extent) {
        this.extent = extent;
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
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
}
