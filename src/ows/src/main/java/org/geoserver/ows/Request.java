/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.BufferedReader;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;

/**
 * A collection of the informations collected and parsed by the {@link Dispatcher} while doing its
 * dispatching work. In case of dispatching exceptions some fields may be left blank, depending how
 * far the dispatching went.
 *
 * @author Justin DeOliveira
 * @author Andrea Aime
 */
public class Request {
    /** Http request / response */
    protected HttpServletRequest httpRequest;

    protected HttpServletResponse httpResponse;

    /** flag indicating if the request is get */
    protected boolean get;

    /** flag indicating if the request is a SOAP request */
    protected boolean soap;

    /** Kvp parameters, only non-null if get = true */
    protected Map kvp;

    /** raw kvp parameters, unparsed */
    protected Map rawKvp;

    /** buffered input stream, only non-null if get = false */
    protected BufferedReader input;

    /** OWS service (combined with request and version) */
    protected String service;
    /** OWS request (ie operation) combined with service and version */
    protected String request;

    /** OWS protocol version (combined with service and request) */
    protected String version;

    /**
     * xml namespace used in request body, only relevant for post requests and when request body
     * content is namespace qualified
     */
    protected String namespace;

    /** The ows service descriptor of the service/version that was actually dispatched */
    protected Service serviceDescriptor;

    /** Pre context of the url path */
    protected String context;
    /** Remaining path without context */
    protected String path;

    /** The output format of hte request */
    protected String outputFormat;

    /** Any errors that occur trying to determine the service */
    protected Throwable error;

    /** Time when the request hit the server */
    protected Date timestamp;

    /**
     * The Operation used to call the service code. Available only after dispatching is done, it
     * will give access to the current service object, and the parsed request
     */
    protected Operation operation;

    /** Uniquely identifies this request */
    protected UUID identifier;

    /** SOAP namespace used in the request */
    private String soapNamespace;

    public Request() {
        timestamp = new Date();
        identifier = UUID.randomUUID();
    }

    /**
     * Copy constructor
     *
     * @param other request to copy
     */
    public Request(Request other) {
        super();
        this.httpRequest = other.httpRequest;
        this.httpResponse = other.httpResponse;
        this.get = other.get;
        this.soap = other.soap;
        this.kvp = other.kvp;
        this.rawKvp = other.rawKvp;
        this.input = other.input;
        this.service = other.service;
        this.request = other.request;
        this.version = other.version;
        this.namespace = other.namespace;
        this.serviceDescriptor = other.serviceDescriptor;
        this.context = other.context;
        this.path = other.path;
        this.outputFormat = other.outputFormat;
        this.error = other.error;
        this.timestamp = other.timestamp;
        this.operation = other.operation;
        this.identifier = other.identifier;
    }

    /** Returns the raw http request being handled by the {@link Dispatcher} */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /** Returns the raw http response being handled by the {@link Dispatcher} */
    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    /** True if the request is a GET one */
    public boolean isGet() {
        return get;
    }

    /** True if the request is a SOAP request. */
    public boolean isSOAP() {
        return soap;
    }

    /** The parsed key value pair map */
    public Map getKvp() {
        return kvp;
    }

    /** The raw, un-parsed key value pair map */
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

    /** The service requested */
    public String getService() {
        return service;
    }

    /** The operation requested against the service */
    public String getRequest() {
        return request;
    }

    /** The service version */
    public String getVersion() {
        return version;
    }

    /** The request namespace */
    public String getNamespace() {
        return namespace;
    }

    /** The service descriptor of the service/version that was actually dispatched. */
    public Service getServiceDescriptor() {
        return serviceDescriptor;
    }

    /** The output format */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * The Operation used to call the service code. Available only after dispatching is done, it
     * provides access to the current service object, and the parsed request
     */
    public Operation getOperation() {
        return operation;
    }

    /** The eventual error thrown during request parsing, execution or output writing */
    public Throwable getError() {
        return error;
    }

    public String toString() {
        return getService() + " " + getVersion() + " " + getRequest();
    }

    /**
     * Allows callbacks to override the http request
     *
     * @param httpRequest http request override
     */
    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    /**
     * Allows call backs to override the http response
     *
     * @param httpResponse http response override
     */
    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    /**
     * Allows call backs to change the GET status
     *
     * @param get true for iHTTP GET Request
     */
    public void setGet(boolean get) {
        this.get = get;
    }

    /**
     * Flags/unflags the request as a SOAP request.
     *
     * @param soap true for SOAP request
     */
    public void setSOAP(boolean soap) {
        this.soap = soap;
    }

    /**
     * Allows callbacks to change the parsed KVP map
     *
     * <p>Clients should consider calling {@link #setOrAppendKvp(java.util.Map)} to retain the
     * existing kvp map.
     *
     * @param kvp Parsed kvp values.
     */
    public void setKvp(Map kvp) {
        this.kvp = kvp;
    }

    /**
     * Sets the parsed kvp map, appending/overwriting to any previously set values.
     *
     * @param kvp Parsed kvp values.
     */
    public void setOrAppendKvp(Map kvp) {
        if (this.kvp == null) {
            setKvp(kvp);
        } else {
            this.kvp.putAll(kvp);
        }
    }

    /**
     * Allows callbacks to override the parsed kvp map
     *
     * @param rawKvp key value pair map override
     */
    public void setRawKvp(Map rawKvp) {
        this.rawKvp = rawKvp;
    }

    /**
     * Allows callbacks to override/wrap the input reader
     *
     * @param input input reader override
     */
    public void setInput(BufferedReader input) {
        this.input = input;
    }

    /**
     * Allows call backs to override the service
     *
     * @param service OWS service
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Allows call backs to override the requested operation
     *
     * @param request OWS Request (ie operation)
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Allows callbacks to override the version
     *
     * @param version OWS version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /** Sets the request namespace */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Allows callbacks to override the service descriptor (id, name and version).
     *
     * @param serviceDescriptor service descriptor
     */
    public void setServiceDescriptor(Service serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    /**
     * Allows call backs to override the output format
     *
     * @param outputFormat Output format override
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Sets the operation in use
     *
     * @param operation OWS Operation
     */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /**
     * The context of the url path of the request.
     *
     * <p>The context is anything before the part that matches an ows service. For instance in:
     *
     * <pre>
     *   /foo/bar/wfs?...
     * </pre>
     *
     * <p>The context would be "/foo/bar".
     */
    public String getContext() {
        return context;
    }

    /**
     * Sets the context.
     *
     * @see #getContext()
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * The remainder part of the url path after the context.
     *
     * <p>In the following:
     *
     * <pre>
     *   /foo/bar/wfs?...
     * </pre>
     *
     * The path would be "/wfs".
     *
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
     *
     * @param error Throwable indication operation failure
     */
    public void setError(Throwable error) {
        this.error = error;
    }

    /** The timestamp when the request hit the server */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the request timestamp
     *
     * @param timestamp request timestamp (represented as a Date)
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Request other = (Request) obj;
        if (identifier == null) {
            if (other.identifier != null) return false;
        } else if (!identifier.equals(other.identifier)) return false;
        return true;
    }

    /** Sets the SOAP namespace used in the request */
    public void setSOAPNamespace(String soapNamespace) {
        this.soapNamespace = soapNamespace;
    }

    /** Returns the SOAP namespace used in the request, or null if the request was not a SOAP one */
    public String getSOAPNamespace() {
        return soapNamespace;
    }
}
