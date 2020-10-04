/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

/**
 * Represents a semantic annotation, a "semantically grounded term that can be applied to another
 * concept", typically linked to either a full table or a record in a table.
 */
public class GeoPkgSemanticAnnotation {

    long id;
    String type;
    String title;
    String description;
    String uri;

    public GeoPkgSemanticAnnotation(
            long id, String type, String title, String description, String uri) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.uri = uri;
    }

    public GeoPkgSemanticAnnotation(String type, String title, String uri) {
        this.type = type;
        this.title = title;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "GeoPkgSemanticAnnotation{"
                + "id="
                + id
                + ", type='"
                + type
                + '\''
                + ", title='"
                + title
                + '\''
                + ", description='"
                + description
                + '\''
                + ", uri='"
                + uri
                + '\''
                + '}';
    }
}
