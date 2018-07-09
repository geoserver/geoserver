/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.WFSExtents;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"name", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection")
public class CollectionDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    String name;
    String title;
    String description;
    WFSExtents extent;
    FeatureTypeInfo featureType;
    String mapPreviewURL;

    public CollectionDocument(
            GeoServer geoServer, BaseRequest request, FeatureTypeInfo featureType) {
        // basic info
        String collectionId = NCNameResourceCodec.encode(featureType);
        setName(collectionId);
        setTitle(featureType.getTitle());
        setDescription(featureType.getAbstract());
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new WFSExtents(bbox));
        this.featureType = featureType;

        // links
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        String baseUrl = request.getBaseUrl();
        for (String format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "wfs3/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            Link.REL_ITEM,
                            format,
                            collectionId + " items as " + format,
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
                        .filter(s -> "WMS".equals(s.getName()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JacksonXmlProperty(localName = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
