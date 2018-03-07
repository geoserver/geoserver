/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.RequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple hack to bridge part of the path based approach in WFS 3 to traditional OWS mappings.
 * If this is somehow generalized and brought into the main dispatcher, check OSEOFilter as well (in the OpenSearch
 * module)
 */
public class WFS3Filter implements GeoServerFilter {
    
    static final Logger LOGGER = Logging.getLogger(WFS3Filter.class);

    private final Catalog catalog;

    public WFS3Filter(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest requestHTTP = (HttpServletRequest) request;
            if (requestNeedsWrapper(requestHTTP)) {
                try {
                    request = new RequestWrapper(requestHTTP);
                } catch(HttpErrorCodeException exception) {
                    ((HttpServletResponse) response).sendError(exception.getErrorCode(), exception.getMessage());
                    return;
                }
            }
        }
        chain.doFilter(request, response);

    }

    private boolean requestNeedsWrapper(HttpServletRequest requestHTTP) {
        String path = requestHTTP.getServletPath();
        return path.contains("wfs3");
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    private class RequestWrapper extends HttpServletRequestWrapper {
        private String request;
        private String typeName;
        private String outputFormat;
        private String featureId;
        private boolean mapped;

        private RequestWrapper(HttpServletRequest wrapped) {
            super(wrapped);
            String pathInfo = wrapped.getPathInfo();
            if (pathInfo.equals("/")) {
                request = "contents";
            } else if (pathInfo.endsWith("api")) {
                request = "api";
            } else if (pathInfo.endsWith("api/conformance")) {
                request = "conformance";
            } else if (pathInfo.contains("__")) {
                if (pathInfo.startsWith("/")) {
                    pathInfo = pathInfo.substring(1);
                }
                // selection by feature id?
                if (pathInfo.contains("/")) {
                    String[] split = pathInfo.split("/");
                    if (split.length > 2) {
                        throw new HttpErrorCodeException(HttpStatus.NOT_FOUND.value(), "Invalid path " + pathInfo);
                    }
                    pathInfo = split[0];
                    featureId = split[1];
                }
                List<LayerInfo> layers = NCNameResourceCodec.getLayers(catalog, pathInfo);
                if (!layers.isEmpty()) {
                    request = "getFeature";
                    typeName = layers.get(0).prefixedName();
                } else {
                    throw new HttpErrorCodeException(HttpStatus.NOT_FOUND.value(), "Could not find layer " + pathInfo);
                }
            } else {
                throw new HttpErrorCodeException(HttpStatus.NOT_FOUND.value(), "Unsupported path " + pathInfo);
            }
            
            mapped = request != null;

            // everythign defaults to JSON in WFS3
            String f = wrapped.getParameter("f");
            if (f != null) {
                if ("json".equalsIgnoreCase(f)) {
                    this.outputFormat = BaseRequest.JSON_MIME;
                } else if("yaml".equalsIgnoreCase(f)) {
                    this.outputFormat = BaseRequest.YAML_MIME;
                } else if("html".equalsIgnoreCase(f)) {
                    this.outputFormat = BaseRequest.HTML_MIME;
                } else {
                    this.outputFormat = f;
                }
            } else {
                this.outputFormat = BaseRequest.JSON_MIME;
            }
        }

        @Override
        public String getRequestURI() {
            return super.getContextPath() + "/wfs3";
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map filtered = new HashMap<>(original);
            filtered.put("service", "WFS");
            filtered.put("version", "3.0.0");
            filtered.put("request", request);
            filtered.put("srsName", "EPSG:4326");
            String bbox = super.getParameter("bbox");
            if (bbox != null) {
                try {
                    // is it parseable?
                    BBoxKvpParser parser = new BBoxKvpParser();
                    ReferencedEnvelope envelope = (ReferencedEnvelope) parser.parse(bbox);
                    // if 2D and lacking a CRS, force WGS84
                    if (envelope.getCoordinateReferenceSystem() == null && envelope.getDimension() == 2) {
                        filtered.put("bbox", bbox + ",EPSG:4326");
                    }
                } catch (Exception expected) {
                    // fine, the actual request parsing later on will deal with this
                }
            }
            if (typeName != null) {
                filtered.put("typeName", typeName);
            }
            if (outputFormat != null) {
                filtered.put("outputFormat", outputFormat);
            }
            if (featureId != null && !featureId.isEmpty()) {
                filtered.put("featureId", featureId);
            }
            return filtered;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] value = getParameterMap().get(name);
            return value;
        }

        @Override
        public String getParameter(String name) {
            String[] values = getParameterValues(name);
            if (values == null || values.length == 0) {
                return null;
            } else {
                return values[0];
            }
        }
    }

}
