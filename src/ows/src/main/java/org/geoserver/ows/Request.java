/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.BufferedReader;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.Service;

/**
 * A collection of the informations collected and parsed by the
 * {@link Dispatcher} while doing its dispatching work. In case of dispatching
 * exceptions some fields may be left blank, depending how far the dispatching
 * went.
 * 
 * @author Justin DeOliveira
 * @author Andrea Aime
 */
public class Request {
    /**
     * Http request / response
     */
    protected HttpServletRequest httpRequest;

    protected HttpServletResponse httpResponse;

    /**
     * flag indicating if the request is get
     */
    protected boolean get;

    /**
     * flag indicating if the request is a SOAP request
     */
    protected boolean soap;

    /**
     * Kvp parameters, only non-null if get = true
     */
    protected Map kvp;

    /**
     * raw kvp parameters, unparsed
     */
    protected Map rawKvp;

    /**
     * buffered input stream, only non-null if get = false
     */
    protected BufferedReader input;

    /**
     * The ows service,request,version
     */
    protected String service;

    protected String request;

    protected String version;

    /**
     * xml namespace used in request body, only relevant for post requests and when request body 
     * content is namespace qualified
     */
    protected String namespace;

    /**
     * The ows service descriptor of the service/version that was actually dispatched  
     */
    protected Service serviceDescriptor;

    
    /**
     * Pre context of the url path
     */
    protected String context;
    /**
     * Remaining path without context
     */
    protected String path; 
    
    /**
     * The output format of hte request
     */
    protected String outputFormat;

    /**
     * Any errors that occur trying to determine the service
     */
    protected Throwable error;
    
    /**
     * Time when the request hit the server
     */
    protected Date timestamp;
    
    public Request() {
        timestamp = new Date(); 
    }

    /**
     * Returns the raw http request being handled by the {@link Dispatcher}
     * @return
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Returns the raw http response being handled by the {@link Dispatcher}
     * @return
     */
    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    /**
     * True if the request is a GET one
     * @return
     */
    public boolean isGet() {
        return get;
    }

    /**
     * True if the request is a SOAP request.
     */
    public boolean isSOAP() {
        return soap;
    }

    /**
     * The parsed key value pair map
     */
    public Map getKvp() {
        return kvp;
    }

    /**
     * The raw, un-parsed key value pair map
     */
    public Map getRawKvp() {
        return rawKvp;
    }

    /**
     * The input as read from the http request. The {@link Dispatcher} will perform some preventive
     * reading on the input so never use the raw {@link HttpServletRequest} one
     */
    public BufferedReader getInput() {
        return input;
    }

    /**
     * The service requested 
     * @return
     */
    public String getService() {
        return service;
    }

    /**
     * The operation requested against the service
     * @return
     */
    public String getRequest() {
        return request;
    }

    /**
     * The service version
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * The request namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The service descriptor of the service/version that was actually dispatched.
     */
    public Service getServiceDescriptor() {
        return serviceDescriptor;
    }
    
    /**
     * The output format
     * @return
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * The eventual error thrown during request parsing, execution or output writing
     * @return
     */
    public Throwable getError() {
        return error;
    }

    public String toString() {
        return getService() + " " + getVersion() + " " + getRequest();
    }

    /**
     * Allows callbacks to override the http request
     * @param httpRequest
     */
    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    /**
     * Allows callbacks to override the http response
     * @param httpRequest
     */
    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    /**
     * Allows callbacks to change the GET status
     * @param httpRequest
     */
    public void setGet(boolean get) {
        this.get = get;
    }

    /**
     * Flags/unflags the request as a SOAP request.
     */
    public void setSOAP(boolean soap) {
        this.soap = soap;
    }

    /**
     * Allows callbacks to change the parsed KVP map
     * @param kvp
     */
    public void setKvp(Map kvp) {
        this.kvp = kvp;
    }

    /**
     * Allows callbacks to override the parsed kvp map
     * @param rawKvp
     */
    public void setRawKvp(Map rawKvp) {
        this.rawKvp = rawKvp;
    }

    /**
     * Allows callbacks to override/wrap the input reader
     * @param input
     */
    public void setInput(BufferedReader input) {
        this.input = input;
    }
    
    /**
     * Allows callbacks to override the service
     * @param service
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Allows callbacks to override the requested operation
     * @param service
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Allows callbacks to override the version
     * @param service
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Sets the request namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Allows callbacks to override the service descriptor
     * @param serviceDescriptor
     */
    public void setServiceDescriptor(Service serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    /**
     * Allows callbacks to override the output format
     * @param service
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * The context of the url path of the request. 
     * <p>
     * The context is anything before the part that matches an ows service. For instance in: 
     * <pre>
     *   /foo/bar/wfs?...
     * </pre>
     * The context would be "/foo/bar".
     * </p>
     */
    public String getContext() {
        return context;
    }
    
    /**
     * Sets the context.
     * 
     * @set {@link #getContext()}
     */
    public void setContext(String context) {
        this.context = context;
    }
    
    /**
     * The remainder part of the url path after the context.
     * <p>
     * In the following: 
     * <pre>
     *   /foo/bar/wfs?...
     * </pre>
     * The path would be "/wfs".
     * </p>
     * @see #getContext()
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Sets the patch.
     * 
     * @see #getPath()
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Allows callbacks to override the operation execution error
     * @param service
     */
    public void setError(Throwable error) {
        this.error = error;
    }
    
    /**
     * The timestamp when the request hit the server
     * @return
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the request timestamp
     * @param timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
