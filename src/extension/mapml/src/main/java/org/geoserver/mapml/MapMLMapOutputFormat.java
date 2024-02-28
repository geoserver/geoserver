/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_FEATURE_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_ATTRIBUTES_FO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.StyleQueryUtil;
import org.geotools.api.data.Query;

/** Handles a GetMap request that for a map in MapML format. */
public class MapMLMapOutputFormat implements GetMapOutputFormat {
    private final MapMLEncoder encoder;
    private WMS wms;
    private GeoServer geoServer;
    private final Set<String> OUTPUT_FORMATS =
            Collections.unmodifiableSet(new HashSet<>(List.of(MapMLConstants.MAPML_MIME_TYPE)));
    static final MapProducerCapabilities MAPML_CAPABILITIES =
            new MapProducerCapabilities(false, true, true);

    /**
     * Constructor
     *
     * @param wms the WMS
     */
    public MapMLMapOutputFormat(WMS wms, GeoServer geoServer, MapMLEncoder encoder) {
        this.wms = wms;
        this.geoServer = geoServer;
        this.encoder = encoder;
    }

    /**
     * Produce a MapML map in WebMap format
     *
     * @param mapContent the WMS map content
     * @return a MapML map
     * @throws ServiceException If an error occurs while producing the map
     * @throws IOException If an error occurs while producing the map
     */
    @Override
    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        Request request = Dispatcher.REQUEST.get();
        Mapml mapMLDocument;
        if (isFeaturesRequest(request)) {
            if (mapContent.layers() != null && mapContent.layers().size() > 1) {
                throw new ServiceException(
                        "MapML WMS Feature format does not currently support Multiple Feature Type output.");
            }
            if (!mapContent.getRequest().getLayers().isEmpty()
                    && MapLayerInfo.TYPE_VECTOR
                            != mapContent.getRequest().getLayers().get(0).getType()) {
                throw new ServiceException(
                        "MapML WMS Feature format does not currently support non-vector layers.");
            }
            List<Query> queries = StyleQueryUtil.getStyleQuery(mapContent.layers(), mapContent);
            Query query = null;
            if (queries != null && !queries.isEmpty()) {
                if (queries.size() > 1) {
                    throw new ServiceException(
                            "MapML WMS Feature format does not currently support Multiple Feature Type output.");
                }
                query = queries.get(0);
            }
            MapMLFeaturesBuilder builder = new MapMLFeaturesBuilder(mapContent, geoServer, query);
            builder.setSkipAttributes(isSkipAttributes(request));
            mapMLDocument = builder.getMapMLDocument();
        } else {
            MapMLDocumentBuilder mapMLDocumentBuilder =
                    new MapMLDocumentBuilder(mapContent, wms, geoServer, request.getHttpRequest());
            mapMLDocument = mapMLDocumentBuilder.getMapMLDocument();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encoder.encode(mapMLDocument, bos);
        return new RawMap(mapContent, bos, MapMLConstants.MAPML_MIME_TYPE);
    }

    /** Checks if the request should dump features instead of HTML */
    private static boolean isFeaturesRequest(Request request) {
        // case 1: the format_options parameter is set to include the mapml feature format
        if (getBoleanFormatOption(request, MAPML_FEATURE_FO)) return true;

        // case 2: it's a GWC tile seeding request, can only be a features request
        return isGWCTiledRequest(request);
    }

    private static boolean isSkipAttributes(Request request) {
        // case 1: the format_options parameter is set to include the mapml feature format
        if (getBoleanFormatOption(request, MAPML_SKIP_ATTRIBUTES_FO)) return true;

        // case 2: it's a GWC tile seeding request, we want to skip attributes as well
        return isGWCTiledRequest(request);
    }

    private static boolean isGWCTiledRequest(Request request) {
        return "true".equals(request.getRawKvp().get(GeoServerTileLayer.GWC_SEED_INTERCEPT_TOKEN));
    }

    /** Tests if the specified format option is present and evaluates to true */
    private static boolean getBoleanFormatOption(Request request, String key) {
        Map formatOptions = (Map) request.getKvp().get("format_options");
        if (formatOptions != null && Boolean.valueOf((String) formatOptions.get(key))) return true;
        return false;
    }

    @Override
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MapMLConstants.MAPML_MIME_TYPE;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return MAPML_CAPABILITIES;
    }
}
