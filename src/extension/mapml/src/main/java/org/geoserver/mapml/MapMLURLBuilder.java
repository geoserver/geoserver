/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.mapml.tcrs.Point;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WMTSAccessLimits;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.ows.OperationType;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wmts.model.TileMatrix;
import org.geotools.ows.wmts.model.TileMatrixSet;
import org.geotools.ows.wmts.model.TileMatrixSetLink;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * This class takes care of setting up URL Template, including preparing cascaded URL when a cascaded Layer is
 * configured to useRemote service, in case the request satisfies criteria to go directly through the remote service.
 */
public class MapMLURLBuilder {

    private static final String WMTS = "WMTS";
    private static final String REQUEST = "request";
    private static final String LAYER = "layer";
    private static final String GETMAP = "GETMAP";
    private static final String GETFEATUREINFO = "GETFEATUREINFO";
    private static final String SERVICE = "service";
    private static final String LAYERS = "layers";
    private static final String CRS_PARAM = "crs";
    private static final String SRS_PARAM = "srs";
    private static final String WMS_1_3_0 = "1.3.0";

    private static final double ORIGIN_DELTA = 0.1;
    private static final double DELTA = 1E-5;

    private static final List<String> GET_FEATURE_INFO_FORMATS = Arrays.asList("text/mapml", "text/html", "text/plain");

    private static final Logger LOGGER = Logging.getLogger(MapMLURLBuilder.class);
    private final MapMLDocumentBuilder.MapMLLayerMetadata mapMLLayerMetadata;
    private final String path;
    private final String baseUrlPattern;
    private final String proj;
    private final HashMap<String, String> params;
    private final WMSMapContent mapContent;
    private ResourceAccessManager resourceAccessManager;

    public MapMLURLBuilder(
            WMSMapContent mapContent,
            MapMLDocumentBuilder.MapMLLayerMetadata mapMLLayerMetadata,
            String baseUrlPattern,
            String path,
            HashMap<String, String> params,
            String proj) {
        this.mapContent = mapContent;
        this.mapMLLayerMetadata = mapMLLayerMetadata;
        this.path = path;
        this.params = params;
        this.baseUrlPattern = baseUrlPattern;
        this.proj = proj;

        SecureCatalogImpl secureCatalog = GeoServerExtensions.bean(SecureCatalogImpl.class);
        this.resourceAccessManager = secureCatalog.getResourceAccessManager();
    }

    public String getUrlTemplate() {
        LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
        String urlTemplate = "";
        try {
            if (!canCascade(layerInfo)) {
                urlTemplate = URLDecoder.decode(
                        ResponseUtils.buildURL(baseUrlPattern, path, params, URLMangler.URLType.SERVICE), "UTF-8");
            } else {
                urlTemplate = generateURL(path, params, layerInfo);
            }
        } catch (UnsupportedEncodingException uee) {
        }
        return urlTemplate;
    }

    /**
     * Check metadata, request Params and layerInfo configuration to verify if there are minimal requirements for a
     * potential cascading to the remote service.
     */
    private boolean canCascade(LayerInfo layerInfo) {
        if (mapMLLayerMetadata.isUseRemote()) {
            if (!MapMLDocumentBuilder.isWMSOrWMTSStore(layerInfo)) return false;
            if (hasRestrictingAccessLimits(layerInfo)) return false;
            if (hasVendorParams()) return false;
            // Not supporting cross-requests yet:
            // GetTiles against remote WMS
            // GetMap against remote WMTS
            return TiledCRSConstants.getSupportedOutputCRS(proj) != null;
        }
        return false;
    }

    /**
     * Try cascading to the remote Server and generate the cascaded URL. If cascading cannot be performed (i.e. CRS not
     * supported on the remote server) a local URL will be generated. If the URL should not be generated at all (i.e.
     * requesting a GetFeatureInfo to a not queryable layer) null will be returned and the document builder won't add
     * the URL (i.e. the query link).
     */
    private String generateURL(String path, HashMap<String, String> params, LayerInfo layerInfo)
            throws UnsupportedEncodingException {
        String baseUrl = baseUrlPattern;
        String version = "1.3.0";
        String reason = null;
        boolean cascadeToRemote = false;
        URLMangler.URLType urlType = URLMangler.URLType.SERVICE;
        List<String> infoFormats = new ArrayList<>();
        List<String> remoteInfoFormats = new ArrayList<>();
        if (layerInfo != null) {
            ResourceInfo resourceInfo = layerInfo.getResource();
            String layerName = resourceInfo.getNativeName();
            boolean isRemoteSupportingFormat = true;
            String outputCRS = TiledCRSConstants.getSupportedOutputCRS(proj);
            boolean isSupportedOutputCRS = outputCRS != null;
            if (resourceInfo != null) {
                String capabilitiesURL = null;
                URL getResourceURL = null;
                String tileMatrixSet = null;
                StoreInfo storeInfo = resourceInfo.getStore();
                String requestedCRS = isSupportedOutputCRS ? outputCRS : proj;
                reason = "RequestedCRS " + requestedCRS + " is not supported by layer: " + layerName;
                if (storeInfo instanceof WMSStoreInfo) {
                    WMSStoreInfo wmsStoreInfo = (WMSStoreInfo) storeInfo;
                    capabilitiesURL = wmsStoreInfo.getCapabilitiesURL();
                    try {
                        WMSCapabilities capabilities =
                                wmsStoreInfo.getWebMapServer(null).getCapabilities();
                        getResourceURL = capabilities.getRequest().getGetMap().getGet();
                        version = capabilities.getVersion();
                        List<Layer> layerList = capabilities.getLayerList();
                        // Check on GetFeatureInfo
                        if (GETFEATUREINFO.equalsIgnoreCase(params.get(REQUEST))) {
                            boolean isQueryable = isQueryable(layerList, layerName);
                            if (isQueryable) {
                                isRemoteSupportingFormat = isRemoteSupportingClientFormats(
                                        capabilities.getRequest().getGetFeatureInfo(), infoFormats, remoteInfoFormats);
                                if (!isRemoteSupportingFormat) {
                                    reason = "Remote Server not supporting the client's infoFormat";
                                }
                            }
                            if (!isQueryable || (!isRemoteSupportingFormat && !isSupportedGML(remoteInfoFormats))) {
                                // remote server is not even supporting GML
                                // so we are not generating featureInfo link at all
                                LOGGER.fine(
                                        "URL won't be generated due to Requesting a not supported GetFeatureInfo format");
                                return null;
                            }
                        }
                        if (!WMS_1_3_0.equals(version)) {
                            version = "1.1.1";
                        }

                        cascadeToRemote = isRemoteSupportingFormat
                                && isSupportedOutputCRS
                                && isSupportedCRS(layerList, layerName, requestedCRS);

                    } catch (IOException e) {
                        reason = "Unable to extract the WMS remote capabilities. Cascading won't be performed";
                        LOGGER.warning(reason + "due to:" + e);
                        cascadeToRemote = false;
                    }
                } else if (storeInfo instanceof WMTSStoreInfo) {
                    WMTSStoreInfo wmtsStoreInfo = (WMTSStoreInfo) storeInfo;
                    capabilitiesURL = wmtsStoreInfo.getCapabilitiesURL();
                    try {
                        WMTSCapabilities capabilities =
                                wmtsStoreInfo.getWebMapTileServer(null).getCapabilities();
                        getResourceURL = capabilities.getRequest().getGetTile().getGet();
                        version = capabilities.getVersion();
                        List<WMTSLayer> layerList = capabilities.getLayerList();
                        // Check on GetFeatureInfo
                        if (GETFEATUREINFO.equalsIgnoreCase(params.get(REQUEST))) {
                            boolean isQueryable = isQueryable(layerList, layerName);
                            if (isQueryable) {
                                isRemoteSupportingFormat = isRemoteSupportingClientFormats(
                                        capabilities.getRequest().getGetFeatureInfo(), infoFormats, remoteInfoFormats);
                                if (!isRemoteSupportingFormat) {
                                    reason = "Remote Server not supporting the client's infoFormat";
                                }
                            }
                            if (!isQueryable || (!isRemoteSupportingFormat && !isSupportedGML(remoteInfoFormats))) {
                                // remote server is not even supporting GML
                                // so we are not generating featureInfo link at all
                                LOGGER.fine(
                                        "URL won't be generated due to Requesting a not supported GetFeatureInfo format");
                                return null;
                            }
                        }
                        tileMatrixSet = getSupportedTileMatrix(layerList, layerName, requestedCRS, capabilities);

                        cascadeToRemote = isRemoteSupportingFormat && isSupportedOutputCRS && tileMatrixSet != null;
                    } catch (IOException | FactoryException e) {
                        reason = "Unable to extract the WMTS remote capabilities. Cascading won't be performed";
                        LOGGER.warning(reason + "due to:" + e);
                        cascadeToRemote = false;
                    }
                }
                if (cascadeToRemote) {
                    // if we reach this point, we can finally cascade.
                    // Let's update all the params for the cascading
                    // getResourceURL may be null if the capabilities doc is misconfigured;
                    if (getResourceURL != null) {
                        baseUrl = getResourceURL.getProtocol()
                                + "://"
                                + getResourceURL.getHost()
                                + (getResourceURL.getPort() == -1 ? "" : ":" + getResourceURL.getPort())
                                + "/";

                        path = getResourceURL.getPath();
                    } else {
                        // if misconfigured capabilites, use cap document URL as base
                        String[] baseUrlAndPath = getBaseUrlAndPath(capabilitiesURL);
                        baseUrl = baseUrlAndPath[0];
                        path = baseUrlAndPath[1];
                    }
                    urlType = URLMangler.URLType.EXTERNAL;
                    updateRequestParams(params, layerName, version, requestedCRS, tileMatrixSet, infoFormats);
                } else {

                    LOGGER.fine("Cascading won't be performed, due to: " + reason);
                }
            }
        }

        String urlTemplate = URLDecoder.decode(ResponseUtils.buildURL(baseUrl, path, params, urlType), "UTF-8");
        return urlTemplate;
    }

    private boolean isQueryable(List<? extends Layer> layerList, String layerName) {
        for (Layer layer : layerList) {
            if (layerName.equals(layer.getName())) {
                return layer.isQueryable();
            }
        }
        return false;
    }

    private boolean isSupportedGML(List<String> remoteInfoFormats) {
        for (String infoFormat : remoteInfoFormats) {
            if (infoFormat.toLowerCase().contains("gml")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRemoteSupportingClientFormats(
            OperationType featureInfo, List<String> infoFormats, List<String> remoteInfoFormats) {
        if (featureInfo != null) {
            List<String> featureInfoFormats = featureInfo.getFormats();
            remoteInfoFormats.addAll(featureInfoFormats);
            infoFormats.addAll(featureInfoFormats);
            infoFormats.retainAll(GET_FEATURE_INFO_FORMATS);
        }
        return !infoFormats.isEmpty();
    }

    private String getSupportedTileMatrix(
            List<WMTSLayer> layerList, String layerName, String requestedCRS, WMTSCapabilities capabilities)
            throws FactoryException {
        // Let's check if the capabilities document has a matching layer
        // supporting a compatible CRS/GridSet
        String tileMatrixSet = null;
        for (WMTSLayer layer : layerList) {
            if (layerName.equals(layer.getName())) {
                tileMatrixSet = getSupportedWMTSGridSet(layer, requestedCRS, capabilities);
                break;
            }
        }
        return tileMatrixSet;
    }

    private void updateCRS(HashMap<String, String> params, String version, String requestedCRS) {
        if (params.containsKey(CRS_PARAM) || params.containsKey(SRS_PARAM)) {
            params.remove(CRS_PARAM);
            params.remove(SRS_PARAM);
            String crsName = WMS_1_3_0.equals(version) ? CRS_PARAM : SRS_PARAM;
            params.put(crsName, requestedCRS);
        }
    }

    private void updateRequestParams(
            HashMap<String, String> params,
            String layerName,
            String version,
            String requestedCRS,
            String tileMatrixSetName,
            List<String> infoFormats) {
        String requestType = params.get(REQUEST);
        String service = params.get(SERVICE);
        if (params.containsKey(LAYER)) {
            params.put(LAYER, layerName);
        } else if (params.containsKey(LAYERS)) {
            params.put(LAYERS, layerName);
        }
        if (GETMAP.equalsIgnoreCase(requestType) || GETFEATUREINFO.equalsIgnoreCase(requestType)) {
            params.put("version", version);
            if (params.containsKey("query_layers")) {
                params.put("query_layers", layerName);
            }
            updateInfoFormat(params, infoFormats);
            updateCRS(params, version, requestedCRS);
        }
        // Extra settings for WMTS
        if (WMTS.equalsIgnoreCase(service)) {
            String[] tileMatrixSetSchema = tileMatrixSetName.split(";");
            tileMatrixSetName = tileMatrixSetSchema[0];
            if (tileMatrixSetSchema.length == 2) {
                params.put("tilematrix", tileMatrixSetSchema[1] + "{z}");
            }
            if (params.containsKey("tilematrixset")) {
                params.put("tilematrixset", tileMatrixSetName);
            }
            params.remove("style");
        }
    }

    private void updateInfoFormat(HashMap<String, String> params, List<String> infoFormats) {
        // When entering this method, infoFormats cannot be empty.

        String paramName = "info_format";
        String infoFormat = params.get(paramName);
        if (infoFormat == null) {
            paramName = "infoformat";
            infoFormat = params.get(paramName);
        }
        if (infoFormat != null) {
            // replace the infoFormat with a supported one
            infoFormat = getInfoFormat(infoFormat, infoFormats);
            params.put(paramName, infoFormat);
        }
    }

    private String getInfoFormat(String infoFormat, List<String> infoFormats) {
        if (!infoFormats.contains(infoFormat)) {
            // Fall back on text/html
            if (infoFormats.contains("text/html") || infoFormats.isEmpty()) {
                infoFormat = "text/html";
            } else {
                infoFormat = infoFormats.get(0);
            }
        }
        return infoFormat;
    }

    @SuppressWarnings("PMD.UseCollectionIsEmpty")
    private boolean hasVendorParams() {
        GetMapRequest req = mapContent.getRequest();
        Map<String, String> kvpMap = req.getRawKvp();

        // The following vendor params have been retrieved from the WMSRequests class.
        // format options
        Map<String, Object> formatOptions = req.getFormatOptions();
        if (formatOptions != null
                && formatOptions.size() >= 1
                && !formatOptions.containsKey(MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION.toUpperCase())) {
            return true;
        }

        // view params
        if (req.getViewParams() != null && !req.getViewParams().isEmpty()) {
            return true;
        }
        if (req.getEnv() != null && !req.getEnv().isEmpty()) {
            return true;
        }

        if (req.getMaxFeatures() != null
                || req.getRemoteOwsType() != null
                || req.getRemoteOwsURL() != null
                || req.getScaleMethod() != null
                || req.getStartIndex() != null) {
            return true;
        }

        if (!req.getStyleFormat().equals(SLDHandler.FORMAT)) {
            return true;
        }
        if (req.getStyleVersion() != null) {
            return true;
        }

        // Boolean params
        if (req.isTiled() || Boolean.TRUE.equals(req.getValidateSchema())) {
            return true;
        }

        if (hasProperty(kvpMap, "propertyName", "bgcolor", "tilesOrigin", "palette", "interpolations", "clip")) {
            return true;
        }

        // numeric params
        if (req.getBuffer() > 0 || Double.compare(req.getAngle(), 0.0) != 0) {
            return true;
        }
        if (req.getCQLFilter() != null && !req.getCQLFilter().isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean hasProperty(Map<String, String> kvpMap, String... properties) {
        for (String property : properties) {
            String prop = kvpMap.get(property);
            if (StringUtils.hasText(prop)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRestrictingAccessLimits(LayerInfo layerInfo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        DataAccessLimits accessLimits = resourceAccessManager.getAccessLimits(auth, layerInfo);

        // If there is any access limits effectively affecting the layer
        // we are not going to cascade so that the vendor param can be
        // honored by the local GeoServer
        if (accessLimits != null) {
            Filter readFilter = accessLimits.getReadFilter();
            if (readFilter != null && readFilter != Filter.INCLUDE) {
                return true;
            }
            Geometry geom = null;
            if (accessLimits instanceof WMSAccessLimits) {
                WMSAccessLimits limits = (WMSAccessLimits) accessLimits;
                geom = limits.getRasterFilter();
            }
            if (accessLimits instanceof WMTSAccessLimits) {
                WMTSAccessLimits limits = (WMTSAccessLimits) accessLimits;
                geom = limits.getRasterFilter();
            }
            if (accessLimits instanceof VectorAccessLimits) {
                VectorAccessLimits limits = (VectorAccessLimits) accessLimits;
                geom = limits.getClipVectorFilter();
            }
            if (accessLimits instanceof CoverageAccessLimits) {
                CoverageAccessLimits limits = (CoverageAccessLimits) accessLimits;
                geom = limits.getRasterFilter();
            }
            if (geom != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupportedCRS(List<Layer> layerList, String layerName, String crs) {
        for (Layer layer : layerList) {
            if (layerName.equals(layer.getName())) {
                if (isSRSInLayerOrParents(layer, crs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSRSInLayerOrParents(Layer layer, String srs) {
        // Check if the current layer supports the SRS
        if (isLayerSupportingSRS(layer, srs)) {
            return true;
        }

        // If not, check the parent layers recursively
        Layer parentLayer = layer.getParent();
        while (parentLayer != null) {
            if (isLayerSupportingSRS(parentLayer, srs)) {
                return true;
            }
            parentLayer = parentLayer.getParent();
        }

        // Return false if no layer supports the SRS
        return false;
    }

    private boolean isLayerSupportingSRS(Layer layer, String srs) {
        Set<String> supportedSRS = layer.getSrs();
        return supportedSRS != null && supportedSRS.contains(srs);
    }

    private String getSupportedWMTSGridSet(WMTSLayer layer, String srs, WMTSCapabilities capabilities)
            throws FactoryException {
        TiledCRSParams inputCrs = TiledCRSConstants.lookupTCRSParams(srs);
        if (inputCrs == null) {
            return null;
        }
        Map<String, TileMatrixSetLink> tileMatrixLinks = layer.getTileMatrixLinks();
        Collection<TileMatrixSetLink> values = tileMatrixLinks.values();
        for (TileMatrixSetLink value : values) {
            String tileMatrixSetName = value.getIdentifier();
            TileMatrixSet tileMatrixSet = capabilities.getMatrixSet(tileMatrixSetName);

            // First check: same CRS
            // Simpler name equality may not work (i.e. urn:ogc:def:crs:EPSG::3857 vs
            // urn:x-ogc:def:crs:EPSG:3857)
            if (!CRS.isEquivalent(CRS.decode(inputCrs.getCode()), CRS.decode(tileMatrixSet.getCrs()))) {
                continue;
            }

            List<TileMatrix> tileMatrices = tileMatrixSet.getMatrices();
            double[] tiledCRSResolutions = inputCrs.getResolutions();

            // check compatible levels
            if (tileMatrices.size() < tiledCRSResolutions.length) {
                continue;
            }
            TileMatrix level0 = tileMatrices.get(0);
            int tiledCRStileSize = inputCrs.getTILE_SIZE();
            if (tiledCRStileSize != level0.getTileHeight() || tiledCRStileSize != level0.getTileWidth()) {
                continue;
            }

            // check same origin
            org.locationtech.jts.geom.Point origin = level0.getTopLeft();
            Point tCRSorigin = inputCrs.getOrigin();

            double deltaCoordinate = tileMatrices.get(tileMatrices.size() - 1).getResolution() * ORIGIN_DELTA;
            if (Math.abs(tCRSorigin.x - origin.getX()) > deltaCoordinate
                    || Math.abs(tCRSorigin.y - origin.getY()) > deltaCoordinate) {
                continue;
            }

            // check same resolutions
            boolean sameResolutions = true;
            for (int i = 0; i < tiledCRSResolutions.length; i++) {
                if (Math.abs(tileMatrices.get(i).getResolution() - tiledCRSResolutions[i]) > DELTA) {
                    sameResolutions = false;
                    break;
                }
            }
            if (!sameResolutions) {
                continue;
            }
            TiledCRSConstants.GridSetLevelType levelType =
                    TiledCRSConstants.getLevelType(MapMLGridsets.getLevelNamesFromTileMatrixList(tileMatrices));
            if (levelType.isPrefixed()) {
                tileMatrixSetName += (";" + levelType.getPrefix());
            }
            return tileMatrixSetName;
        }
        return null;
    }

    private String[] getBaseUrlAndPath(String capabilitiesURL) {
        try {
            URL url = new URL(capabilitiesURL);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            String path = "";
            String baseURL = protocol + "://" + host;
            if (port != -1) {
                baseURL += ":" + port;
            }
            // Optionally, add the context path if needed
            String urlPath = url.getPath();
            int contextPathEnd = urlPath.lastIndexOf("/");
            if (contextPathEnd != -1) {
                baseURL += urlPath.substring(0, contextPathEnd);
                path = urlPath.substring(contextPathEnd + 1, urlPath.length());
            }

            return new String[] {baseURL, path};
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
