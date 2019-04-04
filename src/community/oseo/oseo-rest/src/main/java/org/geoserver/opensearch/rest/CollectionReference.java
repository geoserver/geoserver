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
class CollectionReference {

    String name;

    String href;

    String search;

    public CollectionReference(String name, String href, String search) {
        super();
        this.name = name;
        this.href = href;
        this.search = search;
    }

    public String getName() {
        return name;
    }

    public String getHref() {
        return href;
    }

    public String getSearch() {
        return search;
    }
}
