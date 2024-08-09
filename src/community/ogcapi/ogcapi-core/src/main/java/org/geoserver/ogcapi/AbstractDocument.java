/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.MediaType;

/** Base OGC API document class with shared link generation facilities */
public class AbstractDocument {

    protected String id;
    protected String htmlTitle;
    protected final List<Link> links = new ArrayList<>();

    protected AbstractDocument() {}

    /** Copy constructor */
    public AbstractDocument(AbstractDocument other) {
        this.id = other.id;
        this.htmlTitle = other.htmlTitle;
        this.links.addAll(other.links.stream().map(Link::new).collect(Collectors.toList()));
    }

    /** Adds a link to the document */
    public void addLink(Link link) {
        links.add(link);
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY) // do not list links if empty
    public List<Link> getLinks() {
        return links;
    }

    /** Returns a link URL with the given classification and type, or null if not found */
    public String getLinkUrl(String classification, String type) {
        return links.stream()
                .filter(l -> Objects.equals(classification, l.getClassification()))
                .filter(l -> type.equals(l.getType()))
                .map(l -> l.getHref())
                .findFirst()
                .orElse(null);
    }

    public List<Link> getLinksFor(String classification) {
        return links.stream()
                .filter(l -> Objects.equals(classification, l.getClassification()))
                .collect(Collectors.toList());
    }

    /** Returns all links except the ones matching both classification and type provided */
    public List<Link> getLinksExcept(String classification, String excludedType) {
        return links.stream()
                .filter(
                        l ->
                                classification == null
                                        || Objects.equals(classification, l.getClassification()))
                .filter(l -> excludedType == null || !excludedType.equals(l.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Same as {@link #addSelfLinks(String, MediaType)} using {@link MediaType#APPLICATION_JSON} as
     * the default media type
     */
    protected final void addSelfLinks(String path) {
        addSelfLinks(path, MediaType.APPLICATION_JSON);
    }

    /**
     * Builds the links back to this document, in its various formats
     *
     * @param path The backlink path
     * @param defaultFormat The default format (will be used to create a "self" link instead of
     *     "alternate"
     */
    protected final void addSelfLinks(String path, MediaType defaultFormat) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        new LinksBuilder(getClass(), path)
                .rel(Link.REL_ALTERNATE)
                .title("This document as ")
                .updater(
                        (mt, l) -> {
                            if (requestInfo.isFormatRequested(mt, defaultFormat)) {
                                l.setRel(Link.REL_SELF);
                                l.setClassification(Link.REL_SELF);
                                l.setTitle("This document");
                            }
                        })
                .add(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /** Returns the document id, as is */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getId() {
        return id;
    }

    /** Returns a URL encoded id (or null if the id is missing), useful to encode links in HTML */
    @JsonIgnore
    public String getEncodedId() {
        if (id == null) {
            return null;
        }
        return ResponseUtils.urlEncode(id);
    }

    /**
     * Returns the id where the column is replaced by a double underscore. Mostly used to generate
     * HTML ids for testing purposes
     */
    @JsonIgnore
    public String getHtmlId() {
        if (id == null) {
            return null;
        }
        return id.replace(":", "__");
    }

    /**
     * Returns the title for HTML pages. If not set, uses the id, if also missing, an empty string
     */
    @JsonIgnore
    public String getHtmlTitle() {
        return htmlTitle != null ? htmlTitle : id != null ? id : "";
    }

    public void setHtmlTitle(String htmlTitle) {
        this.htmlTitle = htmlTitle;
    }
}
