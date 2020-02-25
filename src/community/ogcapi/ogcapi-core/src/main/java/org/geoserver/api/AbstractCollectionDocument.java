/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class AbstractCollectionDocument<T> extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(AbstractCollectionDocument.class);

    protected String title;
    protected String description;
    protected CollectionExtents extent;
    protected T subject;
    protected List<StyleDocument> styles = new ArrayList<>();

    /**
     * Builds an abstract collection around the provided subject. Call with null if the collection
     * happens to have none.
     */
    public AbstractCollectionDocument(T subject) {
        this.subject = subject;
    }

    /**
     * Returns the subject around which the collection is build, might be a {@link
     * org.geoserver.catalog.ResourceInfo}, a tiled layer, or anything else. Meant to be used by
     * {@link DocumentCallback} to decide if acting on a collection, or not
     */
    @JsonIgnore
    public T getSubject() {
        return subject;
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

    public CollectionExtents getExtent() {
        return extent;
    }

    public void setExtent(CollectionExtents extent) {
        this.extent = extent;
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<StyleDocument> getStyles() {
        return styles;
    }
}
