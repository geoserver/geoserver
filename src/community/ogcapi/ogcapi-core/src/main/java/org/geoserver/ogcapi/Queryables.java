/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;

/*
 * A Queryables document is a schema, with a couple of additional properties
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Queryables extends Schema<Object> {

    public static final String REL = "http://www.opengis.net/def/rel/ogc/1.0/queryables";

    private final String schema = "https://json-schema.org/draft/2019-09/schema";

    private String id;

    private String collectionId;

    private AbstractDocument linksHolder = new AbstractDocument();

    public Queryables(String id) {
        this.id = id;
    }

    @JsonProperty("$schema")
    public String getSchema() {
        return schema;
    }

    @JsonProperty("$id")
    public String getId() {
        return id;
    }

    @JsonIgnore
    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public void addLink(Link link) {
        linksHolder.addLink(link);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    public List<Link> getLinks() {
        return linksHolder.getLinks();
    }

    public String getLinkUrl(String classification, String type) {
        return linksHolder.getLinkUrl(classification, type);
    }

    public List<Link> getLinksFor(String classification) {
        return linksHolder.getLinksFor(classification);
    }

    public List<Link> getLinksExcept(String classification, String excludedType) {
        return linksHolder.getLinksExcept(classification, excludedType);
    }

    public void addSelfLinks(String path) {
        linksHolder.addSelfLinks(path);
    }
}
