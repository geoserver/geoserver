/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

/**
 * Bean representation of a OWS context OGC Link
 *
 * @author Andrea Aime - GeoSolutions
 */
class OgcLink {
    String offering;

    String method;

    String code;

    String type;

    String href;

    public OgcLink(String offering, String method, String code, String type, String href) {
        this.offering = offering;
        this.method = method;
        this.code = code;
        this.type = type;
        this.href = href;
    }

}
