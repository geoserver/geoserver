/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geotools.util.logging.Logging;

/**
 * A class representing the GeoVolumes "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class GeoVolumes extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(GeoVolumes.class);
    private List<GeoVolume> collections;
    // Map to hold other properties
    private Map<String, Object> additionalProperties = new HashMap<>();

    protected GeoVolumes() {}

    public GeoVolumes(List<GeoVolume> collections) {
        this.collections = collections;
    }

    public GeoVolumes(GeoVolumes other) {
        this.collections = other.collections;
        this.additionalProperties.putAll(other.additionalProperties);
    }

    @Override
    public List<Link> getLinks() {
        return links;
    }

    public List<GeoVolume> getCollections() {
        return collections;
    }

    // Getter for additional properties
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    // This method will be called for any unrecognized properties
    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    public void updateLinks() {
        String basePath = "ogc/3dgeovolumes/v1/collections/";
        addSelfLinks(basePath);
        for (GeoVolume collection : collections) {
            collection.updateLinks();
        }

        String baseURL = APIRequestInfo.get().getBaseURL();
        for (Link link : getLinks()) {
            String updated = LinkUpdater.updateLink(baseURL, basePath, link.getHref());
            link.setHref(updated);
        }

        // update the links in the additionalProperties map
        LinkUpdater.updateLinks(baseURL, basePath, additionalProperties);
    }
}
