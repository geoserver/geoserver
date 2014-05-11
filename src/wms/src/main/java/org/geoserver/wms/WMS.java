/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.Filters;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vividsolutions.jts.geom.Coordinate;

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

    public static final String MAX_ALLOWED_FRAMES = "maxAllowedFrames";

    public static final int MAX_ALLOWED_FRAMES_DEFAULT = Integer.MAX_VALUE;
    
    public static final String MAX_RENDERING_TIME = "maxAnimatorRenderingTime";
    
    public static final String MAX_RENDERING_SIZE = "maxRenderingSize";
    
    public static final String FRAMES_DELAY = "framesDelay";

    public static final int FRAMES_DELAY_DEFAULT = 1000;
     
    public static final String LOOP_CONTINUOUSLY = "loopContinuously";

    public static final Boolean LOOP_CONTINUOUSLY_DEFAULT = Boolean.FALSE;
    
    public static final String SCALEHINT_MAPUNITS_PIXEL = "scalehintMapunitsPixel";
    
    public static final Boolean SCALEHINT_MAPUNITS_PIXEL_DEFAULT = Boolean.FALSE;
    
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
    
    /**
     * the WMS Animator animatorExecutor service
     */
    private ExecutorService animatorExecutorService;
    
    private static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    private final GeoServer geoserver;

    private ApplicationContext applicationContext;

    public WMS(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    public Catalog getCatalog() {
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
     * /** Returns a supported version according to the version negotiation rules in section 6.2.4
     * of the WMS 1.3.0 spec.
     * <p>
     * Calls through to {@link #negotiateVersion(Version)}.
     * </p>
     * 
     * @param requestedVersion
     *            The version, may be bull.
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

    /**
     * @param animatorExecutorService the animatorExecutorService to set
     */
    public void setAnimatorExecutorService(ExecutorService animatorExecutorService) {
        this.animatorExecutorService = animatorExecutorService;
    }

    /**
     * @return the animatorExecutorService
     */
    public ExecutorService getAnimatorExecutorService() {
        return animatorExecutorService;
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
        String charset = geoServer2.getSettings().getCharset();
        return Charset.forName(charset);
    }

    public String getProxyBaseUrl() {
        GeoServer geoServer = getGeoServer();
        return geoServer.getSettings().getProxyBaseUrl();
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

    public int getMaxAllowedFrames() {
    	return getMetadataValue(MAX_ALLOWED_FRAMES, MAX_ALLOWED_FRAMES_DEFAULT, Integer.class);
    }
    
    public Long getMaxAnimatorRenderingTime() {
        return getMetadataValue(MAX_RENDERING_TIME, null, Long.class);
    }
    
    public Long getMaxRenderingSize() {
        return getMetadataValue( MAX_RENDERING_SIZE, null, Long.class);
    }

    public Integer getFramesDelay() {
        return getMetadataValue(FRAMES_DELAY, FRAMES_DELAY_DEFAULT, Integer.class);
    }
    
    public Boolean getLoopContinuously() {
       return getMetadataValue(LOOP_CONTINUOUSLY, LOOP_CONTINUOUSLY_DEFAULT, Boolean.class);
    }
    
    public Boolean getScalehintUnitPixel(){
        return getMetadataValue(SCALEHINT_MAPUNITS_PIXEL, SCALEHINT_MAPUNITS_PIXEL_DEFAULT, Boolean.class);
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

    <T> T getMetadataValue(String key, T defaultValue, Class<T> clazz) {
        if (getServiceInfo() == null) {
            return defaultValue;
        }

        MetadataMap metadata = getServiceInfo().getMetadata();

    	T parsedValue =  Converters.convert(metadata.get(key), clazz);
    	if (parsedValue == null)
            return defaultValue;
    	
    	return parsedValue;
    }
    
    public int getNumDecimals() {
        return getGeoServer().getSettings().getNumDecimals();
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
     * Returns a version object for the specified version string optionally returning null when the
     * version string does not match one of the available WMS versions.
     * 
     * @param version
     *            The version string.
     * @param exact
     *            If set to false, a version object will always be returned. If set to true only a
     *            version matching on of the available wms versions will be returned.
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
     * Transforms a crs identifier to its internal representation based on the specified WMS
     * version.
     * <p>
     * In version 1.3 of WMS geographic coordinate systems are to be ordered y/x or
     * latitude/longitude. The only possible way to represent this internally is to use the explicit
     * epsg namespace "urn:x-ogc:def:crs:EPSG:". This method essentially replaces the traditional
     * "EPSG:" namespace with the explicit.
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

    public Integer getCascadedHopCount(LayerInfo layer) {
        if (!(layer.getResource() instanceof WMSLayerInfo)) {
            return null;
        }
        WMSLayerInfo wmsLayerInfo = (WMSLayerInfo) layer.getResource();
        Layer wmsLayer;
        int cascaded = 1;
        try {
            wmsLayer = wmsLayerInfo.getWMSLayer(null);
            cascaded = 1 + wmsLayer.getCascaded();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Unable to determina WMSLayer cascaded hop count", e);
        }
        return cascaded;
    }

    public boolean isQueryable(LayerGroupInfo layerGroup) {
        for (PublishedInfo published : layerGroup.getLayers()) {
            if (published instanceof LayerInfo) {
                if (!isQueryable((LayerInfo) published)) {
                    return false;
                }
            } else {
                if (!isQueryable((LayerGroupInfo) published)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the read parameters for the specified layer, merging some well known request
     * parameters into the read parameters if possible
     * 
     * @param request
     * @param mapLayerInfo
     * @param layerFilter
     * @param reader
     * @return
     */
    public GeneralParameterValue[] getWMSReadParameters(final GetMapRequest request,
            final MapLayerInfo mapLayerInfo, final Filter layerFilter, final List<Object> times,
            final List<Object> elevations, final GridCoverage2DReader reader,
            boolean readGeom) throws IOException {
        // setup the scene
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        CoverageInfo coverage = mapLayerInfo.getCoverage();
        MetadataMap metadata = coverage.getMetadata();
        GeneralParameterValue[] readParameters = CoverageUtils.getParameters(
                readParametersDescriptor, coverage.getParameters(), readGeom);
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        // pass down time
        final DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
        // add the descriptors for custom dimensions 
        final List<GeneralParameterDescriptor> parameterDescriptors = 
                new ArrayList<GeneralParameterDescriptor>(readParametersDescriptor.getDescriptor().descriptors());
        Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
        parameterDescriptors.addAll(dynamicParameters);
        if (timeInfo != null && timeInfo.isEnabled()) {
            // handle "current"
            List<Object> fixedTimes = new ArrayList<Object>(times);
            for (int i = 0; i < fixedTimes.size(); i++) {
                if (fixedTimes.get(i) == null) {
                    fixedTimes.set(i, getCurrentTime(coverage, dimensions));
                }
            }
            // pass down the parameters
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                    fixedTimes, "TIME", "Time");
        }

        // pass down elevation
        final DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevationInfo != null && elevationInfo.isEnabled()) {
            // handle "current"
            List<Object> fixedElevations = new ArrayList<Object>(elevations);
            for (int i = 0; i < fixedElevations.size(); i++) {
                if (fixedElevations.get(i) == null) {
                    fixedElevations.set(i, getDefaultElevation(coverage, dimensions));
                }
            }
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                    fixedElevations, "ELEVATION", "Elevation");
        }

        if (layerFilter != null && readParameters != null) {
            // test for default [empty is replaced with INCLUDE filter] ]filter
            for (int i = 0; i < readParameters.length; i++) {

                GeneralParameterValue param = readParameters[i];
                GeneralParameterDescriptor pd = param.getDescriptor();

                if (pd.getName().getCode().equalsIgnoreCase("FILTER")) {
                    final ParameterValue pv = (ParameterValue) pd.createValue();
                    // if something different from the default INCLUDE filter is specified
                    if (layerFilter != Filter.INCLUDE) {
                        // override the default filter
                        pv.setValue(layerFilter);
                        readParameters[i] = pv;
                    }
                    break;
                }
            }
        }
        
        // custom dimensions
        final Map<String, String> rawKvpMap = request.getRawKvp();
        List<String> customDomains = new ArrayList(dimensions.getCustomDomains());
        if (rawKvpMap != null) {
            for (Map.Entry<String, String> kvp : rawKvpMap.entrySet()) {
                String name = kvp.getKey();
                if (name.regionMatches(true, 0, "dim_", 0, 4)) {
                    name = name.substring(4);
                    name = caseInsensitiveLookup(customDomains, name);
                    if(name != null) {
                        // remove it so that we don't have to set the default value
                        customDomains.remove(name);
                        final DimensionInfo customInfo = metadata.get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + name,
                                DimensionInfo.class);
                        if (dimensions.hasDomain(name) && customInfo != null && customInfo.isEnabled()) {
                            final ArrayList<String> val = new ArrayList<String>(1);
                            if(kvp.getValue().indexOf(",")>0){
                                String[] elements = kvp.getValue().split(",");
                                val.addAll(Arrays.asList(elements));
                            }else{
                                val.add(kvp.getValue());
                            }
                            readParameters = CoverageUtils.mergeParameter(
                                parameterDescriptors, readParameters, val, name);
                        }
                    }
                }
            }
        }
        
        // see if we have any custom domain for which we have to set the default value
        if(!customDomains.isEmpty()) {
            for (String name : customDomains) {
                final DimensionInfo customInfo = metadata.get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + name,
                        DimensionInfo.class);
                if (customInfo != null && customInfo.isEnabled()) {
                    final ArrayList<String> val = new ArrayList<String>(1);
                    val.add(dimensions.getCustomDomainDefaultValue(name));
                    readParameters = CoverageUtils.mergeParameter(
                        parameterDescriptors, readParameters, val, name);
                }
            }
        }

        return readParameters;
    }

    private String caseInsensitiveLookup(List<String> names, String name) {
        for (String s : names) {
            if(name.equalsIgnoreCase(s)) {
                return s;
            }
        }
        
        return null;
    }

    public Collection<RenderedImageMapResponse> getAvailableMapResponses() {
        return WMSExtensions.findMapResponses(applicationContext);
    }

    /**
     * Returns the list of time values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     * 
     * @param typeInfo
     * @return
     * @throws IOException
     */
    public TreeSet<Date> getFeatureTypeTimes(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.getPrefixedName()
                    + " does not have time support enabled");
        }

        FeatureCollection collection = getDimensionCollection(typeInfo, time);

        TreeSet<Date> result = new TreeSet<Date>();
        if (time.getPresentation() == DimensionPresentation.LIST) {
            final UniqueVisitor visitor = new UniqueVisitor(time.getAttribute());
            collection.accepts(visitor, null);

            @SuppressWarnings("unchecked")
            Set<Date> values = visitor.getUnique();
            if (values.size() <= 0) {
                result = null;
            } else {
                // we might get null values out of the visitor, strip them
                values.remove(null);
                result.addAll(values);
            }
        } else {
            final MinVisitor min = new MinVisitor(time.getAttribute());
            collection.accepts(min, null);
            CalcResult minResult = min.getResult();
            // check calcresult first to avoid potential IllegalStateException if no features are in collection
            if (minResult != CalcResult.NULL_RESULT) {
                result.add((Date) min.getMin());
                final MaxVisitor max = new MaxVisitor(time.getAttribute());
                collection.accepts(max, null);
                result.add((Date) max.getMax());
            }
        }

        return result;
    }

    /**
     * Returns the list of elevation values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     * 
     * @param typeInfo
     * @return
     * @throws IOException
     */
    public TreeSet<Double> getFeatureTypeElevations(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo elevation = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.getPrefixedName()
                    + " does not have elevation support enabled");
        }

        FeatureCollection collection = getDimensionCollection(typeInfo, elevation);

        TreeSet<Double> result = new TreeSet<Double>();
        if (elevation.getPresentation() == DimensionPresentation.LIST
                || (elevation.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL && elevation
                        .getResolution() == null)) {
            final UniqueVisitor visitor = new UniqueVisitor(elevation.getAttribute());
            collection.accepts(visitor, null);

            @SuppressWarnings("unchecked")
            Set<Object> values = visitor.getUnique();
            if (values.size() <= 0) {
                result = null;
            } else {
                for (Object value : values) {
                    result.add(((Number) value).doubleValue());
                }
            }
        } else {
            final MinVisitor min = new MinVisitor(elevation.getAttribute());
            collection.accepts(min, null);
            // check calcresult first to avoid potential IllegalStateException if no features are in collection
            CalcResult calcResult = min.getResult();
            if (calcResult != CalcResult.NULL_RESULT) {
                result.add(((Number) min.getMin()).doubleValue());
                final MaxVisitor max = new MaxVisitor(elevation.getAttribute());
                collection.accepts(max, null);
                result.add(((Number) max.getMax()).doubleValue());
            }
        }

        return result;
    }

    /**
     * Returns the current time for the specified type info
     * 
     * @param typeInfo
     * @return
     * @throws IOException
     */
    public Date getCurrentTime(FeatureTypeInfo typeInfo) throws IOException {
        // check the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.getPrefixedName()
                    + " does not have time support enabled");
        }

        // current is the max time we have
        FeatureCollection collection = getDimensionCollection(typeInfo, time);
        final MaxVisitor max = new MaxVisitor(time.getAttribute());
        collection.accepts(max, null);
        if (max.getResult() != CalcResult.NULL_RESULT) {
            return (Date) max.getMax();
        } else {
            return null;
        }
    }

    /**
     * Returns the current time for the specified coverage
     */
    Date getCurrentTime(CoverageInfo coverage, ReaderDimensionsAccessor dimensions)
            throws IOException {
        // check the time metadata
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        String name = coverage.getPrefixedName();
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + name + " does not have time support enabled");
        }

        // get and parse the current time
        return dimensions.getMaxTime();
    }

    /**
     * Returns the default elevation (the minimum one)
     * 
     * @param typeInfo
     * @return
     */
    Double getDefaultElevation(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo elevation = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.getPrefixedName()
                    + " does not have time support enabled");
        }

        FeatureCollection collection = getDimensionCollection(typeInfo, elevation);
        final MinVisitor min = new MinVisitor(elevation.getAttribute());
        collection.accepts(min, null);
        if (min.getResult() == CalcResult.NULL_RESULT) {
            return null;
        } else {
            return ((Number) min.getMin()).doubleValue();
        }
    }

    /**
     * Returns the default elevation (the minimum one)
     * 
     * @param typeInfo
     * @return
     */
    Double getDefaultElevation(CoverageInfo coverage, ReaderDimensionsAccessor dimensions)
            throws IOException {
        // check the time metadata
        DimensionInfo elevation = coverage.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevation == null || !coverage.isEnabled()) {
            throw new ServiceException("Layer " + coverage.getPrefixedName()
                    + " does not have time support enabled");
        }

        // get and parse the lowest elevation
        return dimensions.getMinElevation();
    }

    /**
     * Returns the collection of all values of the dimension attribute, eventually sorted if the
     * native capabilities allow for it
     * 
     * @param typeInfo
     * @param dimension
     * @return
     * @throws IOException
     */
    FeatureCollection getDimensionCollection(FeatureTypeInfo typeInfo, DimensionInfo dimension)
            throws IOException {
        // grab the feature source
        FeatureSource source = null;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Could not get the feauture source to list time info for layer "
                            + typeInfo.getPrefixedName(), e);
        }

        // build query to grab the dimension values
        final Query dimQuery = new Query(source.getSchema().getName().getLocalPart());
        dimQuery.setPropertyNames(Arrays.asList(dimension.getAttribute()));
        return source.getFeatures(dimQuery);
    }
    
    /**
     * Build a filter for a single value based on an attribute and optional
     * endAttribute. The value is either a Range or object that can be used as
     * a literal (Date,Number).
     * @param value
     * @param attribute
     * @param endAttribute
     * @return 
     */
    Filter buildDimensionFilter(Object value, PropertyName attribute, PropertyName endAttribute) {
        Filter filter;
        if (value == null) {
            filter = Filter.INCLUDE;
        } else if (value instanceof Range) {
            Range range = (Range) value;
            if (endAttribute == null) {
                filter = ff.between(attribute, ff.literal(range.getMinValue()),
                         ff.literal(range.getMaxValue()));
            } else {
                // Range intersects valid range of feature
                // @todo adding another option to dimensionInfo allows contains, versus intersects
                Literal qlower = ff.literal(range.getMinValue());
                Literal qupper = ff.literal(range.getMaxValue());
                Filter lower = ff.lessOrEqual(attribute, qupper);
                Filter upper = ff.greaterOrEqual(endAttribute, qlower);
                return ff.and(lower, upper);
            }
        } else {
            // Single element is equal to
            if (endAttribute == null) {
                filter = ff.equal(attribute, ff.literal(value), true);
            } else {
                // Single element is contained by valid range of feature
                Filter lower = ff.greaterOrEqual(ff.literal(value), attribute);
                Filter upper = ff.lessOrEqual(ff.literal(value), endAttribute);
                filter = ff.and(lower, upper);
            }
        }
        return filter;
    }

    /**
     * Builds a filter for the current time and elevation, should the layer support them. Only one
     * among time and elevation can be multi-valued
     * 
     * @param layerFilter
     * @param currentTime
     * @param currentElevation
     * @param mapLayerInfo
     * @return
     */
    public Filter getTimeElevationToFilter(List<Object> times, List<Object> elevations,
            FeatureTypeInfo typeInfo) throws IOException {
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        DimensionInfo elevationInfo = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        
        // handle time support
        Filter result = null;
        if (timeInfo != null && timeInfo.isEnabled() && times != null) {
            final List<Filter> timeFilters = new ArrayList<Filter>();
            final PropertyName attribute = ff.property(timeInfo.getAttribute());
            final PropertyName endAttribute = timeInfo.getEndAttribute() == null ? null :
                ff.property(timeInfo.getEndAttribute());

            for (Object datetime : times) {
                if (datetime == null) {
                    // this is "current"
                    datetime = getCurrentTime(typeInfo);
                }
                timeFilters.add(buildDimensionFilter(datetime, attribute, endAttribute));
            }
            final int sizeTime = timeFilters.size();
            if (sizeTime > 1) {
                result = ff.or(timeFilters);
            } else if (sizeTime == 1) {
                result = timeFilters.get(0);
            }
        }

        // handle elevation support
        if (elevationInfo != null && elevationInfo.isEnabled() && elevations != null) {
            final List<Filter> elevationFilters = new ArrayList<Filter>();
            final PropertyName attribute = ff.property(elevationInfo.getAttribute());
            final PropertyName endAttribute = elevationInfo.getEndAttribute() == null ? null :
                ff.property(elevationInfo.getEndAttribute());
            for (Object elevation : elevations) {
                if (elevation == null) {
                    // this is "current"
                    elevation = getDefaultElevation(typeInfo);
                }
                elevationFilters.add(buildDimensionFilter(elevation, attribute, endAttribute));
            }
            final int size = elevationFilters.size();
            Filter summary = null;
            if (size > 1) {
                summary = ff.or(elevationFilters);
            } else if (size == 1) {
                summary = elevationFilters.get(0);
            }

            // mix with the previous filter
            if (summary != null) {
                result = Filters.and(ff, result, summary);
            }
        }

        return result;
    }

    /**
     * Converts a coordinate expressed on the device space back to real world coordinates. Stolen
     * from LiteRenderer but without the need of a Graphics object
     * 
     * @param x
     *            horizontal coordinate on device space
     * @param y
     *            vertical coordinate on device space
     * @param map
     *            The map extent
     * @param width
     *            image width
     * @param height
     *            image height
     * 
     * @return The correspondent real world coordinate
     * 
     * @throws RuntimeException
     */
    public static Coordinate pixelToWorld(double x, double y, ReferencedEnvelope map, double width, double height) {
        // set up the affine transform and calculate scale values
        AffineTransform at = worldToScreenTransform(map, width, height);
    
        Point2D result = null;
    
        try {
            result = at.inverseTransform(new java.awt.geom.Point2D.Double(x, y),
                    new java.awt.geom.Point2D.Double());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    
        Coordinate c = new Coordinate(result.getX(), result.getY());
    
        return c;
    }

    /**
     * Sets up the affine transform. Stolen from liteRenderer code.
     * 
     * @param mapExtent
     *            the map extent
     * @param width
     *            the screen size
     * @param height
     * 
     * @return a transform that maps from real world coordinates to the screen
     */
    public static AffineTransform worldToScreenTransform(ReferencedEnvelope mapExtent, double width, double height) {
        
        //the transformation depends on an x/y ordering, if we have a lat/lon crs swap it
        CoordinateReferenceSystem crs = mapExtent.getCoordinateReferenceSystem();
        boolean swap = crs != null && CRS.getAxisOrder(crs) == AxisOrder.NORTH_EAST;
        if (swap) {
            mapExtent = new ReferencedEnvelope(mapExtent.getMinY(), mapExtent.getMaxY(), 
                mapExtent.getMinX(), mapExtent.getMaxX(), null);
        }
        
        double scaleX = (double) width / mapExtent.getWidth();
        double scaleY = (double) height / mapExtent.getHeight();
    
        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + height;
    
        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
    
        //if we swapped concatenate a transform that swaps back
        if (swap) {
            at.concatenate(new AffineTransform(0, 1, 1, 0, 0, 0));
        }
    
        return at;
    }

    public static WMS get() {
        return GeoServerExtensions.bean(WMS.class);
    }
}
