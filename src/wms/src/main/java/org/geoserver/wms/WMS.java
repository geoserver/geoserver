/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A facade providing access to the WMS configuration details
 * 
 * @author Gabriel Roldan
 */
public class WMS implements ApplicationContextAware {

    public static final Version VERSION_1_1_1 = new Version("1.1.1");

    public static final Version VERSION_1_3_0 = new Version("1.3.0");

    public static final String JPEG_COMPRESSION = "jpegCompression";

    public static final int JPEG_COMPRESSION_DEFAULT = 25;

    public static final String PNG_COMPRESSION = "pngCompression";

    public static final int PNG_COMPRESSION_DEFAULT = 25;

    static final Logger LOGGER = Logging.getLogger(WMS.class);

    public static final String WEB_CONTAINER_KEY = "WMS";

    /**
     * SVG renderers.
     */
    public static final String SVG_SIMPLE = "Simple";

    public static final String SVG_BATIK = "Batik";

    /**
     * KML reflector mode
     */
    public static String KML_REFLECTOR_MODE = "kmlReflectorMode";

    /**
     * KML reflector mode values
     */
    public static final String KML_REFLECTOR_MODE_REFRESH = "refresh";

    public static final String KML_REFLECTOR_MODE_SUPEROVERLAY = "superoverlay";

    public static final String KML_REFLECTOR_MODE_DOWNLOAD = "download";

    public static final String KML_REFLECTOR_MODE_DEFAULT = KML_REFLECTOR_MODE_REFRESH;

    /**
     * KML superoverlay sub-mode
     */
    public static final String KML_SUPEROVERLAY_MODE = "kmlSuperoverlayMode";

    public static final String KML_SUPEROVERLAY_MODE_AUTO = "auto";

    public static final String KML_SUPEROVERLAY_MODE_RASTER = "raster";

    public static final String KML_SUPEROVERLAY_MODE_OVERVIEW = "overview";

    public static final String KML_SUPEROVERLAY_MODE_HYBRID = "hybrid";

    public static final String KML_SUPEROVERLAY_MODE_CACHED = "cached";

    public static final String KML_SUPEROVERLAY_MODE_DEFAULT = KML_SUPEROVERLAY_MODE_AUTO;

    public static final String KML_KMLATTR = "kmlAttr";

    public static final boolean KML_KMLATTR_DEFAULT = true;

    public static final String KML_KMLPLACEMARK = "kmlPlacemark";

    public static final boolean KML_KMLPLACEMARK_DEFAULT = false;

    public static final String KML_KMSCORE = "kmlKmscore";

    public static final int KML_KMSCORE_DEFAULT = 40;

    private final GeoServer geoserver;

    private ApplicationContext applicationContext;

    public WMS(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    private Catalog getCatalog() {
        return geoserver.getCatalog();
    }

    public WMSInfo getServiceInfo() {
        return geoserver.getService(WMSInfo.class);
    }

    public Style getStyleByName(String styleName) throws IOException {
        StyleInfo styleInfo = getCatalog().getStyleByName(styleName);
        return styleInfo == null ? null : styleInfo.getStyle();
    }

    public LayerInfo getLayerByName(String layerName) {
        return getCatalog().getLayerByName(layerName);
    }

    public LayerGroupInfo getLayerGroupByName(String layerGroupName) {
        return getCatalog().getLayerGroupByName(layerGroupName);
    }

    public boolean isEnabled() {
        WMSInfo serviceInfo = getServiceInfo();
        return serviceInfo.isEnabled();
    }

    /**
     * /**
     * Returns a supported version according to the version negotiation rules in section 6.2.4 of
     * the WMS 1.3.0 spec.
     * <p>
     * Calls through to {@link #negotiateVersion(Version)}. 
     * </p>
     * @param requestedVersion The version, may be bull.
     * 
     */
    public static Version negotiateVersion(final String requestedVersion) {
        return negotiateVersion(requestedVersion != null ? new Version(requestedVersion) : null);
    }
    
    /**
     * Returns a supported version according to the version negotiation rules in section 6.2.4 of
     * the WMS 1.3.0 spec.
     * <p>
     * For instance: <u>
     * <li>request version not provided? -> higher version supported
     * <li>requested version supported? -> that same version
     * <li>requested version < lowest supported version? -> lowest supported
     * <li>requested version > lowest supported version? -> higher supported version that's lower
     * than the requested version </u>
     * </p>
     * 
     * @param requestedVersion
     *            the request version, or {@code null} if unspecified
     * @return
     */
    public static Version negotiateVersion(final Version requestedVersion) {
        if (null == requestedVersion) {
            return VERSION_1_3_0;
        }
        if (VERSION_1_1_1.equals(requestedVersion)) {
            return VERSION_1_1_1;
        }
        if (VERSION_1_3_0.equals(requestedVersion)) {
            return VERSION_1_3_0;
        }
        if (requestedVersion.compareTo(VERSION_1_3_0) < 0) {
            return VERSION_1_1_1;
        }

        return VERSION_1_3_0;
    }

    public String getVersion() {
        WMSInfo serviceInfo = getServiceInfo();
        List<Version> versions = serviceInfo.getVersions();
        String version;
        if (versions.size() > 0) {
            version = versions.get(0).toString();
        } else {
            // shouldn't a version be set?
            version = "1.1.1";
        }
        return version;
    }

    public GeoServer getGeoServer() {
        return this.geoserver;
    }

    public WMSInterpolation getInterpolation() {
        return getServiceInfo().getInterpolation();
    }

    public Boolean getPNGNativeAcceleration() {
        JAIInfo jaiInfo = getJaiInfo();
        return Boolean.valueOf(jaiInfo.isPngAcceleration());
    }

    public Boolean getJPEGNativeAcceleration() {
        JAIInfo jaiInfo = getJaiInfo();
        return Boolean.valueOf(jaiInfo.isJpegAcceleration());
    }

    private JAIInfo getJaiInfo() {
        GeoServer geoServer = getGeoServer();
        GeoServerInfo global = geoServer.getGlobal();
        return global.getJAI();
    }

    public Charset getCharSet() {
        GeoServer geoServer2 = getGeoServer();
        GeoServerInfo global = geoServer2.getGlobal();
        String charset = global.getCharset();
        return Charset.forName(charset);
    }

    public String getProxyBaseUrl() {
        GeoServer geoServer = getGeoServer();
        GeoServerInfo global = geoServer.getGlobal();
        String proxyBaseUrl = global.getProxyBaseUrl();
        return proxyBaseUrl;
    }

    public long getUpdateSequence() {
        GeoServerInfo global = getGeoServer().getGlobal();
        return global.getUpdateSequence();
    }

    public int getWatermarkTransparency() {
        WatermarkInfo watermark = getServiceInfo().getWatermark();
        return watermark.getTransparency();
    }

    public int getWatermarkPosition() {
        WatermarkInfo watermark = getServiceInfo().getWatermark();
        Position position = watermark.getPosition();
        return position.getCode();
    }

    public boolean isGlobalWatermarking() {
        WatermarkInfo watermark = getServiceInfo().getWatermark();
        return watermark.isEnabled();
    }

    public String getGlobalWatermarkingURL() {
        WatermarkInfo watermark = getServiceInfo().getWatermark();
        return watermark.getURL();
    }

    public FeatureTypeInfo getFeatureTypeInfo(final Name name) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo resource = catalog.getResourceByName(name, FeatureTypeInfo.class);
        return resource;
    }

    public CoverageInfo getCoverageInfo(final Name name) {
        Catalog catalog = getCatalog();
        CoverageInfo resource = catalog.getResourceByName(name, CoverageInfo.class);
        return resource;
    }    

    public WMSLayerInfo getWMSLayerInfo(final Name name) {
        Catalog catalog = getCatalog();
        WMSLayerInfo resource = catalog.getResourceByName(name, WMSLayerInfo.class);
        return resource;
    }
    
    public ResourceInfo getResourceInfo(final Name name) {
        Catalog catalog = getCatalog();
        ResourceInfo resource = catalog.getResourceByName(name, ResourceInfo.class);
        return resource;
    }

    public List<LayerInfo> getLayers() {
        Catalog catalog = getCatalog();
        return catalog.getLayers();
    }

    public String getNamespaceByPrefix(final String prefix) {
        Catalog catalog = getCatalog();
        NamespaceInfo namespaceInfo = catalog.getNamespaceByPrefix(prefix);
        return namespaceInfo == null ? null : namespaceInfo.getURI();
    }

    public List<LayerGroupInfo> getLayerGroups() {
        Catalog catalog = getCatalog();
        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
        return layerGroups;
    }

    /**
     * Informs the user that this WMS supports SLD. We don't currently handle sld, still needs to be
     * rolled in from geotools, so this now must be false.
     * 
     * //djb: we support it now
     * 
     * @return false
     */
    public boolean supportsSLD() {
        return true; // djb: we support it now
    }

    /**
     * Informs the user that this WMS supports User Layers
     * <p>
     * We support this both remote wfs and inlineFeature
     * </p>
     * 
     * @return true
     */
    public boolean supportsUserLayer() {
        return true;
    }

    /**
     * Informs the user that this WMS supports User Styles
     * 
     * @return true
     */
    public boolean supportsUserStyle() {
        return true;
    }

    /**
     * Informs the user that this WMS supports Remote WFS.
     * 
     * @return true
     */
    public boolean supportsRemoteWFS() {
        return true;
    }

    public void setSvgRenderer(String svgRendererHint) {
        WMSInfo serviceInfo = getServiceInfo();
        serviceInfo.getMetadata().put("svgRenderer", svgRendererHint);
        getGeoServer().save(serviceInfo);
    }

    public String getSvgRenderer() {
        WMSInfo serviceInfo = getServiceInfo();
        String svgRendererHint = (String) serviceInfo.getMetadata().get("svgRenderer");
        return svgRendererHint;
    }

    public boolean isSvgAntiAlias() {
        WMSInfo serviceInfo = getServiceInfo();
        Boolean svgAntiAlias = Converters.convert(serviceInfo.getMetadata().get("svgAntiAlias"),
                Boolean.class);
        return svgAntiAlias == null ? true : svgAntiAlias.booleanValue();
    }

    public int getPngCompression() {
        WMSInfo serviceInfo = getServiceInfo();
        return getMetadataPercentage(serviceInfo.getMetadata(), PNG_COMPRESSION,
                PNG_COMPRESSION_DEFAULT);
    }

    public int getJpegCompression() {
        WMSInfo serviceInfo = getServiceInfo();
        return getMetadataPercentage(serviceInfo.getMetadata(), JPEG_COMPRESSION,
                JPEG_COMPRESSION_DEFAULT);
    }

    int getMetadataPercentage(MetadataMap metadata, String key, int defaultValue) {
        Integer parsedValue = Converters.convert(metadata.get(key), Integer.class);
        if (parsedValue == null)
            return defaultValue;
        int value = parsedValue.intValue();
        if (value < 0 || value > 100) {
            LOGGER.warning("Invalid percertage value for '" + key
                    + "', it should be between 0 and 100");
            return defaultValue;
        }

        return value;
    }

    public int getNumDecimals() {
        GeoServerInfo global = getGeoServer().getGlobal();
        return global.getNumDecimals();
    }

    public String getNameSpacePrefix(final String nsUri) {
        Catalog catalog = getCatalog();
        NamespaceInfo ns = catalog.getNamespaceByURI(nsUri);
        return ns == null ? null : ns.getPrefix();
    }

    public int getMaxBuffer() {
        return getServiceInfo().getMaxBuffer();
    }

    public int getMaxRequestMemory() {
        return getServiceInfo().getMaxRequestMemory();
    }

    public int getMaxRenderingTime() {
        return getServiceInfo().getMaxRenderingTime();
    }

    public int getMaxRenderingErrors() {
        return getServiceInfo().getMaxRenderingErrors();
    }

    public String getKmlReflectorMode() {
        String value = (String) getServiceInfo().getMetadata().get(KML_REFLECTOR_MODE);
        return value != null ? value : KML_REFLECTOR_MODE_DEFAULT;
    }

    public String getKmlSuperoverlayMode() {
        String value = (String) getServiceInfo().getMetadata().get(KML_SUPEROVERLAY_MODE);
        return value != null ? value : KML_SUPEROVERLAY_MODE_DEFAULT;
    }

    public boolean getKmlKmAttr() {
        Boolean kmAttr = Converters.convert(getServiceInfo().getMetadata().get(KML_KMLATTR),
                Boolean.class);
        return kmAttr == null ? KML_KMLATTR_DEFAULT : kmAttr.booleanValue();
    }

    public boolean getKmlPlacemark() {
        Boolean kmAttr = Converters.convert(getServiceInfo().getMetadata().get(KML_KMLPLACEMARK),
                Boolean.class);
        return kmAttr == null ? KML_KMLPLACEMARK_DEFAULT : kmAttr.booleanValue();
    }

    public int getKmScore() {
        return getMetadataPercentage(getServiceInfo().getMetadata(), KML_KMSCORE,
                KML_KMSCORE_DEFAULT);
    }

    /**
     * Returns all available map output formats. 
     */
    public Collection<GetMapOutputFormat> getAvailableMapFormats() {
        return WMSExtensions.findMapProducers(applicationContext);
    }
    
    /**
     * Grabs the list of available MIME-Types for the GetMap operation from the set of
     * {@link GetMapOutputFormat}s registered in the application context.
     * 
     * @param applicationContext
     *            The application context where to grab the GetMapOutputFormats from.
     * @see GetMapOutputFormat#getContentType()
     */
    public Set<String> getAvailableMapFormatNames() {

        final Collection<GetMapOutputFormat> producers;
        producers = WMSExtensions.findMapProducers(applicationContext);
        final Set<String> formats = new HashSet<String>();

        for (GetMapOutputFormat producer : producers) {
            formats.addAll(producer.getOutputFormatNames());
        }

        return formats;

    }

    public Set<String> getAvailableLegendGraphicsFormats() {

        List<GetLegendGraphicOutputFormat> formats;
        formats = WMSExtensions.findLegendGraphicFormats(applicationContext);

        Set<String> mimeTypes = new HashSet<String>();
        for (GetLegendGraphicOutputFormat format : formats) {
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
    }

    /**
     * Returns all {@link ExtendedCapabilitiesProvider} extensions.
     */
    public List<ExtendedCapabilitiesProvider> getAvailableExtendedCapabilitiesProviders() {
        return WMSExtensions.findExtendedCapabilitiesProviders(applicationContext);
    }
    
    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @param requestFormat
     * @return a {@link GetFeatureInfoOutputFormat} that can handle the requested mime type or
     *         {@code null} if none if found
     */
    public GetFeatureInfoOutputFormat getFeatureInfoOutputFormat(String requestFormat) {
        List<GetFeatureInfoOutputFormat> outputFormats;
        outputFormats = WMSExtensions.findFeatureInfoFormats(applicationContext);

        for (GetFeatureInfoOutputFormat format : outputFormats) {
            if (format.canProduce(requestFormat)) {
                return format;
            }
        }
        return null;
    }

    public List<String> getAvailableFeatureInfoFormats() {
        List<GetFeatureInfoOutputFormat> outputFormats;
        outputFormats = WMSExtensions.findFeatureInfoFormats(applicationContext);
        List<String> mimeTypes = new ArrayList<String>(outputFormats.size());
        for (GetFeatureInfoOutputFormat format : outputFormats) {
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
    }

    /**
     * @param mimeType
     *            the mime type to look a GetMapOutputFormat for
     * @return the GetMapOutputFormat that can handle {@code mimeType}, or {@code null} if none is
     *         found
     */
    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
        GetMapOutputFormat outputFormat;
        outputFormat = WMSExtensions.findMapProducer(mimeType, applicationContext);
        return outputFormat;
    }

    /**
     * 
     * @param outputFormat
     *            desired output format mime type
     * @return the GetLegendGraphicOutputFormat that can handle {@code mimeType}, or {@code null} if
     *         none is found
     */
    public GetLegendGraphicOutputFormat getLegendGraphicOutputFormat(final String outputFormat) {
        GetLegendGraphicOutputFormat format;
        format = WMSExtensions.findLegendGraphicFormat(outputFormat, applicationContext);
        return format;
    }

    /**
     * Returns a version object for the specified version string.
     * <p>
     * Calls through to {@link #version(String, boolean)} with exact set to <code>false</false>.
     * </p>
     */
    public static Version version(String version) {
        return version(version, false);
    }

    /**
     * Returns a version object for the specified version string optionally returning null
     * when the version string does not match one of the available WMS versions.
     * 
     * @param version The version string.
     * @param exact If set to false, a version object will always be returned. If set to true 
     *   only a version matching on of the available wms versions will be returned.
     * @return
     */
    public static Version version(String version, boolean exact) {
        if (version == null || 0 == version.trim().length()) {
            return null;
        }
        if (VERSION_1_1_1.toString().equals(version)) {
            return VERSION_1_1_1;
        } else if (VERSION_1_3_0.toString().equals(version)) {
            return VERSION_1_3_0;
        }

        return exact ? null : new Version(version);
    }

    /**
     * Transforms a crs identifier to its internal representation based on the specified 
     * WMS version.
     * <p>
     * In version 1.3 of WMS geographic coordinate systems are to be ordered y/x or 
     * latitude/longitude. The only possible way to represent this internally is to use the 
     * explicit epsg namespace "urn:x-ogc:def:crs:EPSG:". This method essentially replaces the 
     * traditional "EPSG:" namespace with the explicit. 
     * </p>
     */
    public static String toInternalSRS(String srs, Version version) {
        if (VERSION_1_3_0.equals(version)) {
            if (srs != null && srs.toUpperCase().startsWith("EPSG:")) {
                srs = srs.toUpperCase().replace("EPSG:", "urn:x-ogc:def:crs:EPSG:");
            }
        }
        
        return srs;
    }

    /**
     * Returns true if the layer can be queried
     */
    public boolean isQueryable(LayerInfo layer) {
        try {
            if (layer.getResource() instanceof WMSLayerInfo) {
                WMSLayerInfo info = (WMSLayerInfo) layer.getResource();
                Layer wl = info.getWMSLayer(null);
                if (!wl.isQueryable()) {
                    return false;
                }
                WMSCapabilities caps = info.getStore().getWebMapServer(null).getCapabilities();
                if (!caps.getRequest().getGetFeatureInfo().getFormats()
                        .contains("application/vnd.ogc.gml")) {
                    return false;
                }
            }

            return layer.isQueryable();
            
        } catch (IOException e) {
            LOGGER.log(Level.INFO,
                    "Failed to determin if the layer is queryable, assuming it's not", e);
            return false;
        }
    }

    public boolean isQueryable(LayerGroupInfo layerGroup) {
        for (LayerInfo layer : layerGroup.getLayers()) {
            if (!isQueryable(layer)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns the read parameters for the specified layer, merging some well known request
     * parameters into the read parameters if possible 
     * @param request
     * @param mapLayerInfo
     * @param layerFilter
     * @param reader
     * @return
     */
    public GeneralParameterValue[] getWMSReadParameters(final GetMapRequest request,
            final MapLayerInfo mapLayerInfo, final Filter layerFilter,
            final AbstractGridCoverage2DReader reader, boolean readGeom) {
        final ParameterValueGroup readParametersDescriptor = reader.getFormat()
                .getReadParameters();
        GeneralParameterValue[] readParameters = CoverageUtils.getParameters(
                readParametersDescriptor, mapLayerInfo.getCoverage().getParameters(), readGeom);

        /*
         * Pass down a few well known parameters to the grid reader: TIME, ELEVEATION, FILTER
         */
        final List dateTime = request.getTime();
        final boolean hasTime = dateTime != null && dateTime.size() > 0;
        final List<GeneralParameterDescriptor> parameterDescriptors = readParametersDescriptor
                .getDescriptor().descriptors();
        if (hasTime) {
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, 
                    readParameters, request.getTime(), "TIME", "Time");
        }

        final double elevationValue = request.getElevation();
        final boolean hasElevation = !Double.isNaN(elevationValue);
        if (hasElevation) {
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, 
                    readParameters, request.getElevation(), "ELEVATION", "Elevation");
        }
        
        if(layerFilter != null) {
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, 
                    readParameters, layerFilter, "FILTER", "Filter");
        }
        return readParameters;
    }

}
