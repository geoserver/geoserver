/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.ServiceInfo;
import org.vfny.geoserver.util.Requests;

/**
 * Defines a general Request type and provides accessor methods for universal request information.
 *
 * <p>Also provides access to the HttpRequest that spawned this GeoServer Request. This HttpRequest
 * is most often used to lookup information stored in the Web Container (such as the GeoServer
 * Global information).
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Roldan
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @version $Id$
 * @deprecated implement {@link org.geoserver.ows.Request} instead
 */
public abstract class Request {
    /** HttpServletRequest responsible for generating this GeoServer Request. */
    protected HttpServletRequest httpServletRequest;

    /**
     * The service type of the request. In other words, is it a WMS or a WFS. This is a standard
     * element of a request. It now has a practical purpose in GeoServer, as a GetCapabilities
     * request can be WMS or WFS, this element tells which it is.
     */
    protected String service;

    /** Request type */
    protected String request = "";

    /** Request version */
    protected String version = "";

    /** service reference */
    // protected AbstractService serviceRef;
    protected ServiceInfo serviceConfig;

    /**
     * reference to the base Url that this request was called with. Note that this is a complete
     * duplicate of info in the above HttpServletRequest object, and is mainly a forward-thinking
     * field that's going to stick around when the above HttpServletRequest goes away.
     */
    protected String baseUrl;

    /**
     * ServiceType,RequestType,ServiceRef constructor.
     *
     * @param serviceType Name of hte service (example, WFS)
     * @param requestType Name of the request (example, GetCapabilties)
     * @param serviceRef The servlet for the request.
     */
    protected Request(String service, String request, ServiceInfo serviceConfig) {
        this.service = service;
        this.request = request;
        this.serviceConfig = serviceConfig;
    }

    /** Set the baseUrl that this request was called with. */
    public void setBaseUrl(String s) {
        baseUrl = s;
    }

    /**
     * Gets the base url that made this request. This is used to return the referenced schemas and
     * whatnot relative to the request.
     *
     * @return The base portion of the url that the client used to make the request.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets requested service.
     *
     * @return The requested service.
     */
    public String getService() {
        return this.service;
    }

    /**
     * Gets requested service.
     *
     * @param service The requested service.
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Gets requested request type.
     *
     * <p>TODO: Could this bre renamed getType() for clarity?
     *
     * <p>Um, no. getType() is less clear. getRequest makes sense because this is directly modeled
     * off of the XML and KVP Requests that a wfs or wms makes, and they all contain an element
     * called Request.
     *
     * @return The name of the request.
     */
    public String getRequest() {
        return this.request;
    }

    /**
     * Sets requested request type.
     *
     * @param reqeust The type of request.
     */
    public void setRequest(String requestType) {
        this.request = requestType;
    }

    /**
     * Return version type.
     *
     * @return The request type version.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Sets version type.
     *
     * @param version The request type version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /** @return The service configuration. */
    public ServiceInfo getServiceConfig() {
        return serviceConfig;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Request)) {
            return false;
        }

        Request req = (Request) o;
        boolean equals = true;
        equals =
                ((request == null) ? (req.getRequest() == null) : request.equals(req.getRequest()))
                        && equals;
        equals =
                ((version == null) ? (req.getVersion() == null) : version.equals(req.getVersion()))
                        && equals;
        equals =
                ((service == null) ? (req.getService() == null) : service.equals(req.getService()))
                        && equals;

        return equals;
    }

    /** Generate a hashCode based on this Request Object. */
    public int hashCode() {
        int result = 17;
        result = (23 * result) + ((request == null) ? 0 : request.hashCode());
        result = (23 * result) + ((request == null) ? 0 : version.hashCode());
        result = (23 * result) + ((request == null) ? 0 : service.hashCode());

        return result;
    }

    /**
     * Retrive the ServletRequest that generated this GeoServer request.
     *
     * <p>The ServletRequest is often used to:
     *
     * <ul>
     *   <li>Access the Sesssion and WebContainer by execute opperations
     *   <li>Of special importance is the use of the ServletRequest to locate the GeoServer
     *       Application
     *       <p>This method is called by AbstractServlet during the processing of a Request.
     *
     * @return The HttpServletRequest responsible for generating this SerivceRequest
     */
    public HttpServletRequest getHttpServletRequest() throws ClassCastException {
        return httpServletRequest;
    }

    public String getRootDir() {
        throw new IllegalArgumentException(
                "getRootDir -- functionality removed - please verify that its okay with geoserver_data_dir");

        // return httpServletRequest.getSession().getServletContext().getRealPath("/");
    }

    /**
     * Tests if user is Logged into GeoServer.
     *
     * @return <code>true</code> if user is logged in
     */
    public boolean isLoggedIn() {
        return Requests.isLoggedIn(getHttpServletRequest());
    }

    /**
     * Sets the servletRequest that generated this GeoServer request.
     *
     * <p>The ServletRequest is often used to:
     *
     * <ul>
     *   <li>Access the Sesssion and WebContainer by execute opperations
     *   <li>Of special importance is the use of the ServletRequest to locate the GeoServer
     *       Application
     *
     * @param servletRequest The servletRequest to set.
     */
    public void setHttpServletRequest(HttpServletRequest servletRequest) {
        httpServletRequest = servletRequest;
    }
}
