/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.Link;

/** Describes a GeoVolumes collection. */
public class GeoVolume extends AbstractCollectionDocument<Void> {

    protected final List<Link> content = new ArrayList<>();

    @JsonProperty("collectiontype")
    private String collectionType;

    // Map to hold other properties
    private Map<String, Object> additionalProperties = new HashMap<>();

    /** Builds a {@link GeoVolume} */
    public GeoVolume() {
        super();
    }

    public GeoVolume(GeoVolume other) {
        super(other);
        this.collectionType = other.collectionType;
        this.content.addAll(other.content);
        this.additionalProperties.putAll(other.additionalProperties);
    }

    public String getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public List<Link> getContent() {
        return content;
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

    /** Updates the links for this GeoVolume. */
    void updateLinks() {
        // clean up the links we are about to add
        for (Link l : new ArrayList<>(links)) {
            String rel = l.getRel();
            if (Link.REL_SELF.equals(rel) || Link.REL_ALTERNATE.equals(rel)) {
                links.remove(l);
            }
        }
        String basePath = "ogc/3dgeovolumes/v1/collections/";
        String baseURL = APIRequestInfo.get().getBaseURL();
        for (Link link : content) {
            String updated = LinkUpdater.updateLink(baseURL, basePath, link.getHref());
            link.setHref(updated);
        }
        if (content != null) {
            for (Link content : getContent()) {
                String updated = LinkUpdater.updateLink(baseURL, basePath, content.getHref());
                content.setHref(updated);
            }
        }

        // links in dynamic content
        LinkUpdater.updateLinks(baseURL, basePath, additionalProperties);

        // dynamically generated links, self references
        addSelfLinks(basePath + getId());
    }
}
