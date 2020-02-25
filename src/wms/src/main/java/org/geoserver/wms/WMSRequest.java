/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Map;
import org.geoserver.ows.Dispatcher;

/**
 * Defines a general Request type and provides accessor methods for universal request information.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Roldan
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @version $Id$
 */
public abstract class WMSRequest {

    private String baseUrl;

    private Map<String, String> rawKvp;

    /** flag indicating if the request is get */
    protected boolean get;

    protected String request;

    protected String version;

    private String requestCharset;

    /**
     * Creates the new request with the given operation name
     *
     * @param request name of the request, (Example, GetCapabiliites)
     */
    protected WMSRequest(final String request) {
        setRequest(request);
    }

    /**
     * Tells whether the originating request used HTTP GET method or not; may be useful, for
     * example, to determine if client can do HTTP caching and then set the corresponding response
     * headers.
     *
     * @return {@code true} if the originating HTTP request used HTTP GET method, {@code false}
     *     otherwise
     */
    public boolean isGet() {
        return get;
    }

    public void setGet(boolean get) {
        this.get = get;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRawKvp(Map<String, String> rawKvp) {
        this.rawKvp = rawKvp;
    }

    /** Set by {@link Dispatcher} */
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    /** Gets the raw kvp parameters which were used to create the request. */
    public Map<String, String> getRawKvp() {
        return rawKvp;
    }

    /** Setter for the 'WMTVER' parameter, which is an alias for 'VERSION'. */
    public void setWmtVer(String version) {
        setVersion(version);
    }

    /** @return the HTTP request charset, may be {@code null} */
    public String getRequestCharset() {
        return requestCharset;
    }

    public void setRequestCharset(String requestCharset) {
        this.requestCharset = requestCharset;
    }
}
