/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

public class GeoPkgSymbol {

    long id;
    String symbol;
    String description;
    String uri;

    public GeoPkgSymbol(String symbol, String description, String uri) {
        this.symbol = symbol;
        this.description = description;
        this.uri = uri;
    }

    public GeoPkgSymbol(long id, String symbol, String description, String uri) {
        this.id = id;
        this.symbol = symbol;
        this.description = description;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
}
