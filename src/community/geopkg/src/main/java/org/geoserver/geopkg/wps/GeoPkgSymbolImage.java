/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

public class GeoPkgSymbolImage {
    long id;
    String format;
    byte[] content;
    String uri;
    GeoPkgSymbol symbol;

    public GeoPkgSymbolImage(String format, byte[] content, String uri, GeoPkgSymbol symbol) {
        this.format = format;
        this.content = content;
        this.uri = uri;
        this.symbol = symbol;
    }

    public GeoPkgSymbolImage(
            long id, String format, byte[] content, String uri, GeoPkgSymbol symbol) {
        this.id = id;
        this.format = format;
        this.content = content;
        this.uri = uri;
        this.symbol = symbol;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public GeoPkgSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(GeoPkgSymbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "GeoPkgSymbolImage{"
                + "id="
                + id
                + ", format='"
                + format
                + '\''
                + ", uri='"
                + uri
                + '\''
                + ", symbol="
                + symbol
                + '}';
    }
}
