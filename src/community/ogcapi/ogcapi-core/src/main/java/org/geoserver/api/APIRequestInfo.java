/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An object which contains information about the current API request. If the current request is a
 * API one, it can be retrieved using the {@link APIRequestInfo#get()} method
 */
public class APIRequestInfo {

    List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);

    /** key to reference this object by */
    public static final String KEY = "APIRequestInfo";

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    String baseURL;
    List<MediaType> requestedMediaTypes;
    APIDispatcher dispatcher;

    private Map<String, String[]> queryMap;

    /**
     * Constructs a {@link APIRequestInfo} object, generating content based on the passed request.
     */
    public APIRequestInfo(
            HttpServletRequest request, HttpServletResponse response, APIDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.request = request;
        this.response = response;

        // http://host:port/appName
        baseURL = ResponseUtils.baseURL(request);
    }

    /** Gets the base URL of the server, e.g. "http://localhost:8080/geoserver" */
    public String getBaseURL() {
        return baseURL;
    }

    String buildURI(String base, String path) {
        if (path != null) {
            if (path.startsWith(".")) {
                if (base.endsWith("/")) base = base.substring(1);
                path = base + path;
            } else {
                path = ResponseUtils.appendPath(base, path);
            }
        }

        return ResponseUtils.buildURL(baseURL, path, null, URLMangler.URLType.SERVICE);
    }

    /** Returns the APIRequestInfo from the current {@link RequestContextHolder} */
    public static APIRequestInfo get() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) return null;
        return (APIRequestInfo)
                requestAttributes.getAttribute(APIRequestInfo.KEY, RequestAttributes.SCOPE_REQUEST);
    }

    public Map<String, String[]> getQueryMap() {
        return queryMap;
    }

    /** Sets the provided APIRequestInfo into the {@link RequestContextHolder} */
    static void set(APIRequestInfo requestInfo) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new IllegalStateException("Request attributes are not set");
        }
        requestAttributes.setAttribute(
                APIRequestInfo.KEY, requestInfo, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Returns the requested media types (the resolver will fill in defaults in case none was
     * provided in the "f" parameter or in the Accept header
     */
    public List<MediaType> getRequestedMediaTypes() {
        return requestedMediaTypes;
    }

    void setRequestedMediaTypes(List<MediaType> requestedMediaTypes) {
        this.requestedMediaTypes = requestedMediaTypes;
    }

    /** Returns the message converters available in the dispatcher (as a read only collection) */
    public List<HttpMessageConverter<?>> getConverters() {
        return dispatcher.getConverters();
    }

    public Collection<MediaType> getProducibleMediaTypes(Class<?> responseType, boolean addHTML) {
        return dispatcher.getProducibleMediaTypes(responseType, addHTML);
    }

    /** Returns true if no indication was given as to what media type is to be returned */
    public boolean isAnyMediaTypeAccepted() {
        return requestedMediaTypes == null
                || ContentNegotiationManager.MEDIA_TYPE_ALL_LIST.equals(requestedMediaTypes);
    }

    /** Returns true if the given format has been requested */
    public boolean isFormatRequested(MediaType mediaType, MediaType defaultMediaType) {
        if (requestedMediaTypes == null) {
            return false;
        }

        if ((MEDIA_TYPE_ALL_LIST.equals(requestedMediaTypes))
                && (defaultMediaType != null && defaultMediaType.isCompatibleWith(mediaType))) {
            return true;
        }

        return requestedMediaTypes
                .stream()
                .filter(mt -> !mt.equals(MediaType.ALL))
                .anyMatch(curr -> mediaType.isCompatibleWith(curr));
    }

    /** Returns the {@link HttpServletRequest} for the current API request */
    public HttpServletRequest getRequest() {
        return request;
    }

    /** Returns the {@link HttpServletResponse} for the current API request */
    public HttpServletResponse getResponse() {
        return response;
    }

    public List<Link> getLinksFor(
            String path,
            Class<?> responseType,
            String titlePrefix,
            String classification,
            BiConsumer<MediaType, Link> linkUpdater,
            String rel,
            boolean includeHTML) {
        List<Link> result = new ArrayList<>();
        for (MediaType mediaType :
                APIRequestInfo.get().getProducibleMediaTypes(responseType, includeHTML)) {
            String format = mediaType.toString();
            Map<String, String> params = Collections.singletonMap("f", format);
            String url = buildURL(baseURL, path, params, URLMangler.URLType.SERVICE);
            String linkTitle = titlePrefix + format;
            Link link = new Link(url, rel, format, linkTitle);
            link.setClassification(classification);
            if (linkUpdater != null) {
                linkUpdater.accept(mediaType, link);
            }
            result.add(link);
        }
        return result;
    }

    /**
     * Returns the base URL for the current service, that is, GeoServer base url + service base. Can
     * be called only after the service has been looked up, will otherwise throw a descriptive
     * exception.
     */
    public String getServiceBaseURL() {
        return Optional.ofNullable(Dispatcher.REQUEST.get())
                .map(r -> r.getServiceDescriptor())
                .map(sd -> sd.getService())
                .map(s -> s.getClass())
                .map(c -> APIDispatcher.getApiServiceAnnotation(c))
                .map(a -> a.landingPage())
                .map(lp -> ResponseUtils.appendPath(baseURL, lp))
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Could not find a service base URL at this stage, maybe the service has not been dispatched yet"));
    }
}
