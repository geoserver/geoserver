/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.geoserver.platform.ServiceException;
import org.opengis.geometry.BoundingBox;


/**
 * The request object, a simple java bean that gathers all the information and data that is 
 * monitored per request.
 * 
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
public class RequestData implements Serializable {

    private static final long serialVersionUID = 4115701065212157258L;

    private static AtomicLong COUNTER = new AtomicLong();
    
    /**
     * Enumeration describing the status of a request.
     */
    public static enum Status {
        WAITING, RUNNING, CANCELLING, FAILED, FINISHED, CANCELLED, INTERRUPTED
    };

    /**
     * Enumeration describing the category of a request.
     */
    public static enum Category {
        OWS, REST, OTHER
    };
    
    public long internalid = COUNTER.getAndIncrement();
    
    /**
     * request id
     */
    private long id = -1;

    /**
     * Request status / state
     */
    private Status status = Status.WAITING;
    
    /**
     * Request category
     */
    private Category category = Category.OTHER;
    
    /**
     * The path of the request URL.
     */
    private String path;
    
    /**
     * The query string that is contained in the request URL after the path, or {@code null} if the
     * URL does not have a query string.
     */
    private String queryString;

    /**
     * The body of the request in the case of a PUT or POST
     */
    private byte[] body;
    
    /**
     * The length of the request body in teh case of a PUT or POST
     */
    private long bodyContentLength;
    
    /**
     * The mime type of the request body
     */
    private String bodyContentType;
    
    /**
     * The HTTP method of the request
     */
    private String httpMethod;

    /**
     * The request start timestamp in the Server's local time (as per
     * {@link System#currentTimeMillis()})
     */
    private Date startTime;

    /**
     * The request end timestamp in the Server's local time (as per
     * {@link System#currentTimeMillis()})
     */
    private Date endTime;

    /**
     * The total time, in milliseconds, the request took to complete
     */
    private long totalTime;
    
    /**
     * The Internet Protocol (IP) address of the client or last proxy that sent the request.
     */
    private String remoteAddr;

    /**
     * The fully qualified name of the client or the last proxy that sent the request. If the engine
     * cannot or chooses not to resolve the hostname (to improve performance), the the dotted-string
     * form of the IP address.
     */
    private String remoteHost;
    
    /**
     * Username (if available) specified with the request
     */
    private String remoteUser;

    /**
     * Remote user agent (user-agent header from request)
     */
    private String remoteUserAgent;

    /**
     * Country request originated from (if available), obtained via geoip lookup.
     */
    private String remoteCountry;
    
    /**
     * City request originated from (if available), obtained via geoip lookup
     */
    private String remoteCity;
    
    /**
     * Latitude request originated from (if available), obtained via geoip lookup
     */
    private double remoteLat;
    
    /**
     * Longitude request originated from (if available), obtained via geoip lookup
     */
    private double remoteLon;
    
    /**
     * The server host (useful in case we are dealing with a cluster of GeoServer instances)
     */
    private String host;

    /**
     * The internal server host (to the internal network)
     */
    private String internalHost;
    
    /**
     * The service name, in the case of ows this is WMS, WFS, WCS, WPS, etc...
     */
    private String service;

    /**
     * The operation name, such as GetMap, GetFeature, etc...
     */
    private String operation;

    /**
     * The OWS service version, specific to ows requests
     */
    private String owsVersion;

    /**
     * The sub operation, example for WFS transaction being INSERT, UPDATE, etc... 
     */
    private String subOperation;
    
    /**
     * The requested resources
     */
    private List<String> resources = new ArrayList<String>(1);

    /**
     * The HTTP response length, in bytes
     */
    private long responseLength;

    /**
     * The response content MIME type, might be {@code null}
     */
    private String responseContentType;

    /**
     * The {@link ServiceException} message, or {@code null}
     */
    private String errorMessage;

    /**
     * The exception that occurred while processing the request, if any.
     */
    private Throwable error;
    
    /**
     * The response status
     */
    Integer responseStatus;
    
    /**
     *  The Referer of the HTTP request, if any
     */
    private String httpReferer;
    
    /**
     * A bounding box for the region the request covers if any (May be approximate)
     */
    private BoundingBox bbox;

    /**
     * Whether the request hit or miss the cache for cache-able protocols (GWC, direct integration)
     */
    private String cacheResult;

    /**
     * If there was a cache miss, the reason for it
     */
    private String missReason;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * The body of the HTTP request
     * 
     * May be trimmed to a maximum length.  If so, check getBodyContentLength for the length of the
     * untrimmed body.
     */
    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public long getBodyContentLength() {
        return bodyContentLength;
    }
    
    public void setBodyContentLength(long bodyContentLength) {
        this.bodyContentLength = bodyContentLength;
    }
    
    public String getBodyContentType() {
        return bodyContentType;
    }
    
    public void setBodyContentType(String bodyContentType) {
        this.bodyContentType = bodyContentType;
    }
    
    public String getBodyAsString() {
        if(body != null) {
            return new String(body);
        } else {
            return null;
        }
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    
    public String getInternalHost() {
        return internalHost;
    }

    public void setInternalHost(String internalHost) {
        this.internalHost = internalHost;
    }
    
    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteUserAgent() {
        return remoteUserAgent;
    }

    public void setRemoteUserAgent(String remoteUserAgent) {
        this.remoteUserAgent = remoteUserAgent;
    }

    public String getRemoteCountry() {
        return remoteCountry;
    }
    
    public void setRemoteCountry(String remoteCountry) {
        this.remoteCountry = remoteCountry;
    }
    
    public String getRemoteCity() {
        return remoteCity;
    }
    
    public void setRemoteCity(String remoteCity) {
        this.remoteCity = remoteCity;
    }
    
    public double getRemoteLat() {
        return remoteLat;
    }
    
    public void setRemoteLat(double remoteLat) {
        this.remoteLat = remoteLat;
    }
    
    public double getRemoteLon() {
        return remoteLon;
    }
    
    public void setRemoteLon(double remoteLon) {
        this.remoteLon = remoteLon;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }

    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getOwsVersion() {
        return owsVersion;
    }

    public void setOwsVersion(String owsVersion) {
        this.owsVersion = owsVersion;
    }

    public String getSubOperation() {
        return subOperation;
    }
    
    public void setSubOperation(String subOperation) {
        this.subOperation = subOperation;
    }
    
    public List<String> getResources() {
        return resources;
    }
    
    public String getResourcesList() {
        if(resources != null && resources.size() > 0) {
            String result = resources.toString();
            return result.substring(1, result.length() - 1); 
        } else {
            return null;
        }
    }
    
    public void setResources(List<String> resources) {
        this.resources = resources;
    }
    
    public long getResponseLength() {
        return responseLength;
    }

    public void setResponseLength(long responseLength) {
        this.responseLength = responseLength;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }
    
    public RequestData clone() {
        RequestData clone = new RequestData();
        clone.setId(id);
        clone.setStatus(status);
        clone.setPath(path);
        clone.setQueryString(queryString);
        clone.setBody(body);
        clone.setHttpMethod(httpMethod);
        clone.setStartTime(startTime);
        clone.setEndTime(endTime);
        clone.setTotalTime(totalTime);
        clone.setRemoteAddr(remoteAddr);
        clone.setRemoteHost(remoteHost);
        clone.setHost(host);
        clone.setInternalHost(internalHost);
        clone.setRemoteUser(remoteUser);
        clone.setService(service);
        clone.setOperation(operation);
        clone.setSubOperation(subOperation);
        clone.setOwsVersion(owsVersion);
        clone.setResources(new ArrayList(resources));
        clone.setResponseLength(responseLength);
        clone.setResponseContentType(responseContentType);
        clone.setErrorMessage(errorMessage);
        clone.setError(error);
        clone.setResponseStatus(responseStatus);
        clone.setHttpReferer(httpReferer);
        clone.setBbox(bbox);
        clone.setCacheResult(cacheResult);
        clone.setMissReason(missReason);
     
        return clone;
    }
    
    @Override
    public String toString() {
        return "Request (" + String.valueOf(id) + ")";
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer httpStatus) {
        this.responseStatus = httpStatus;
    }
    
    public String getHttpReferer() {
        return httpReferer;
    }
    
    public void setHttpReferer(String httpReferer){
        this.httpReferer = httpReferer;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public String getCacheResult() {
        return cacheResult;
    }

    public void setCacheResult(String cacheResult) {
        this.cacheResult = cacheResult;
    }

    public String getMissReason() {
        return missReason;
    }

    public void setMissReason(String missReason) {
        this.missReason = missReason;
    }
}
