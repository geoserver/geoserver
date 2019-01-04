/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.gwc.config.GWCServiceEnablementInterceptor;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geotools.util.Version;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.service.tms.TMSDocumentFactory;
import org.geowebcache.util.ServletUtils;

/**
 * Service bean used as service implementation for the GeoServer {@link Dispatcher} when processing
 * GWC service requests.
 *
 * <p>See the package documentation for more insights on how these all fit together.
 */
public class GwcServiceProxy {

    private final ServiceInfoImpl serviceInfo;

    private final GeoWebCacheDispatcher gwcDispatcher;

    private final Pattern kmlPattern = Pattern.compile("/service/kml/", Pattern.CASE_INSENSITIVE);
    private final Pattern kmlXyzPattern =
            Pattern.compile(
                    ".*x(?<x>[0-9]+)y(?<y>[0-9]+)z(?<z>[0-9]+).*?", Pattern.CASE_INSENSITIVE);

    public GwcServiceProxy() {
        serviceInfo = new ServiceInfoImpl();
        serviceInfo.setId("gwc");
        serviceInfo.setName("gwc");
        serviceInfo.setEnabled(true);
        serviceInfo.setVersions(ImmutableList.of(new Version("1.0.0")));
        gwcDispatcher = GeoWebCacheExtensions.bean(GeoWebCacheDispatcher.class);
    }

    /**
     * This method is here to assist the {@link DisabledServiceCheck} callback, that uses reflection
     * to find such a method and check if the returned service info is {@link
     * ServiceInfo#isEnabled() enabled} (and hence avoid the WARNING message it spits out if this
     * method is not found); not though, that in the interest of keeping a single {@link
     * GwcServiceProxy} to proxy all gwc provided services (wmts, tms, etc), the service info
     * returned here will always be enabled, we already have a GWC {@link
     * GWCServiceEnablementInterceptor service interceptor} aspect that decorates specific gwc
     * services to check for enablement.
     */
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    /**
     * This method is the only operation defined for the {@link org.geoserver.platform.Service} bean
     * descriptor, and is meant to execute all requests to /gwc/service/*, delegating to the {@code
     * GeoWebCacheDispatcher}'s {@link GeoWebCacheDispatcher#handleRequest(HttpServletRequest,
     * HttpServletResponse) handleRequest(HttpServletRequest, HttpServletResponse)} method, and
     * return a response object so that the GeoServer {@link Dispatcher} looks up a {@link Response}
     * that finally writes the result down to the client response stream.
     *
     * @param rawRequest
     * @param rawRespose
     * @see GwcOperationProxy
     * @see GwcResponseProxy
     */
    public GwcOperationProxy dispatch(HttpServletRequest rawRequest, HttpServletResponse rawRespose)
            throws Exception {

        //        DispatcherController.BASE_URL.set(ResponseUtils.baseURL(rawRequest));

        ResponseWrapper responseWrapper = new ResponseWrapper(rawRespose);

        gwcDispatcher.handleRequest(rawRequest, responseWrapper);

        final String contentType = responseWrapper.getContentType();
        final Map<String, String> headers = responseWrapper.getHeaders();
        final byte[] bytes = responseWrapper.out.getBytes();

        return new GwcOperationProxy(contentType, headers, bytes);
    }

    private static Map<String, String> splitTMSParams(HttpServletRequest request) {

        // get all elements of the pathInfo after the leading "/tms/1.0.0/" part.
        String pathInfo = request.getPathInfo();
        pathInfo =
                pathInfo.substring(pathInfo.indexOf(TMSDocumentFactory.TILEMAPSERVICE_LEADINGPATH));
        String[] params = pathInfo.split("/");
        // {"tms", "1.0.0", "img states@EPSG:4326", ... }

        int paramsLength = params.length;

        Map<String, String> parsed = new HashMap<>();

        if (params.length < 4) {
            return Collections.emptyMap();
        }

        String[] yExt = params[paramsLength - 1].split("\\.");

        parsed.put("x", params[paramsLength - 2]);
        parsed.put("y", yExt[0]);
        parsed.put("z", params[paramsLength - 3]);

        String layerNameAndSRS = params[2];
        String[] lsf =
                ServletUtils.URLDecode(layerNameAndSRS, request.getCharacterEncoding()).split("@");
        parsed.put("layerId", lsf[0]);
        if (lsf.length >= 3) {
            parsed.put("gridSetId", lsf[1]);
        }

        parsed.put("fileExtension", yExt[1]);

        return parsed;
    }

    /** */
    private final class ResponseWrapper extends HttpServletResponseWrapper {

        final BufferedServletOutputStream out = new BufferedServletOutputStream();
        Map<String, String> headers = new LinkedHashMap<String, String>();

        private ResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    private static class BufferedServletOutputStream extends ServletOutputStream {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4096);

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }
}
