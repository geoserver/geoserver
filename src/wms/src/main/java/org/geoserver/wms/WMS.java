/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.geoserver.catalog.AttributeTypeInfo;
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
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategyFactory;
import org.geoserver.wms.dimension.DimensionFilterBuilder;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.decorate.Wrapper;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
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
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.geoserver.wms.NearestMatchWarningAppender.WarningType.Nearest;
import static org.geoserver.wms.NearestMatchWarningAppender.WarningType.NotFound;

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

    public static final String DISPOSAL_METHOD = "disposalMethod";

    public static final String LOOP_CONTINUOUSLY = "loopContinuously";

    public static final Boolean LOOP_CONTINUOUSLY_DEFAULT = Boolean.FALSE;
    
    public static final String SCALEHINT_MAPUNITS_PIXEL = "scalehintMapunitsPixel";
    
    public static final Boolean SCALEHINT_MAPUNITS_PIXEL_DEFAULT = Boolean.FALSE;
    
    public static final String DYNAMIC_STYLING_DISABLED = "dynamicStylingDisabled";

    public static final String FEATURES_REPROJECTION_DISABLED = "featuresReprojectionDisabled";

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
     * Enable continuous map wrapping (global sys var)
     */
    public static Boolean ENABLE_MAP_WRAPPING = null;

    /**
     * Continuous map wrapping key
     */
    public static String MAP_WRAPPING_KEY = "mapWrapping";

    /**
     * Enable advanced projection handling
     */
    public static Boolean ENABLE_ADVANCED_PROJECTION = null;

    /**
     * Advanced projection key
     */
    public static String ADVANCED_PROJECTION_KEY = "advancedProjectionHandling";

    /**
     * GIF disposal methods
     */
    public static final String DISPOSAL_METHOD_NONE = "none";

    public static final String DISPOSAL_METHOD_NOT_DISPOSE = "doNotDispose";

    public static final String DISPOSAL_METHOD_BACKGROUND = "backgroundColor";

    public static final String DISPOSAL_METHOD_PREVIOUS = "previous";

    public static final String DISPOSAL_METHOD_DEFAULT = DISPOSAL_METHOD_NONE;

    public static final String[] DISPOSAL_METHODS = {
            DISPOSAL_METHOD_NONE, 
            DISPOSAL_METHOD_NOT_DISPOSE,
            DISPOSAL_METHOD_BACKGROUND,
            DISPOSAL_METHOD_PREVIOUS
    };

    /**
     * the WMS Animator animatorExecutor service
     */
    private ExecutorService animatorExecutorService;
    
    private static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    private final GeoServer geoserver;

    private ApplicationContext applicationContext;

    private DimensionDefaultValueSelectionStrategyFactory defaultDimensionValueFactory;

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
     *
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

    public boolean isDynamicStylingDisabled() {
        return getServiceInfo().isDynamicStylingDisabled();
    }

    /**
     * If TRUE is returned GetFeatureInfo results should NOT be reproject
     * to the map coordinate reference system.
     *
     * @return GetFeatureInfo results reprojection allowance
     */
    public boolean isFeaturesReprojectionDisabled() {
        return getServiceInfo().isFeaturesReprojectionDisabled();
    }

    public JAIInfo.PngEncoderType getPNGEncoderType() {
        JAIInfo jaiInfo = getJaiInfo();
        return jaiInfo.getPngEncoderType();
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

    /**
     * Checks if continuous map wrapping is enabled or not
     * 
     *
     */
    public boolean isContinuousMapWrappingEnabled() {
        // for backwards compatibility we set the config value to the sys variable one if set, but
        // once set, the config wins
        Boolean enabled = getMetadataValue(MAP_WRAPPING_KEY, ENABLE_MAP_WRAPPING, Boolean.class);
        return enabled;
    }

    /**
     * Checks if advanced projection handling is enabled or not
     * 
     *
     */
    public boolean isAdvancedProjectionHandlingEnabled() {
        // for backwards compatibility we set the config value to the sys variable one if set, but
        // once set, the config wins
        Boolean enabled = getMetadataValue(ADVANCED_PROJECTION_KEY, ENABLE_ADVANCED_PROJECTION,
                Boolean.class);
        return enabled;
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
    
    public String getDisposalMethod() {
        return getMetadataValue(DISPOSAL_METHOD, DISPOSAL_METHOD_DEFAULT, String.class);
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

    /**
     * Returns the maximum number of requested dimension values, picking from the appropriate service 
     * configuration 
     * @return
     */
    public int getMaxRequestedDimensionValues() {
        return getServiceInfo().getMaxRequestedDimensionValues();
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
     * Returns all allowed map output formats. 
     */
    public Collection<GetMapOutputFormat> getAllowedMapFormats() {
        List<GetMapOutputFormat> result = new ArrayList<GetMapOutputFormat>();
        for (GetMapOutputFormat producer:  WMSExtensions.findMapProducers(applicationContext)) {
            if (isAllowedGetMapFormat(producer)) {
                result.add(producer);
            }
        }
        return result;
    }
    
    /**
     * Returns all  map output formats. 
     */
    public Collection<GetMapOutputFormat> getAvailableMapFormats() {
        return WMSExtensions.findMapProducers(applicationContext);
    }


    /**
     * Grabs the list of allowed MIME-Types for the GetMap operation from the set of
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
    
    /**
     * @return all allowed GetMap format names
     */
    public Set<String> getAllowedMapFormatNames() {

        final Collection<GetMapOutputFormat> producers;
        producers = WMSExtensions.findMapProducers(applicationContext);
        final Set<String> formats = new HashSet<String>();

        for (GetMapOutputFormat producer : producers) {
            if (isAllowedGetMapFormat(producer)==false) {
                continue; // skip this producer, its mime type is not allowed
            }
            formats.addAll(producer.getOutputFormatNames());
        }

        return formats;

    }

    
    /**
     * Checks is a getMap mime type is allowed
     * 
     * @param format
     *
     */
    public boolean isAllowedGetMapFormat(GetMapOutputFormat format) {
        
        if  (getServiceInfo().isGetMapMimeTypeCheckingEnabled()==false)
            return true;
        Set<String> mimeTypes = getServiceInfo().getGetMapMimeTypes();
        return mimeTypes.contains(format.getMimeType());
    }
    
    /**
     * Checks is a getFeatureInfo mime type is allowed
     * 
     * @param format
     *
     */
    public boolean isAllowedGetFeatureInfoFormat(GetFeatureInfoOutputFormat format) {
        if (getServiceInfo().isGetFeatureInfoMimeTypeCheckingEnabled()==false)
            return true;
        Set<String> mimeTypes = getServiceInfo().getGetFeatureInfoMimeTypes();
        return mimeTypes.contains(format.getContentType());
    }

    /**
     * create a {@link ServiceException} for an unallowed
     * GetFeatureInfo format
     * 
     * @param requestFormat
     *
     */
    public ServiceException unallowedGetFeatureInfoFormatException(String requestFormat) {
        ServiceException e = new ServiceException("Getting feature info using "
                + requestFormat + " is not allowed", "ForbiddenFormat");
        e.setCode("ForbiddenFormat");
        return e;
    }

    /**
     * create a {@link ServiceException} for an unallowed
     * GetMap format
     * 
     * @param requestFormat
     *
     */
    public ServiceException unallowedGetMapFormatException(String requestFormat) {
        ServiceException e = new ServiceException("Creating maps using "
            + requestFormat + " is not allowed", "ForbiddenFormat");
        e.setCode("ForbiddenFormat");
        return e;
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

        // get the default dimension value selector factory, picking the one with
        // the highest priority (this allows for plugin overrides)
        defaultDimensionValueFactory = GeoServerExtensions.extensions(
                DimensionDefaultValueSelectionStrategyFactory.class).get(0);

        // enable/disable map wrapping
        if (ENABLE_MAP_WRAPPING == null) {
            String wrapping = GeoServerExtensions.getProperty("ENABLE_MAP_WRAPPING",
                    applicationContext);
            // default to true, but allow switching off
            if (wrapping == null)
                ENABLE_MAP_WRAPPING = true;
            else
                ENABLE_MAP_WRAPPING = Boolean.valueOf(wrapping);
        }

        // enable/disable advanced reprojection handling
        if (ENABLE_ADVANCED_PROJECTION == null) {
            String projection = GeoServerExtensions.getProperty("ENABLE_ADVANCED_PROJECTION",
                    applicationContext);
            // default to true, but allow switching off
            if (projection == null)
                ENABLE_ADVANCED_PROJECTION = true;
            else
                ENABLE_ADVANCED_PROJECTION = Boolean.valueOf(projection);
        }
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

    /**
     * @return a list of all getFeatureInfo content types
     */
    public List<String> getAvailableFeatureInfoFormats() {
        List<String> mimeTypes = new ArrayList<String>();
        for (GetFeatureInfoOutputFormat format : WMSExtensions.findFeatureInfoFormats(applicationContext)) {
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
        
    }
    
    /**
     * @return a list of all allowed getFeature info content types
     */
    public List<String> getAllowedFeatureInfoFormats() {
        List<String> mimeTypes = new ArrayList<String>();
        for (GetFeatureInfoOutputFormat format : WMSExtensions.findFeatureInfoFormats(applicationContext)) {
            if (isAllowedGetFeatureInfoFormat(format)==false) {
                continue; // skip this format
            }                
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
     *
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
                srs = srs.toUpperCase().replace("EPSG:", "urn:ogc:def:crs:EPSG:");
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
                OperationType featureInfo = caps.getRequest().getGetFeatureInfo();
                if (featureInfo == null || !featureInfo.getFormats()
                        .contains("application/vnd.ogc.gml")) {
                    return false;
                }
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                return false;
            }

            return layer.isQueryable();

        } catch (IOException e) {
            LOGGER.log(Level.INFO,
                    "Failed to determine if the layer is queryable, assuming it's not", e);
            return false;
        }
    }

    /**
     * Returns true if the layer is opaque
     */
    public boolean isOpaque(LayerInfo layer) {
        return layer.isOpaque();
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
        if (layerGroup.isQueryDisabled())
            return false;
        
        boolean queryable = false;
        List<PublishedInfo> layers = getLayersForQueryableChecks(layerGroup);
        for (PublishedInfo published : layers) {
            if (published instanceof LayerInfo) {
                queryable |= isQueryable((LayerInfo) published);
            } else {
                queryable |= isQueryable((LayerGroupInfo) published);
            }
        }
        return queryable;
    }

    /**
     * For queryability purposes, a group is queryable if any layer inside of it, advertised or not,
     * is queriable (because the non advertised layer is still available in GetFeatureInfo). So,
     * just for this use case, we are going to unwrap {@link
     * org.geoserver.catalog.impl.AdvertisedCatalog.AdvertisedLayerGroup} and get the original list
     * of layers. The security wrappers are below it by construction, so this won't case, per se,
     * security issues.
     *
     * @param layerGroup the group whose query-ability needs to be checked
     * @return a list of {@link PublishedInfo} contained in the layer (queryable or not)
     */
    private List<PublishedInfo> getLayersForQueryableChecks(LayerGroupInfo layerGroup) {
        List<PublishedInfo> layers = layerGroup.getLayers();
        // direct wrapper?
        if (layerGroup instanceof AdvertisedCatalog.AdvertisedLayerGroup) {
            AdvertisedCatalog.AdvertisedLayerGroup wrapper =
                    (AdvertisedCatalog.AdvertisedLayerGroup) layerGroup;
            layers = wrapper.getOriginalLayers();
        } else if (layerGroup instanceof Wrapper) {
            // hidden inside some other wrapper?
            Wrapper wrapper = (Wrapper) layerGroup;
            if (wrapper.isWrapperFor(AdvertisedCatalog.AdvertisedLayerGroup.class)) {
                wrapper.unwrap(AdvertisedCatalog.AdvertisedLayerGroup.class);
                AdvertisedCatalog.AdvertisedLayerGroup alg =
                        (AdvertisedCatalog.AdvertisedLayerGroup) layerGroup;
                layers = alg.getOriginalLayers();
            }
        }
        return layers;
    }

    /**
     * Returns the read parameters for the specified layer, merging some well known request
     * parameters into the read parameters if possible
     * 
     * @deprecated Use {@link #getWMSReadParameters(GetMapRequest, MapLayerInfo, Filter, SortBy[], List, List, GridCoverage2DReader, boolean)} instead
     */
    @Deprecated
    public GeneralParameterValue[] getWMSReadParameters(final GetMapRequest request,
            final MapLayerInfo mapLayerInfo, final Filter layerFilter, final List<Object> times,
            final List<Object> elevations, final GridCoverage2DReader reader,
            boolean readGeom) throws IOException {
        return getWMSReadParameters(request, mapLayerInfo, layerFilter, null, times, elevations, reader, readGeom);
    }

    
    /**
     * Returns the read parameters for the specified layer, merging some well known request
     * parameters into the read parameters if possible
     */
    public GeneralParameterValue[] getWMSReadParameters(final GetMapRequest request,
            final MapLayerInfo mapLayerInfo, final Filter layerFilter, SortBy[] sortBy, final List<Object> times,
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
            // handle "default"
            List<Object> fixedTimes = new ArrayList<Object>(times);
            for (int i = 0; i < fixedTimes.size(); i++) {
                if (fixedTimes.get(i) == null) {
                    fixedTimes.set(i, getDefaultTime(coverage));
                }

                // nearest time support
                if (timeInfo.isNearestMatchEnabled()) {
                    fixedTimes = getNearestMatches(coverage, timeInfo, fixedTimes, ResourceInfo.TIME);
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
            // handle "default"
            List<Object> fixedElevations = new ArrayList<Object>(elevations);
            for (int i = 0; i < fixedElevations.size(); i++) {
                if (fixedElevations.get(i) == null) {
                    fixedElevations.set(i, getDefaultElevation(coverage));
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
        
        if (sortBy != null && readParameters != null) {
            // test for default sortBy
            for (int i = 0; i < readParameters.length; i++) {

                GeneralParameterValue param = readParameters[i];
                GeneralParameterDescriptor pd = param.getDescriptor();

                if (pd.getName().getCode().equalsIgnoreCase("SORTING")) {
                    final ParameterValue pv = (ParameterValue) pd.createValue();
                    if (pd instanceof ParameterDescriptor
                            && String.class.equals(((ParameterDescriptor) pd).getValueClass())) {
                        // convert down to string
                        String sortBySpec = Arrays.stream(sortBy)
                                .map(sb -> sb.getPropertyName().getPropertyName() + " "
                                        + sb.getSortOrder().name().charAt(0))
                                .collect(Collectors.joining(","));
                        pv.setValue(sortBySpec);
                    } else {
                        pv.setValue(sortBy);
                    }
                    readParameters[i] = pv;
                    break;
                }
                
            }
        }
        
        // custom dimensions
        List<String> customDomains = new ArrayList(dimensions.getCustomDomains());
        for (String domain : new ArrayList<>(customDomains)) {
            List<String> values = request.getCustomDimension(domain);
            if (values != null) {
                int maxValues = getMaxRequestedDimensionValues();
                if (maxValues > 0 && maxValues < values.size()) {
                    throw new ServiceException(
                            "More than " + maxValues + " dimension values specified in the request, bailing out.",
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "DIM_" + domain.toUpperCase());
                }

                readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                        dimensions.convertDimensionValue(domain, values), domain);
                customDomains.remove(domain);
            }
        }
        
        // see if we have any custom domain for which we have to set the default value
        if(!customDomains.isEmpty()) {
            for (String name : customDomains) {
                final DimensionInfo customInfo = metadata.get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + name,
                        DimensionInfo.class);
                if (customInfo != null && customInfo.isEnabled()) {
                    Object val = dimensions.convertDimensionValue(name,
                            getDefaultCustomDimensionValue(name, coverage, String.class));

                    readParameters = CoverageUtils.mergeParameter(
                        parameterDescriptors, readParameters, val, name);
                }
            }
        }

        return readParameters;
    }

    public Collection<RenderedImageMapResponse> getAvailableMapResponses() {
        return WMSExtensions.findMapResponses(applicationContext);
    }

    /**
     * Query and returns the times for the given layer, in the given time range
     * @throws IOException 
     */
    public TreeSet<Object> queryCoverageTimes(CoverageInfo coverage, DateRange queryRange,
            int maxAnimationSteps) throws IOException {
        // grab the time metadata
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + coverage.getPrefixedName()
                    + " does not have time support enabled");
        }
        
        GridCoverage2DReader reader = null;
        try {
            reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            throw new ServiceException("Unable to acquire a reader for this coverage " + coverage.prefixedName(), t);
        }
        if (reader  == null) {
            throw new ServiceException("Unable to acquire a reader for this coverage " + coverage.prefixedName());
        }
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        return dimensions.getTimeDomain(queryRange, maxAnimationSteps);
    }
    
    /**
     * Query and returns the times for the given layer, in the given time range
     */
    public TreeSet<Object> queryFeatureTypeTimes(FeatureTypeInfo typeInfo, DateRange range, int maxItems) throws IOException {
        return queryFeatureTypeDimension(typeInfo, range, maxItems, ResourceInfo.TIME);
    }

    /**
     * Returns the list of time values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     * 
     * @param typeInfo
     *
     * @throws IOException
     */
    public TreeSet<Date> getFeatureTypeTimes(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.prefixedName()
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
     * Query and returns the elevations for the given layer, in the given time range
     * @throws IOException 
     */
    public TreeSet<Object> queryCoverageElevations(CoverageInfo coverage, NumberRange queryRange,
            int maxAnimationSteps) throws IOException {
        // grab the metadata
        DimensionInfo elevation = coverage.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException("Layer " + coverage.prefixedName()
                    + " does not have elevation support enabled");
        }
        
        GridCoverage2DReader reader = null;
        try {
            reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            throw new ServiceException("Unable to acquire a reader for this coverage " + coverage.prefixedName(), t);
        }
        if (reader  == null) {
            throw new ServiceException("Unable to acquire a reader for this coverage " + coverage.prefixedName());
        }
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        return dimensions.getElevationDomain(queryRange, maxAnimationSteps);
    }

    /**
     * Returns the list of elevation values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     * 
     * @param typeInfo
     *
     * @throws IOException
     */
    public TreeSet<Double> getFeatureTypeElevations(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo elevation = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.prefixedName()
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
     * Query and returns the times for the given layer, in the given time range
     */
    public TreeSet<Object> queryFeatureTypeElevations(FeatureTypeInfo typeInfo, NumberRange range, int maxItems) throws IOException {
        return queryFeatureTypeDimension(typeInfo, range, maxItems, ResourceInfo.ELEVATION);
    }
    
    /**
     * Query and returns the dimension values for the given layer, in the given range
     */
    TreeSet<Object> queryFeatureTypeDimension(FeatureTypeInfo typeInfo, Range range, int maxItems, String dimensionName) throws IOException {
        // grab the metadata
        DimensionInfo di = typeInfo.getMetadata().get(dimensionName, DimensionInfo.class);
        if (di == null || !di.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.prefixedName()
                    + " does not have " + dimensionName + " support enabled");
        }

        // filter by date range
        FeatureSource fs = getFeatureSource(typeInfo);
        // build query to grab the time values
        final Query query = new Query(fs.getSchema().getName().getLocalPart());
        query.setPropertyNames(Arrays.asList(di.getAttribute()));
        final PropertyName attribute = ff.property(di.getAttribute());
        final PropertyIsBetween rangeFilter = ff.between(attribute, ff.literal(range.getMinValue()), ff.literal(range.getMaxValue()));
        query.setFilter(rangeFilter);
        query.setMaxFeatures(maxItems);
        FeatureCollection collection = fs.getFeatures(query);
        
        // collect all unique values (can't do ranges now, we don't have a multi-attribute unique visitor)
        UniqueVisitor visitor = new UniqueVisitor(attribute);
        collection.accepts(visitor, null);
        TreeSet<Object> result = new TreeSet<>(visitor.getUnique());
        return result;
    }

    /**
     * Returns the default value for time dimension.
     * 
     * @param resourceInfo
     *
     */
    public Object getDefaultTime(ResourceInfo resourceInfo) {
        // check the time metadata
        DimensionInfo time = resourceInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + resourceInfo.prefixedName()
                    + " does not have time support enabled");
        }        
        DimensionDefaultValueSelectionStrategy strategy = this.getDefaultValueStrategy(resourceInfo, ResourceInfo.TIME, time);        
        return strategy.getDefaultValue(resourceInfo, ResourceInfo.TIME, time, Date.class);
    }
    
    

    /**
     * Returns the default value for elevation dimension.
     * 
     * @param resourceInfo
     *
     */
    public Object getDefaultElevation(ResourceInfo resourceInfo) {
        DimensionInfo elevation = getDimensionInfo(resourceInfo, ResourceInfo.ELEVATION);
        DimensionDefaultValueSelectionStrategy strategy = this.getDefaultValueStrategy(resourceInfo, ResourceInfo.ELEVATION, elevation);
        return strategy.getDefaultValue(resourceInfo, ResourceInfo.ELEVATION, elevation, Double.class);
    }

    /**
     * Looks up the elevation configuration, throws an exception if not found
     * @param resourceInfo
     * @param dimensionName 
     *
     */
    public DimensionInfo getDimensionInfo(ResourceInfo resourceInfo, String dimensionName) {
        DimensionInfo info = resourceInfo.getMetadata().get(dimensionName,
                DimensionInfo.class);
        if (info == null || !info.isEnabled()) {
            throw new ServiceException("Layer " + resourceInfo.prefixedName()
                    + " does not have " + dimensionName + " support enabled");
        }
        return info;
    }
    
    /**
     * Returns the default value for the given custom dimension.
     * 
     * @param <T>
     * @param dimensionName
     * @param resourceInfo
     * @param clz
     *
     */
    public <T> T getDefaultCustomDimensionValue(String dimensionName, ResourceInfo resourceInfo, Class<T> clz){
        DimensionInfo customDim = resourceInfo.getMetadata().get(ResourceInfo.CUSTOM_DIMENSION_PREFIX+dimensionName,
                DimensionInfo.class);
        if (customDim == null || !customDim.isEnabled()) {
            throw new ServiceException("Layer " + resourceInfo.prefixedName()
                    + " does not have support enabled for dimension "+dimensionName);
        }
        DimensionDefaultValueSelectionStrategy strategy = this.getDefaultValueStrategy(resourceInfo, ResourceInfo.CUSTOM_DIMENSION_PREFIX+dimensionName, customDim);
        // custom dimensions have no range support
        return (T) strategy.getDefaultValue(resourceInfo, ResourceInfo.CUSTOM_DIMENSION_PREFIX+dimensionName, customDim, clz);
    }
    
    
    public DimensionDefaultValueSelectionStrategy getDefaultValueStrategy(ResourceInfo resource,
            String dimensionName, DimensionInfo dimensionInfo){
        if (defaultDimensionValueFactory != null) {
            return defaultDimensionValueFactory.getStrategy(resource, dimensionName, dimensionInfo);
         }
         else {
             return null;
         }
    }

    /**
     * Returns the collection of all values of the dimension attribute, eventually sorted if the
     * native capabilities allow for it
     * 
     * @param typeInfo
     * @param dimension
     *
     * @throws IOException
     */
    FeatureCollection getDimensionCollection(FeatureTypeInfo typeInfo, DimensionInfo dimension)
            throws IOException {
        FeatureSource source = getFeatureSource(typeInfo);

        // build query to grab the dimension values
        final Query dimQuery = new Query(source.getSchema().getName().getLocalPart());
        dimQuery.setPropertyNames(Arrays.asList(dimension.getAttribute()));
        return source.getFeatures(dimQuery);
    }

    /**
     * Returns the feature source for the given feature type
     */
    FeatureSource getFeatureSource(FeatureTypeInfo typeInfo) {
        // grab the feature source
        FeatureSource source = null;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Could not get the feauture source to list time info for layer "
                            + typeInfo.prefixedName(), e);
        }
        return source;
    }
    


    /**
     * Builds a filter for the current time and elevation, should the layer support them. Only one
     * among time and elevation can be multi-valued
     * 
     * @param layerFilter
     * @param currentTime
     * @param currentElevation
     * @param mapLayerInfo
     *
     */
    public Filter getTimeElevationToFilter(List<Object> times, List<Object> elevations,
            FeatureTypeInfo typeInfo) throws IOException {
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        DimensionInfo elevationInfo = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);

        // handle time support
        if (timeInfo != null && timeInfo.isEnabled() && times != null) {
            List<Object> defaultedTimes = new ArrayList<>(times.size());
            for (Object datetime : times) {
                if (datetime == null) {
                    // this is "default"
                    datetime = getDefaultTime(typeInfo);
                }
                defaultedTimes.add(datetime);
            }
            
            if(timeInfo.isNearestMatchEnabled()) {
                List<Object> nearestMatchedTimes = getNearestMatches(typeInfo, timeInfo, defaultedTimes, ResourceInfo.TIME);
                builder.appendFilters(timeInfo.getAttribute(), timeInfo.getEndAttribute(), nearestMatchedTimes);
            } else {
                builder.appendFilters(timeInfo.getAttribute(), timeInfo.getEndAttribute(), defaultedTimes);
            }
        }

        // handle elevation support
        if (elevationInfo != null && elevationInfo.isEnabled() && elevations != null) {
            List<Object> defaultedElevations = new ArrayList<Object>(elevations.size());
            for (Object elevation : elevations) {
                if (elevation == null) {
                    // this is "default"
                    elevation = getDefaultElevation(typeInfo);
                }
                defaultedElevations.add(elevation);
            }
            builder.appendFilters(elevationInfo.getAttribute(), elevationInfo.getEndAttribute(),
                    defaultedElevations);
        }

        Filter result = builder.getFilter();
        return result;
    }

    private List<Object> getNearestMatches(ResourceInfo resourceInfo, DimensionInfo dimension, List<Object> values, String dimensionName) throws IOException {
        // if there is a max rendering time set, use it on this match, as the input request might make the
        // code go through a lot of nearest match queries
        int maxRenderingTime = getMaxRenderingTime();
        long maxTime = maxRenderingTime > 0 ? System.currentTimeMillis() + maxRenderingTime * 1000 : -1;
        NearestMatchFinder finder = NearestMatchFinder.get(resourceInfo, dimension, dimensionName);
        List<Object> result = new ArrayList<>();
        for (Object value : values) {
            Object nearest = finder.getNearest(value);
            if (nearest == null) {
                // no way to specify there is no match yet, so we'll use the original value, which will not match
                NearestMatchWarningAppender.addWarning(resourceInfo.prefixedName(), dimensionName, null, dimension.getUnits(), NotFound);
                result.add(value);
            } else if (value.equals(nearest)) {
                result.add(value);
            } else {
                NearestMatchWarningAppender.addWarning(resourceInfo.prefixedName(), dimensionName, nearest, dimension.getUnits(), Nearest);
                result.add(nearest);
            }

            // check timeout
            if (maxTime > 0 && System.currentTimeMillis() > maxTime) {
                throw new ServiceException(
                        "Nearest matching dimension values required more time than allowed and has been forcefully stopped. "
                                + "The max rendering time is " + (maxRenderingTime) + "s");
            }
        }

        return result;
    }


    /**
     * Returns the max rendering time taking into account the server limits and the request options
     * @param request
     * @return
     */
    public int getMaxRenderingTime(GetMapRequest request) {
        int localMaxRenderingTime = 0;
        Object timeoutOption = request.getFormatOptions().get("timeout");
        if (timeoutOption != null) {
            try {
                localMaxRenderingTime = Integer.parseInt(timeoutOption.toString());
            } catch (NumberFormatException e) {
                RenderedImageMapOutputFormat.LOGGER.log(Level.WARNING,"Could not parse format_option \"timeout\": "+timeoutOption, e);
            }
        }
        int maxRenderingTime = getMaxRenderingTime(localMaxRenderingTime);
        return maxRenderingTime;
    }
    
    /**
     * Returns the max rendering time for animations taking into account the server limits and the request options
     * @param request
     * @return
     */
    public int getMaxAnimationRenderingTime(GetMapRequest request) {
        int localMaxRenderingTime = 0;
        Object timeoutOption = request.getFormatOptions().get("timeout");
        if (timeoutOption != null) {
            try {
                localMaxRenderingTime = Integer.parseInt(timeoutOption.toString());
            } catch (NumberFormatException e) {
                RenderedImageMapOutputFormat.LOGGER.log(Level.WARNING,"Could not parse format_option \"timeout\": "+timeoutOption, e);
            }
        }
        Long maxRenderingTime = getMaxAnimatorRenderingTime();
        if(maxRenderingTime == null) {
            return localMaxRenderingTime;
        } else if(localMaxRenderingTime == 0) {
            return maxRenderingTime.intValue();
        } else {
            return Math.min(maxRenderingTime.intValue(), localMaxRenderingTime);
        }
    }
    
    /**
     * Timeout on the smallest nonzero value of the WMS timeout and the timeout format option
     * If both are zero then there is no timeout
     * 
     * @param localMaxRenderingTime
     *
     */
    private int getMaxRenderingTime(int localMaxRenderingTime) {
        int maxRenderingTime = getMaxRenderingTime() * 1000;
        
        if (maxRenderingTime == 0) {
            maxRenderingTime = localMaxRenderingTime;
        } else if (localMaxRenderingTime != 0) {
            maxRenderingTime = Math.min(maxRenderingTime, localMaxRenderingTime);
        }
        
        return maxRenderingTime;
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
        
        double scaleX = width / mapExtent.getWidth();
        double scaleY = height / mapExtent.getHeight();
    
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

    /**
     * Checks if the layer can be drawn, that is, if it's raster, or vector with a geometry attribute
     * @param lyr
     * @return
     */
    public static boolean isWmsExposable(LayerInfo lyr) {
        if (lyr.getType() == PublishedType.RASTER || lyr.getType() == PublishedType.WMS || lyr.getType() == PublishedType.WMTS) {
            return true;
        }

        if (lyr.getType() == PublishedType.VECTOR) {
            final ResourceInfo resource = lyr.getResource();
            try {
                for (AttributeTypeInfo att : ((FeatureTypeInfo) resource).attributes()) {
                    if (att.getBinding() != null
                            && Geometry.class.isAssignableFrom(att.getBinding())) {
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                        "An error occurred trying to determine if" + " the layer is geometryless",
                        e);
            }
        }

        return false;
    }
}
