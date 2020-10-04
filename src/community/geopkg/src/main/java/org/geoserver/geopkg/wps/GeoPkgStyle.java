/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

public class GeoPkgStyle {

    long id;
    String style;
    String description;
    String uri;

    public GeoPkgStyle(String style, String uri) {
        this.style = style;
        this.uri = uri;
    }

    public GeoPkgStyle(long id, String style, String uri) {
        this.id = id;
        this.style = style;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
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
        return "GeoPkgStyle{"
                + "id="
                + id
                + ", style='"
                + style
                + '\''
                + ", description='"
                + description
                + '\''
                + ", uri='"
                + uri
                + '\''
                + '}';
    }

    void setId(long id) {
        this.id = id;
    }
}
