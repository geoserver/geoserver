/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

/**
 * Bean used for JSON serialization
 *
 * @author Andrea Aime - GeoSolutions
 */
class ProductReference {

    String id;

    String href;

    String rss;

    public ProductReference(String id, String href, String search) {
        super();
        this.id = id;
        this.href = href;
        this.rss = search;
    }

    public String getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    public String getRss() {
        return rss;
    }
}
