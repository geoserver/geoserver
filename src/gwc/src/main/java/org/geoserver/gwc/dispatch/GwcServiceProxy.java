/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCServiceEnablementInterceptor;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.GeoWebCacheExtensions;

import com.google.common.collect.ImmutableList;

/**
 * Service bean used as service implementation for the GeoServer {@link Dispatcher} when processing
 * GWC service requests.
 * <p>
 * See the package documentation for more insights on how these all fit together.
 */
public class GwcServiceProxy {

    private final ServiceInfoImpl serviceInfo;

    private final GeoWebCacheDispatcher gwcDispatcher;

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
     * to find such a method and check if the returned service info is
     * {@link ServiceInfo#isEnabled() enabled} (and hence avoid the WARNING message it spits out if
     * this method is not found); not though, that in the interest of keeping a single
     * {@link GwcServiceProxy} to proxy all gwc provided services (wmts, tms, etc), the service info
     * returned here will always be enabled, we already have a GWC
     * {@link GWCServiceEnablementInterceptor service interceptor} aspect that decorates specific
     * gwc services to check for enablement.
     * 
     * @return
     */
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    /**
     * This method is the only operation defined for the {@link org.geoserver.platform.Service} bean
     * descriptor, and is meant to execute all requests to /gwc/service/*, delegating to the
     * {@code GeoWebCacheDispatcher}'s
     * {@link GeoWebCacheDispatcher#handleRequest(HttpServletRequest, HttpServletResponse)
     * handleRequest(HttpServletRequest, HttpServletResponse)} method, and return a response object
     * so that the GeoServer {@link Dispatcher} looks up a {@link Response} that finally writes the
     * result down to the client response stream.
     * 
     * @param rawRequest
     * @param rawRespose
     * @return
     * @throws Exception
     * @see GwcOperationProxy
     * @see GwcResponseProxy
     */
    public GwcOperationProxy dispatch(HttpServletRequest rawRequest, HttpServletResponse rawRespose)
            throws Exception {

        ResponseWrapper responseWrapper = new ResponseWrapper(rawRespose);
        
        if (GWC.get().getConfig().isSecurityEnabled()) {
            verifyAccess(rawRequest);
        }

        gwcDispatcher.handleRequest(rawRequest, responseWrapper);

        final String contentType = responseWrapper.getContentType();
        final Map<String, String> headers = responseWrapper.getHeaders();
        final byte[] bytes = responseWrapper.out.getBytes();

        return new GwcOperationProxy(contentType, headers, bytes);
    }
    
    /***
     * Do a security check using the geoserver internal catalog security for a specific gwc request 
     * WMS-C requests are handled as regular WMS requests
     * 
     * @param rawRequest the request
     * @throws org.geotools.ows.ServiceException
     */
    public void verifyAccess(HttpServletRequest rawRequest) throws org.geotools.ows.ServiceException {     
        Map parameters = KvpUtils.normalize(rawRequest.getParameterMap());            

        if (rawRequest.getPathInfo().toLowerCase().startsWith("/service/wms")) {
            
            //trick geoserver security into thinking this is a regular wms request
            Dispatcher.REQUEST.get().setService("wms");
            Dispatcher.REQUEST.get().setRequest((String) parameters.get("REQUEST"));

            String layerstr = (String) parameters.get("LAYERS");
            String bboxstr = (String) parameters.get("BBOX");
            String srs = (String) parameters.get("SRS");
                        
            if (layerstr != null) {
                ReferencedEnvelope bbox = null;

                try {
                     bbox = (ReferencedEnvelope) new BBoxKvpParser().parse(bboxstr);                            
                } catch (Exception e) {
                    throw new ServiceException("Invalid bbox: " + bboxstr, e, "MissingOrInvalidParameter");
                }
                if (srs != null) {
                    try {
                        bbox = new ReferencedEnvelope(bbox, CRS.decode(srs));
                    } catch (Exception e) {
                        throw new ServiceException("Invalid srs: " + srs, e, "MissingOrInvalidParameter");
                    } 
                }
                
                String[] layers = layerstr.split(",");
                for (String layerName: layers) {
                    layerName = layerName.trim();
                    GWC.get().verifyAccessLayer(layerName, bbox);
                }
            }
            
        } else if (rawRequest.getPathInfo().toLowerCase().startsWith("/service/wmts")) {
            String layer = (String) parameters.get("LAYER");
                        
            if (layer != null) {
                GWC.get().verifyAccessLayer(layer, null);
            }
            
        } else if (rawRequest.getPathInfo().toLowerCase().startsWith("/service/tms/1.0.0/")) {
            String layer = rawRequest.getPathInfo().toLowerCase().substring("/service/tms/1.0.0/".length());
            if (layer.indexOf('/') >= 0) {
                layer = layer.substring(0, layer.indexOf('/'));
            }
            if (layer.indexOf('@') >= 0) {
                layer = layer.substring(0, layer.indexOf('@'));
            }
            
            GWC.get().verifyAccessLayer(layer, null);
        } else if (rawRequest.getPathInfo().toLowerCase().startsWith("/service/kml/")) {
            String layer = rawRequest.getPathInfo().toLowerCase().substring("/service/kml/".length());
            if (layer.indexOf('.') >= 0) {
                layer = layer.substring(0, layer.indexOf('.'));
            }
            
            GWC.get().verifyAccessLayer(layer, null);
        } else if (rawRequest.getPathInfo().toLowerCase().startsWith("/service/gmaps") || rawRequest.getPathInfo().toLowerCase().startsWith("/service/ve")) {
            String layerstr = (String) parameters.get("LAYERS");
                        
            if (layerstr != null) {                
                String[] layers = layerstr.split(",");
                for (String layerName: layers) {
                    layerName = layerName.trim();
                    GWC.get().verifyAccessLayer(layerName, null);
                }
            }            
        }
    }

    /**
     * 
     *
     */
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
