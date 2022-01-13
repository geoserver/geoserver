/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

/** This class is used for passing custom OfferingDetail object to freemarker templates. */
public class OfferingDetail {

    String method;
    String code;
    String type;
    String href;

    public OfferingDetail(String method, String code, String type, String href) {
        this.method = method;
        this.code = code;
        this.type = type;
        this.href = href;
    }

    public String getMethod() {
        return method;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getHref() {
        return href;
    }
}
