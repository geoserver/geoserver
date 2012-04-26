/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.service;

import java.util.Map;

public abstract class BaseRequest {

    private static final String SERVICE = "GSS";

    private String baseUrl;

    private String version;

    private String handle;

    private String request;

    private Map<String, String> rawKvp;

    public BaseRequest(final String request) {
        version = "1.0.0";
        setRequest(request);
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequest() {
        return this.request;
    }

    public String getService() {
        return SERVICE;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Gets the raw kvp parameters which were used to create the request.
     */
    public Map<String, String> getRawKvp() {
        return rawKvp;
    }

    public void setRawKvp(Map<String, String> rawKvp) {
        this.rawKvp = rawKvp;
    }

}
