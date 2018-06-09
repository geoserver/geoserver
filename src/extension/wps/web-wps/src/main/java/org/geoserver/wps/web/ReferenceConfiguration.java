/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;

/**
 * The GUI configuration for reference data
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class ReferenceConfiguration implements Serializable {
    enum Method {
        GET,
        POST
    };

    Method method = Method.GET;

    String url;

    String body;

    String mime;

    public Method getMethod() {
        return method;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
