/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

public class GeoPkgStyleSheet {

    long id;
    GeoPkgStyle style;
    String format;
    String stylesheet;

    public GeoPkgStyleSheet(GeoPkgStyle style, String format, String stylesheet) {
        this.style = style;
        this.format = format;
        this.stylesheet = stylesheet;
    }

    public GeoPkgStyleSheet(long id, GeoPkgStyle style, String format, String stylesheet) {
        this.id = id;
        this.style = style;
        this.format = format;
        this.stylesheet = stylesheet;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GeoPkgStyle getStyle() {
        return style;
    }

    public void setStyle(GeoPkgStyle style) {
        this.style = style;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    @Override
    public String toString() {
        return "GeoPkgStyleSheet{"
                + "id="
                + id
                + ", style="
                + style
                + ", format='"
                + format
                + '\''
                + '}';
    }
}
