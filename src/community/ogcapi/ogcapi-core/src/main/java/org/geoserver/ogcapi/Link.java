/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Map;
import org.springframework.http.HttpMethod;

/** Represents a JSON/XML link */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Link {

    public static final String REL_SERVICE = "service";
    public static final String REL_PREV = "prev";
    public static final String REL_SELF = "self";
    public static final String REL_NEXT = "next";
    public static final String REL_ALTERNATE = "alternate";
    public static final String REL_ABOUT = "about";
    public static final String REL_ITEM = "item";
    public static final String REL_ITEMS = "items";
    public static final String REL_DESCRIBEDBY = "describedBy";

    public static final String REL_DATA = "data";

    public static final String REL_COLLECTION = "collection";
    public static final String REL_SERVICE_DESC = "service-desc";
    public static final String REL_SERVICE_DOC = "service-doc";
    public static final String REL_CONFORMANCE = "conformance";
    /**
     * Refers to a resource that identifies the specifications that the link’s context conforms to.
     *
     * <p>This is an OGC definition, from OGC API Common Part 1: Core specification.
     */
    public static final String REL_CONFORMANCE_URI =
            "http://www.opengis.net/def/rel/ogc/1.0/conformance";

    /**
     * Refers to the root resource of a dataset in an API.
     *
     * <p>This is an OGC definition, from OGC API Common Part 1: Core specification.
     */
    public static final String REL_DATA_URI = "http://www.opengis.net/def/rel/ogc/1.0/data";

    public static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    String href;
    String rel;
    String type;
    String title;
    String classification;
    Boolean templated;
    Boolean merge;
    Map<String, Object> body;
    HttpMethod method;

    public Link() {}

    public Link(String href, String rel, String type, String title, String classification) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
        this.classification = classification;
    }

    public Link(String href, String rel, String type, String title) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JacksonXmlProperty(namespace = ATOM_NS, isAttribute = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonIgnore
    public String getClassification() {
        return classification == null ? rel : classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Boolean isTemplated() {
        return templated;
    }

    public void setTemplated(Boolean templated) {
        this.templated = templated;
    }

    /**
     * Used by STAC, requests a client to merge the original query parameters with the ones provided
     * in the link, to reduce the payload size in responses
     *
     * @return
     */
    public Boolean getMerge() {
        return merge;
    }

    public void setMerge(Boolean merge) {
        this.merge = merge;
    }

    /**
     * The request body for a POST link
     *
     * @return
     */
    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "Link{"
                + "href='"
                + href
                + '\''
                + ", rel='"
                + rel
                + '\''
                + ", type='"
                + type
                + '\''
                + ", title='"
                + title
                + '\''
                + ", classification='"
                + classification
                + '\''
                + ", templated="
                + templated
                + ", merge="
                + merge
                + ", body="
                + body
                + ", method="
                + method
                + '}';
    }
}
