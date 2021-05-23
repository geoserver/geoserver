/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

/** Simplified JSON link representation. Look for ogcapi-core version for a fuller representation */
public class Link {

    String href;
    String title;
    String type;

    public Link(String href) {
        this.href = href;
    }

    public Link(String href, String title, String type) {
        this.href = href;
        this.title = title;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }
}
