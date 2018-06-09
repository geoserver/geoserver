/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.util.List;

/**
 * List of OGC links, exists only to serve JSON encoding
 *
 * @author Andrea Aime - GeoSolutions
 */
class OgcLinks {

    List<OgcLink> links;

    public OgcLinks() {
        super();
    }

    public OgcLinks(List<OgcLink> links) {
        this.links = links;
    }

    public List<OgcLink> getLinks() {
        return links;
    }

    public void setLinks(List<OgcLink> links) {
        this.links = links;
    }
}
