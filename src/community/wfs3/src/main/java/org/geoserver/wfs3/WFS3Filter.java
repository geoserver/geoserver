/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geoserver.wfs3.response.RFCGeoJSONFeaturesResponse;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

/**
 * Simple hack to bridge part of the path based approach in WFS 3 to traditional OWS mappings. If
 * this is somehow generalized and brought into the main dispatcher, check OSEOFilter as well (in
 * the OpenSearch module)
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
                } catch (HttpErrorCodeException exception) {
                    ((HttpServletResponse) response)
                            .sendError(exception.getErrorCode(), exception.getMessage());
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean requestNeedsWrapper(HttpServletRequest requestHTTP) {
        String path = requestHTTP.getRequestURI();
        return path.contains("wfs3") && !path.contains("wfs3css");
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    private class RequestWrapper extends HttpServletRequestWrapper {
        private String pathInfo;
        private String request;
        private String typeName;
        private String outputFormat;
        private String featureId;
        private String limit;
        private String tilingScheme;
        private String level;
        private String row;
        private String col;
        private String styleId;

        private RequestWrapper(HttpServletRequest wrapped) {
            super(wrapped);
            pathInfo = getPathInfo(wrapped);
            if (pathInfo.equals("/") || pathInfo.equals("")) {
                request = "landingPage";
            } else if (pathInfo.equals("/api") || pathInfo.equals("/api/")) {
                request = "api";
            } else if (pathInfo.endsWith("/conformance") || pathInfo.endsWith("/conformance/")) {
                request = "conformance";
            } else if (pathInfo.matches("/styles/?")) {
                request = wrapped.getMethod().toLowerCase() + "Styles";
            } else if (pathInfo.matches("/styles/([^/]+)/?")) {
                request = wrapped.getMethod().toLowerCase() + "Style";
                Matcher matcher = Pattern.compile("/styles/([^/]+)/?").matcher(pathInfo);
                matcher.matches();
                this.styleId = matcher.group(1);
            } else if (pathInfo.matches("/tilingSchemes/([^/]+)/?")) {
                request = "describeTilingScheme";
                Matcher matcher = Pattern.compile("/tilingSchemes/([^/]+)/?").matcher(pathInfo);
                matcher.matches();
                this.tilingScheme = matcher.group(1);
            } else if (pathInfo.matches("/collections/([^/]+)/tiles/([^/]+)/?")) {
                request = "describeTilingScheme";
                Matcher matcher =
                        Pattern.compile("/collections/([^/]+)/tiles/([^/]+)/?").matcher(pathInfo);
                matcher.matches();
                this.tilingScheme = matcher.group(2);
            } else if (pathInfo.matches("/collections/([^/]+)/styles/?")) {
                request = wrapped.getMethod().toLowerCase() + "Styles";
                Matcher matcher =
                        Pattern.compile("/collections/([^/]+)/styles/?").matcher(pathInfo);
                matcher.matches();
                setLayerName(matcher.group(1));
            } else if (pathInfo.matches("/collections/([^/]+)/styles/([^/]+)/?")) {
                request = wrapped.getMethod().toLowerCase() + "Style";
                Matcher matcher =
                        Pattern.compile("/collections/([^/]+)/styles/([^/]+)/?").matcher(pathInfo);
                matcher.matches();
                setLayerName(matcher.group(1));
                this.styleId = matcher.group(2);
            } else if (pathInfo.startsWith("/collections")) {
                List<Function<String, Boolean>> matchers = new ArrayList<>();
                matchers.add(
                        path -> {
                            Matcher matcher =
                                    Pattern.compile("/collections/([^/]+)/items/(.+)")
                                            .matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "getFeature";
                                String layerName = matcher.group(1);
                                if (layerName != null) {
                                    layerName = urlDecode(layerName);
                                }
                                setLayerName(layerName);
                                this.featureId = urlDecode(matcher.group(2));
                            }
                            return matches;
                        });
                matchers.add(
                        path -> {
                            Matcher matcher =
                                    Pattern.compile("/collections/([^/]+)/items/?").matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "getFeature";
                                String layerName = matcher.group(1);
                                if (layerName != null) {
                                    layerName = urlDecode(layerName);
                                }
                                setLayerName(layerName);
                            }
                            return matches;
                        });
                matchers.add(
                        path -> {
                            Matcher matcher =
                                    Pattern.compile("/collections/([^/]+)/tiles/?").matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "tilingSchemes";
                                String layerName = matcher.group(1);
                                if (layerName != null) {
                                    layerName = urlDecode(layerName);
                                }
                                setLayerName(layerName);
                            }
                            return matches;
                        });
                matchers.add(
                        path -> {
                            Matcher matcher =
                                    Pattern.compile("/collections/([^/]+)/?").matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "collection";
                                String layerName = matcher.group(1);
                                if (layerName != null) {
                                    layerName = urlDecode(layerName);
                                }
                                setLayerName(layerName);
                            }
                            return matches;
                        });
                matchers.add(
                        path -> {
                            Matcher matcher = Pattern.compile("/collections/?").matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "collections";
                            }
                            return matches;
                        });
                // collections/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}
                matchers.add(
                        path -> {
                            Matcher matcher =
                                    Pattern.compile(
                                                    "/collections/([^/]+)/tiles/([^/]+)/([^/]+)/([^/]+)/([^/]+)/?")
                                            .matcher(path);
                            boolean matches = matcher.matches();
                            if (matches) {
                                request = "getTile";
                                String layerName = matcher.group(1);
                                if (layerName != null) {
                                    layerName = urlDecode(layerName);
                                }
                                setLayerName(layerName);
                                this.tilingScheme = urlDecode(matcher.group(2));
                                this.level = urlDecode(matcher.group(3));
                                this.row = urlDecode(matcher.group(4));
                                this.col = urlDecode(matcher.group(5));
                            }
                            return matches;
                        });

                // loop over the matchers
                boolean matched = false;
                for (Function<String, Boolean> matcher : matchers) {
                    if (matcher.apply(pathInfo)) {
                        matched = true;
                        break;
                    }
                }
                // if none matches, complain
                if (!matched) {
                    throw new HttpErrorCodeException(
                            HttpStatus.NOT_FOUND.value(), "Unsupported path " + pathInfo);
                }
            } else if (pathInfo.startsWith("/tilingSchemes")) {
                request = "tilingSchemes";
            } else {
                throw new HttpErrorCodeException(
                        HttpStatus.NOT_FOUND.value(), "Unsupported path " + pathInfo);
            }

            // everything defaults to JSON in WFS3
            String f = wrapped.getParameter("f");
            if (f != null) {
                if ("json".equalsIgnoreCase(f)) {
                    this.outputFormat =
                            "getFeature".equals(request)
                                    ? RFCGeoJSONFeaturesResponse.MIME
                                    : BaseRequest.JSON_MIME;
                } else if ("yaml".equalsIgnoreCase(f)) {
                    this.outputFormat = BaseRequest.YAML_MIME;
                } else if ("html".equalsIgnoreCase(f)) {
                    this.outputFormat = BaseRequest.HTML_MIME;
                } else {
                    this.outputFormat = f;
                }
            } else if ("getFeature".equals(request)) {
                this.outputFormat = RFCGeoJSONFeaturesResponse.MIME;
            } else if ("getTile".equals(request)) {
                this.outputFormat = MapBoxTileBuilderFactory.MIME_TYPE;
            }

            // support for the limit parameter
            String limit = wrapped.getParameter("limit");
            if (limit != null) {
                this.limit = limit;
            }
        }

        /**
         * Extracts the path info in a way that accounts for virtual services, parameter extractor
         * and whatnot
         */
        private String getPathInfo(HttpServletRequest wrapped) {
            String fullPath = wrapped.getRequestURI();
            int idx = fullPath.indexOf("wfs3");
            return fullPath.substring(idx + 4);
        }

        private void setLayerName(String layerName) {
            List<LayerInfo> layers = NCNameResourceCodec.getLayers(catalog, layerName);
            if (!layers.isEmpty()) {
                typeName = layers.get(0).prefixedName();
            } else {
                throw new HttpErrorCodeException(
                        HttpStatus.NOT_FOUND.value(), "Could not find feature type " + layerName);
            }
        }

        @Override
        public String getRequestURI() {
            String requestURI = super.getRequestURI();
            return requestURI.substring(0, requestURI.length() - pathInfo.length());
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public String getMethod() {
            // hack to ensure the Dispatcher does not handle this one as a OWS style post and
            // consequently tries to find the request type in the payload
            return "GET";
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map<String, String[]> filtered = new HashMap<>(original);
            filtered.put("service", new String[] {"WFS"});
            filtered.put("version", new String[] {"3.0.0"});
            filtered.put("request", new String[] {request});
            if ("GoogleMapsCompatible".equals(this.tilingScheme)) {
                filtered.put("srsName", new String[] {"EPSG:3857"});
            } else {
                filtered.put("srsName", new String[] {"EPSG:4326"});
            }

            String bbox = super.getParameter("bbox");
            if (bbox != null) {
                try {
                    // is it parseable?
                    BBoxKvpParser parser = new BBoxKvpParser();
                    ReferencedEnvelope envelope = (ReferencedEnvelope) parser.parse(bbox);
                    // if 2D and lacking a CRS, force WGS84
                    if (envelope.getCoordinateReferenceSystem() == null
                            && envelope.getDimension() == 2) {
                        filtered.put("bbox", new String[] {bbox + ",EPSG:4326"});
                    }
                } catch (Exception expected) {
                    // fine, the actual request parsing later on will deal with this
                }
            }
            if (typeName != null) {
                if (request.toLowerCase().contains("style")) {
                    filtered.put("layerName", new String[] {typeName});
                } else {
                    filtered.put("typeName", new String[] {typeName});
                    filtered.put("typeNames", new String[] {typeName});
                }
            }
            if (outputFormat != null) {
                filtered.put("outputFormat", new String[] {outputFormat});
            }
            if (featureId != null && !featureId.isEmpty()) {
                filtered.put("featureId", new String[] {featureId});
            }
            if (limit != null && !limit.isEmpty()) {
                filtered.put("count", new String[] {limit});
            }
            if (tilingScheme != null) filtered.put("tilingScheme", new String[] {tilingScheme});
            if (level != null) filtered.put("level", new String[] {level});
            if (row != null) filtered.put("row", new String[] {row});
            if (col != null) filtered.put("col", new String[] {col});
            if (styleId != null) filtered.put("styleId", new String[] {styleId});
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

    /** URL decodes the given string */
    private String urlDecode(String name) {
        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return name;
    }
}
