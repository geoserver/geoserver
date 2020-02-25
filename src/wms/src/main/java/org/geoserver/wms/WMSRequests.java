/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Envelope;
import org.springframework.util.StringUtils;
import org.vfny.geoserver.util.Requests;

/**
 * Utility class for creating wms requests.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Carlo Cancellieri - Geo-Solutions SAS
 * @see Requests
 */
public class WMSRequests {

    /**
     * Encodes the url of a GetMap request pointing to a tile cache if one exists.
     *
     * <p>The tile cache location is determined from {@link GeoServer#getTileCache()}. If the above
     * method returns null this method falls back to the behaviour of {@link
     * #getGetMapUrl(WMSMapContent, Layer, Envelope, String[])}.
     *
     * <p>If the <tt>layer</tt> argument is <code>null</code>, the request is made including all
     * layers in the <tt>mapContexT</tt>.
     *
     * <p>If the <tt>bbox</tt> argument is <code>null</code>. {@link
     * WMSMapContent#getAreaOfInterest()} is used for the bbox parameter.
     *
     * @param req The getMap request.
     * @param layer The Map layer, may be <code>null</code>.
     * @param layerIndex The index of the layer in the request
     * @param bbox The bounding box of the request, may be <code>null</code>.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     * @return The full url for a getMap request.
     */
    public static String getTiledGetMapUrl(
            GeoServer geoserver,
            GetMapRequest req,
            Layer layer,
            int layerIndex,
            Envelope bbox,
            String[] kvp) {

        HashMap<String, String> params =
                getGetMapParams(
                        req, layer.getTitle(), layerIndex, layer.getStyle().getName(), bbox, kvp);

        String baseUrl = getTileCacheBaseUrl(req, geoserver);

        if (baseUrl == null) {
            return ResponseUtils.buildURL(req.getBaseUrl(), "wms", params, URLType.SERVICE);
        }

        return ResponseUtils.buildURL(baseUrl, "", params, URLType.EXTERNAL);
    }

    /**
     * Returns the full url to the tile cache used by GeoServer ( if any ).
     *
     * <p>If the tile cache set in the configuration ({@link GeoServer#getTileCache()}) is set to an
     * asbsolute url, it is simply returned. Otherwise the value is appended to the scheme and host
     * of the supplied <tt>request</tt>.
     *
     * @param req The request.
     * @param geoServer The geoserver configuration.
     * @return The url to the tile cache, or <code>null</code> if no tile cache set.
     */
    private static String getTileCacheBaseUrl(GetMapRequest req, GeoServer geoServer) {
        // first check if tile cache set
        String tileCacheBaseUrl = (String) geoServer.getGlobal().getMetadata().get("tileCache");

        if (tileCacheBaseUrl != null) {
            // two possibilities, local path, or full remote path
            try {
                new URL(tileCacheBaseUrl);

                // full url, return it
                return tileCacheBaseUrl;
            } catch (MalformedURLException e1) {
                // try relative to the same host as request
                try {
                    String baseUrl = req.getBaseUrl();
                    // GR: this replicates what the old code depending on httpServletRequest was
                    // doing: req.getScheme() + "://" + req.getServerName()
                    URL base = new URL(baseUrl);
                    baseUrl = base.getProtocol() + ":" + base.getPort() + "//" + base.getHost();
                    String url = Requests.appendContextPath(baseUrl, tileCacheBaseUrl);
                    new URL(url);

                    // cool return it
                    return url;
                } catch (MalformedURLException e2) {
                    // out of guesses
                }
            }
        }

        return null;
    }

    /**
     * Encodes the url of a GetMap request.
     *
     * <p>If the <tt>layer</tt> argument is <code>null</code>, the request is made including all
     * layers in the <tt>mapContexT</tt>.
     *
     * <p>If the <tt>bbox</tt> argument is <code>null</code>. {@link
     * WMSMapContent#getAreaOfInterest()} is used for the bbox parameter.
     *
     * @param req The getMap request
     * @param layer The Map layer, may be <code>null</code>.
     * @param layerIndex The index of the layer in the request
     * @param bbox The bounding box of the request, may be <code>null</code>.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     * @return The full url for a getMap request.
     */
    public static String getGetMapUrl(
            GetMapRequest req, Layer layer, int layerIndex, Envelope bbox, String[] kvp) {

        String layerName = layer != null ? layer.getTitle() : null;
        String style = layer != null ? layer.getStyle().getName() : null;

        LinkedHashMap<String, String> params =
                getGetMapParams(req, layerName, layerIndex, style, bbox, kvp);
        return ResponseUtils.buildURL(req.getBaseUrl(), "wms", params, URLType.SERVICE);
    }

    /**
     * Encodes the url of a GetMap request.
     *
     * <p>If the <tt>layer</tt> argument is <code>null</code>, the request is made including all
     * layers in the <tt>mapContexT</tt>.
     *
     * <p>If the <tt>style</tt> argument is not <code>null</code> and the <tt>layer</tt> argument is
     * <code>null</code>, then the default style for that layer is used.
     *
     * <p>If the <tt>bbox</tt> argument is <code>null</code>. {@link
     * WMSMapContent#getAreaOfInterest()} is used for the bbox parameter.
     *
     * @param req The getMap request
     * @param layer The layer name, may be <code>null</code>.
     * @param layerIndex The index of the layer in the request.
     * @param style The style name, may be <code>null</code>
     * @param bbox The bounding box of the request, may be <code>null</code>.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     * @return The full url for a getMap request.
     */
    public static String getGetMapUrl(
            GetMapRequest req,
            String layer,
            int layerIndex,
            String style,
            Envelope bbox,
            String[] kvp) {
        HashMap<String, String> params = getGetMapParams(req, layer, layerIndex, style, bbox, kvp);
        return ResponseUtils.buildURL(req.getBaseUrl(), "wms", params, URLType.SERVICE);
    }

    /**
     * Encodes the url of a GetLegendGraphic request.
     *
     * @param req The wms request.
     * @param layers The layers, may not be <code>null</code>.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     * @return The full url for a getMap request.
     */
    public static String getGetLegendGraphicUrl(WMSRequest req, Layer[] layers, String[] kvp) {
        // parameters
        HashMap<String, String> params = new HashMap<String, String>();

        params.put("service", "wms");
        params.put("request", "GetLegendGraphic");
        params.put("version", "1.1.1");
        params.put("format", "image/png");
        params.put("layer", getLayerTitles(layers));
        params.put("style", getLayerStyles(layers));
        params.put("height", "20");
        params.put("width", "20");

        // overrides / additions
        for (int i = 0; (kvp != null) && (i < kvp.length); i += 2) {
            params.put(kvp[i], kvp[i + 1]);
        }

        return ResponseUtils.buildURL(req.getBaseUrl(), "wms", params, URLType.SERVICE);
    }

    private static String getLayerTitles(Layer[] layers) {
        StringBuilder sb = new StringBuilder();
        for (Layer layer : layers) {
            if (layer != null && layer.getTitle() != null) {
                sb.append(layer.getTitle());
            }
            sb.append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static String getLayerStyles(Layer[] layers) {
        StringBuilder sb = new StringBuilder();
        for (Layer layer : layers) {
            sb.append(layer.getStyle().getName()).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    /** Helper method for encoding GetMap request parameters. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static LinkedHashMap<String, String> getGetMapParams(
            GetMapRequest req,
            String layer,
            int layerIndex,
            String style,
            Envelope bbox,
            String[] kvp) {
        // parameters
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();

        params.put("service", "wms");
        params.put("request", "GetMap");
        params.put("version", "1.1.1");

        params.put("format", req.getFormat());

        StringBuffer layers = new StringBuffer();
        StringBuffer styles = new StringBuffer();

        boolean useLayerIndex = true;
        int count = 0;
        for (int i = 0; i < req.getLayers().size(); i++) {
            if (layer != null && layer.equals(req.getLayers().get(i).getName())) {
                ++count;
            }
        }
        // only one of each layer in the request
        if (count == 1) {
            useLayerIndex = false;
        }

        if (layer != null) {
            layers.append(layer);
            if (style != null) {
                styles.append(style);
            } else {
                // use default for layer
                if (useLayerIndex) {
                    styles.append(req.getLayers().get(layerIndex).getDefaultStyle().getName());
                } else {
                    for (int i = 0; i < req.getLayers().size(); i++) {
                        if (layer.equals(req.getLayers().get(i).getName())) {
                            styles.append(req.getLayers().get(i).getDefaultStyle().getName());
                        }
                    }
                }
            }
        } else {
            // no layer specified, use layers+styles specified by request
            for (int i = 0; i < req.getLayers().size(); i++) {
                MapLayerInfo mapLayer = req.getLayers().get(i);
                Style s = (Style) req.getStyles().get(0);

                layers.append(mapLayer.getName()).append(",");
                styles.append(s.getName()).append(",");
            }

            layers.setLength(layers.length() - 1);
            styles.setLength(styles.length() - 1);
        }

        params.put("layers", layers.toString());
        params.put("styles", styles.toString());

        // filters, we grab them from the original raw kvp since re-encoding
        // them from objects is kind of silly
        Map<String, String> kvpMap = req.getRawKvp();
        if (layer != null) {
            // only get filters for the layer
            int index = 0;

            if (useLayerIndex) {
                index = layerIndex;
            } else {
                for (; index < req.getLayers().size(); index++) {
                    if (req.getLayers().get(index).getName().equals(layer)) {
                        break;
                    }
                }
            }
            index = getRawLayerIndex(req, index);

            if (req.getRawKvp().get("filter") != null) {
                // split out the filter we need
                List filters =
                        KvpUtils.readFlat(
                                (String) req.getRawKvp().get("filter"), KvpUtils.OUTER_DELIMETER);
                params.put("filter", (String) filters.get(index));
            } else if (req.getRawKvp().get("cql_filter") != null) {
                // split out the filter we need
                List filters =
                        KvpUtils.readFlat(
                                (String) req.getRawKvp().get("cql_filter"), KvpUtils.CQL_DELIMITER);
                params.put("cql_filter", (String) filters.get(index));
            } else if (req.getRawKvp().get("featureid") != null) {
                // semantics of feature id slightly different, replicate entire value
                params.put("featureid", req.getRawKvp().get("featureid"));
            }
            if (!StringUtils.isEmpty(kvpMap.get("interpolations"))) {
                List<String> interpolations = KvpUtils.readFlat(kvpMap.get("interpolations"));
                if (!interpolations.get(index).isEmpty()) {
                    params.put("interpolations", interpolations.get(index));
                }
            }
            if (!StringUtils.isEmpty(kvpMap.get("sortby"))) {
                List<String> sortBy =
                        KvpUtils.readFlat(kvpMap.get("sortby"), KvpUtils.OUTER_DELIMETER);
                if (!sortBy.get(index).isEmpty()) {
                    params.put("sortby", sortBy.get(index));
                }
            }

        } else {
            // include all
            if (req.getRawKvp().get("filter") != null) {
                params.put("filter", req.getRawKvp().get("filter"));
            } else if (req.getRawKvp().get("cql_filter") != null) {
                params.put("cql_filter", req.getRawKvp().get("cql_filter"));
            } else if (req.getRawKvp().get("featureid") != null) {
                params.put("featureid", req.getRawKvp().get("featureid"));
            }
            if (!StringUtils.isEmpty(kvpMap.get("interpolations"))) {
                params.put("interpolations", kvpMap.get("interpolations"));
            }
            if (!StringUtils.isEmpty(kvpMap.get("sortby"))) {
                params.put("sortby", kvpMap.get("sortby"));
            }
        }
        // Jira: #GEOS-6411: adding time and elevation support in case of a timeserie layer
        if (kvpMap.get("time") != null) {
            params.put("time", kvpMap.get("time"));
        }
        if (kvpMap.get("elevation") != null) {
            params.put("elevation", kvpMap.get("elevation"));
        }
        kvpMap.entrySet()
                .stream()
                .filter(e -> e.getKey().toLowerCase().startsWith("dim_"))
                .forEach(e -> params.put(e.getKey().toLowerCase(), e.getValue()));

        // image params
        params.put("height", String.valueOf(req.getHeight()));
        params.put("width", String.valueOf(req.getWidth()));
        params.put("transparent", "" + req.isTransparent());

        // bbox
        if (bbox == null) {
            bbox = req.getBbox();
        }
        if (bbox != null) {
            params.put("bbox", encode(bbox));
        }

        // srs
        params.put("srs", req.getSRS());

        // format options
        if (req.getFormatOptions() != null && !req.getFormatOptions().isEmpty()) {
            params.put("format_options", encodeFormatOptions(req.getFormatOptions()));
        }

        // view params
        if (req.getViewParams() != null && !req.getViewParams().isEmpty()) {
            params.put("viewParams", encodeFormatOptions(req.getViewParams()));
        }

        String propertyName = kvpMap.get("propertyName");
        if (propertyName != null && !propertyName.isEmpty()) {
            params.put("propertyName", propertyName);
        }
        if (!StringUtils.isEmpty(kvpMap.get("bgcolor"))) {
            params.put("bgcolor", kvpMap.get("bgcolor"));
        }
        if (!req.getExceptions().equals(GetMapRequest.SE_XML)) {
            params.put("exceptions", req.getExceptions());
        }
        if (req.getMaxFeatures() != null) {
            params.put("maxfeatures", req.getMaxFeatures().toString());
        }
        if (req.getRemoteOwsType() != null) {
            params.put("remote_ows_type", req.getRemoteOwsType());
        }
        if (req.getRemoteOwsURL() != null) {
            String url = ResponseUtils.urlDecode(req.getRemoteOwsURL().toString());
            params.put("remote_ows_url", url);
        }
        if (req.getScaleMethod() != null) {
            params.put("scalemethod", req.getScaleMethod().toString());
        }
        if (req.getStartIndex() != null) {
            params.put("startindex", req.getStartIndex().toString());
        }
        if (!req.getStyleFormat().equals(SLDHandler.FORMAT)) {
            params.put("style_format", req.getStyleFormat());
        }
        if (req.getStyleVersion() != null) {
            params.put("style_version", req.getStyleVersion());
        }
        if (Boolean.TRUE.equals(req.getValidateSchema())) {
            params.put("validateschema", "true");
        }

        if (req.getSld() != null) {
            // the request encoder will url-encode the url, if it has already url encoded
            // chars, the will be encoded twice
            params.put("sld", ResponseUtils.urlDecode(req.getSld().toString()));
        }

        if (req.getSldBody() != null) {
            params.put("sld_body", req.getSldBody());
        }

        if (req.getEnv() != null && !req.getEnv().isEmpty()) {
            params.put("env", encodeFormatOptions(req.getEnv()));
        }

        String tilesOrigin = kvpMap.get("tilesorigin");
        if (tilesOrigin != null && !tilesOrigin.isEmpty()) {
            params.put("tilesorigin", tilesOrigin);
        }

        if (req.isTiled()) {
            params.put("tiled", req.isTiled() ? "true" : "false");
        }

        String palette = kvpMap.get("palette");
        if (palette != null && !palette.isEmpty()) {
            params.put("palette", palette);
        }

        if (req.getBuffer() > 0) {
            params.put("buffer", Integer.toString(req.getBuffer()));
        }

        if (Double.compare(req.getAngle(), 0.0) != 0) {
            params.put("angle", Double.toString(req.getAngle()));
        }

        // overrides / additions
        for (int i = 0; (kvp != null) && (i < kvp.length); i += 2) {
            params.put(kvp[i], kvp[i + 1]);
        }

        return params;
    }

    /**
     * Layer groups are expanded into their component layers very early in the WMS GetMap request
     * handling process. This method is intended to reverse that process and find the index in the
     * raw layers KVP parameter that corresponds to a layer index after layer groups are expanded.
     *
     * @param req the WMS GetMap request
     * @param layerIndex the layer index in the expanded layers list
     * @return the layer index in the raw layers list
     * @throws IllegalArgumentException if unable to determine the raw layer index
     */
    @SuppressWarnings("unchecked")
    private static int getRawLayerIndex(GetMapRequest req, int layerIndex) {
        List<String> names = KvpUtils.readFlat(req.getRawKvp().get("layers"));
        if (names.size() == 1) {
            // single layer or layer group
            return 0;
        } else if (names.size() == req.getLayers().size()) {
            // multiple layers without any layer groups
            return layerIndex;
        }
        // layer group and one or more additional layers and/or layer groups
        List<?> layers =
                new LayerParser(WMS.get())
                        .parseLayers(names, req.getRemoteOwsURL(), req.getRemoteOwsType());
        int numLayers = 0;
        for (int index = 0; index < layers.size(); index++) {
            if (layers.get(index) instanceof LayerGroupInfo) {
                numLayers += ((LayerGroupInfo) layers.get(index)).layers().size();
            } else {
                numLayers++;
            }
            if (numLayers > layerIndex) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Unable to determine raw index for " + layerIndex + " in " + names);
    }

    /**
     * Copy the Entry matching the key from the kvp map and put it into the formatOptions map. If a
     * parameter is already present in formatOption map its value will be preserved.
     *
     * @param key the key to parse
     * @throws Exception - In the event of an unsuccesful parse.
     */
    public static void mergeEntry(
            Map<String, String> kvp, Map<String, Object> formatOptions, final String key)
            throws Exception {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);

        // strip out parsers which do not match current service/request/version
        String service = KvpUtils.getSingleValue(kvp, "service");
        String version = KvpUtils.getSingleValue(kvp, "version");
        String request = KvpUtils.getSingleValue(kvp, "request");

        KvpUtils.purgeParsers(parsers, service, version, request);

        String val = null;
        if ((val = kvp.get(key)) != null) {
            Object foValue = formatOptions.get(key);
            // if not found in format option
            if (foValue == null) {
                Object parsed = KvpUtils.parseKey(key, val, service, request, version, parsers);
                if (parsed != null) {
                    formatOptions.put(key, parsed);
                } else {
                    formatOptions.put(key, val);
                }
            }
        }
    }

    /**
     * Encodes a map of formation options to be used as the value in a kvp.
     *
     * <p>A string of the form 'key1:value1,value2;key2:value1;...', or the empty string if the
     * formatOptions map is empty.
     *
     * @param formatOptions The map of formation options.
     * @param sb StringBuffer to append to.
     */
    public static void encodeFormatOptions(Map formatOptions, StringBuffer sb) {
        if (formatOptions == null || formatOptions.isEmpty()) {
            return;
        }

        for (Iterator e = formatOptions.entrySet().iterator(); e.hasNext(); ) {
            Map.Entry entry = (Map.Entry) e.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();

            sb.append(key).append(":");
            if (val instanceof Collection) {
                Iterator i = ((Collection) val).iterator();
                while (i.hasNext()) {
                    sb.append(i.next()).append(",");
                }
                sb.setLength(sb.length() - 1);
            } else if (val.getClass().isArray()) {
                int len = Array.getLength(val);
                for (int i = 0; i < len; i++) {
                    Object o = Array.get(val, i);
                    if (o != null) {
                        sb.append(o).append(",");
                    }
                }
                sb.setLength(sb.length() - 1);
            } else {
                sb.append(val.toString());
            }
            sb.append(";");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Encodes a map of format options to be used as the value in a kvp.
     *
     * @param formatOptions The map of format options.
     * @return A string of the form 'key1:value1,value2;key2:value1;...', or the empty string if the
     *     formatOptions map is empty.
     */
    public static String encodeFormatOptions(Map formatOptions) {
        StringBuffer sb = new StringBuffer();
        encodeFormatOptions(formatOptions, sb);
        return sb.toString();
    }

    /**
     * Encodes a list of format option maps to be used as the value in a kvp.
     *
     * @param formatOptions The list of formation option maps.
     * @return A string of the form
     *     'key1.1:value1.1,value1.2;key1.2:value1.1;...[,key2.1:value2.1,value2.2;key2.2:value2.1]',
     *     or the empty string if the formatOptions list is empty.
     */
    public static String encodeFormatOptions(List<Map<String, String>> formatOptions) {
        if (formatOptions == null || formatOptions.isEmpty()) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (Map<String, String> map : formatOptions) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            encodeFormatOptions(map, sb);
        }

        sb.setLength(sb.length());
        return sb.toString();
    }

    /** Helper method to encode an envelope to be used in a wms request. */
    static String encode(Envelope box) {
        return new StringBuffer()
                .append(box.getMinX())
                .append(",")
                .append(box.getMinY())
                .append(",")
                .append(box.getMaxX())
                .append(",")
                .append(box.getMaxY())
                .toString();
    }

    /** Helper to access protected method to avoid duplicating the layer parsing code. */
    private static class LayerParser extends GetMapKvpRequestReader {

        public LayerParser(WMS wms) {
            super(wms);
        }

        @Override
        protected List<?> parseLayers(
                List<String> requestedLayerNames, URL remoteOwsUrl, String remoteOwsType) {
            try {
                return super.parseLayers(requestedLayerNames, remoteOwsUrl, remoteOwsType);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing layers list", e);
            }
        }
    }
}
