/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import org.geotools.filter.visitor.DefaultFilterVisitor;

public class JsonLdVisitor extends DefaultFilterVisitor {

    private String jsonLdPath;

    public JsonLdVisitor(String jsonLdPath) {
        this.jsonLdPath = jsonLdPath;
    }

    public String getJsonLdPath() {
        return jsonLdPath;
    }

    public void setJsonLdPath(String jsonLdPath) {
        this.jsonLdPath = jsonLdPath;
    }
}
