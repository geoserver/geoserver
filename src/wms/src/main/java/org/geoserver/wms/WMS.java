/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geoserver.catalog.ResourceInfo.ELEVATION;
import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.geoserver.util.HTTPWarningAppender.addWarning;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupHelper;
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
import org.geoserver.data.DimensionFilterBuilder;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.DimensionWarning;
import org.geoserver.util.NearestMatchFinder;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.capabilities.DimensionHelper;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategyFactory;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.ows.OperationType;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.Version;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
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

    public static final String SCALEHINT_MAPUNITS_PIXEL = "scalehintMapunitsPixel";

    public static final Boolean SCALEHINT_MAPUNITS_PIXEL_DEFAULT = Boolean.FALSE;

    public static final String DYNAMIC_STYLING_DISABLED = "dynamicStylingDisabled";

    public static final String FEATURES_REPROJECTION_DISABLED = "featuresReprojectionDisabled";

    static final Logger LOGGER = Logging.getLogger(WMS.class);

    public static final String WEB_CONTAINER_KEY = "WMS";

    /** SVG renderers. */
    public static final String SVG_SIMPLE = "Simple";

    public static final String SVG_BATIK = "Batik";

    /** Prefix for custom dimensions used in GetMap requests */
    public static final String DIM_ = "dim_";

    /** KML reflector mode */
    public static String KML_REFLECTOR_MODE = "kmlReflectorMode";

    /** KML reflector mode values */
    public static final String KML_REFLECTOR_MODE_REFRESH = "refresh";

    public static final String KML_REFLECTOR_MODE_SUPEROVERLAY = "superoverlay";

    public static final String KML_REFLECTOR_MODE_DOWNLOAD = "download";

    public static final String KML_REFLECTOR_MODE_DEFAULT = KML_REFLECTOR_MODE_REFRESH;

    /** KML superoverlay sub-mode */
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

    /** Enable continuous map wrapping (global sys var) */
    public static Boolean ENABLE_MAP_WRAPPING = null;

    /** Continuous map wrapping key */
    public static String MAP_WRAPPING_KEY = "mapWrapping";

    /** Enable advanced projection handling */
    public static Boolean ENABLE_ADVANCED_PROJECTION = null;

    /** Advanced projection key */
    public static String ADVANCED_PROJECTION_KEY = "advancedProjectionHandling";

    /** Enable advanced projection handling */
    public static Boolean ENABLE_ADVANCED_PROJECTION_DENSIFICATION = false;

    /** Advanced projection densification key */
    public static String ADVANCED_PROJECTION_DENSIFICATION_KEY = "advancedProjectionDensification";

    /** Disable DateLine Wrapping Heuristic. */
    public static Boolean DISABLE_DATELINE_WRAPPING_HEURISTIC = false;

    /** DateLine Wrapping Heuristic key */
    public static String DATELINE_WRAPPING_HEURISTIC_KEY = "disableDatelineWrappingHeuristic";

    /**
     * Capabilities will be produced with a root Layer element, only when needed (there is no single
     * top layer element) *
     */
    public static Boolean ROOT_LAYER_IN_CAPABILITIES_DEFAULT = true;

    /** Root Layer in Capabilities key * */
    public static String ROOT_LAYER_IN_CAPABILITIES_KEY = "rootLayerInCapabilities";

    /** GIF disposal methods */
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
     * Whether to throw an InvalidDimensioValue on invalid dimension values
     *
     * @return
     */
    public boolean exceptionOnInvalidDimension() {
        return getServiceInfo().exceptionOnInvalidDimension();
    }

    /**
     * /** Returns a supported version according to the version negotiation rules in section 6.2.4
     * of the WMS 1.3.0 spec.
     *
     * <p>Calls through to {@link #negotiateVersion(Version)}.
     *
     * @param requestedVersion The version, may be bull.
     */
    public static Version negotiateVersion(final String requestedVersion) {
        return negotiateVersion(requestedVersion != null ? new Version(requestedVersion) : null);
    }

    /**
     * Returns a supported version according to the version negotiation rules in section 6.2.4 of
     * the WMS 1.3.0 spec.
     *
     * <p>For instance: <u>
     * <li>request version not provided? -> higher version supported
     * <li>requested version supported? -> that same version
     * <li>requested version < lowest supported version? -> lowest supported
     * <li>requested version > lowest supported version? -> higher supported version that's lower
     *     than the requested version </u>
     *
     * @param requestedVersion the request version, or {@code null} if unspecified
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
        if (versions.isEmpty()) {
            // shouldn't a version be set?
            version = "1.1.1";
        } else {
            version = versions.get(0).toString();
        }
        return version;
    }

    /**
     * Checks if request would result in a number of dimensions that exceeds the configured maximum
     *
     * @param mapLayerInfo the layer info
     * @param times the times requested
     * @param elevations the elevations requested
     * @param isCoverage true if the layer is a coverage
     * @throws IOException if an error occurs
     */
    public void checkMaxDimensions(
            MapLayerInfo mapLayerInfo,
            List<Object> times,
            List<Object> elevations,
            boolean isCoverage)
            throws IOException {
        if (getServiceInfo() == null
                || getServiceInfo().getMaxRequestedDimensionValues()
                        == DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES) {
            // max is default, which is unlimited
            return;
        }
        TreeSet<Object> treeSet = new TreeSet<>();
        int maxDimensionsToTest = getServiceInfo().getMaxRequestedDimensionValues();
        if (times != null && !times.isEmpty() && times.get(0) != null) {
            // list is null for default value
            ListIterator<Object> timesIterator = times.listIterator();
            while (timesIterator.hasNext()) {
                Object time = timesIterator.next();
                if (time instanceof DateRange) {
                    DateRange range = (DateRange) times.get(timesIterator.previousIndex());
                    if (isCoverage) {
                        treeSet.addAll(
                                queryCoverageTimes(
                                        mapLayerInfo.getCoverage(),
                                        range,
                                        maxDimensionsToTest + 1));
                    } else {
                        treeSet.addAll(
                                queryFeatureTypeTimes(
                                        mapLayerInfo.getFeature(), range, maxDimensionsToTest + 1));
                    }
                } else {
                    treeSet.add(time);
                }
                // check dimensions after each time parameter is added to treeset
                checkDimensions(treeSet, maxDimensionsToTest, ResourceInfo.TIME);
            }
        }

        if (elevations != null && !elevations.isEmpty() && elevations.get(0) != null) {
            // list is null for default value, so we skip it
            ListIterator<Object> elevationsIterator = elevations.listIterator();
            while (elevationsIterator.hasNext()) {
                Object elevation = elevationsIterator.next();
                if (elevation instanceof NumberRange) {
                    NumberRange range =
                            (NumberRange) elevations.get(elevationsIterator.previousIndex());
                    if (isCoverage) {
                        treeSet.addAll(
                                queryCoverageElevations(
                                        mapLayerInfo.getCoverage(),
                                        range,
                                        maxDimensionsToTest + 1));
                    } else {
                        treeSet.addAll(
                                queryFeatureTypeElevations(
                                        mapLayerInfo.getFeature(), range, maxDimensionsToTest + 1));
                    }
                } else if (elevation instanceof Double) {
                    // The queryElevations calls that check ranges are populating the treeset with
                    // integers,
                    // so we need to convert the double to an integer for this check
                    Double elevationSingle = (Double) elevation;
                    treeSet.add(elevationSingle.intValue());
                } else {
                    treeSet.add(elevation);
                }
                // check dimensions after each elevation parameter is added to treeset
                checkDimensions(treeSet, maxDimensionsToTest, ResourceInfo.ELEVATION);
            }
        }
    }

    private static void checkDimensions(
            TreeSet<Object> treeSet, int maxDimensionsToTest, String dimensionName) {
        if (treeSet.size() > maxDimensionsToTest) {
            throw new ServiceException(
                    "This request would process more "
                            + dimensionName
                            + " than the maximum allowed: "
                            + maxDimensionsToTest
                            + ". Please reduce the size of the requested "
                            + dimensionName
                            + " range.",
                    "InvalidParameterValue",
                    dimensionName);
        }
    }

    public GeoServer getGeoServer() {
        return this.geoserver;
    }

    public WMSInterpolation getInterpolation() {
        return getServiceInfo().getInterpolation();
    }

    public boolean isDynamicStylingDisabled() {
        return getServiceInfo().isDynamicStylingDisabled();
    }

    /**
     * If TRUE is returned GetFeatureInfo results should NOT be reproject to the map coordinate
     * reference system.
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

    public boolean isRemoteStylesCacheEnabled() {
        CacheConfiguration cache = getServiceInfo().getCacheConfiguration();
        return cache != null && cache.isEnabled() ? true : false;
    }

    public CacheConfiguration getRemoteResourcesCacheConfiguration() {
        return getServiceInfo().getCacheConfiguration();
    }

    public void setRemoteResourcesCacheConfiguration(CacheConfiguration cacheCfg) {
        getServiceInfo().setCacheConfiguration(cacheCfg);
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
     * <p>//djb: we support it now
     *
     * @return false
     */
    public boolean supportsSLD() {
        return true; // djb: we support it now
    }

    /**
     * Informs the user that this WMS supports User Layers
     *
     * <p>We support this both remote wfs and inlineFeature
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
        Boolean svgAntiAlias =
                Converters.convert(serviceInfo.getMetadata().get("svgAntiAlias"), Boolean.class);
        return svgAntiAlias == null ? true : svgAntiAlias.booleanValue();
    }

    public int getPngCompression() {
        WMSInfo serviceInfo = getServiceInfo();
        return getMetadataPercentage(
                serviceInfo.getMetadata(), PNG_COMPRESSION, PNG_COMPRESSION_DEFAULT);
    }

    public int getJpegCompression() {
        WMSInfo serviceInfo = getServiceInfo();
        return getMetadataPercentage(
                serviceInfo.getMetadata(), JPEG_COMPRESSION, JPEG_COMPRESSION_DEFAULT);
    }

    /** Checks if continuous map wrapping is enabled or not */
    public boolean isContinuousMapWrappingEnabled() {
        // for backwards compatibility we set the config value to the sys variable one if set, but
        // once set, the config wins
        Boolean enabled = getMetadataValue(MAP_WRAPPING_KEY, ENABLE_MAP_WRAPPING, Boolean.class);
        return enabled;
    }

    /** Checks if advanced projection handling is enabled or not */
    public boolean isAdvancedProjectionHandlingEnabled() {
        // for backwards compatibility we set the config value to the sys variable one if set, but
        // once set, the config wins
        Boolean enabled =
                getMetadataValue(
                        ADVANCED_PROJECTION_KEY, ENABLE_ADVANCED_PROJECTION, Boolean.class);
        return enabled;
    }

    public boolean isAdvancedProjectionDensificationEnabled() {
        Boolean enabled =
                getMetadataValue(
                        ADVANCED_PROJECTION_DENSIFICATION_KEY,
                        ENABLE_ADVANCED_PROJECTION_DENSIFICATION,
                        Boolean.class);
        return enabled;
    }

    public boolean isDateLineWrappingHeuristicDisabled() {
        Boolean disabled =
                getMetadataValue(
                        DATELINE_WRAPPING_HEURISTIC_KEY,
                        DISABLE_DATELINE_WRAPPING_HEURISTIC,
                        Boolean.class);
        return disabled;
    }

    public boolean isRootLayerInCapabilitesEnabled() {
        return getMetadataValue(
                ROOT_LAYER_IN_CAPABILITIES_KEY, ROOT_LAYER_IN_CAPABILITIES_DEFAULT, Boolean.class);
    }

    public Boolean getScalehintUnitPixel() {
        return getMetadataValue(
                SCALEHINT_MAPUNITS_PIXEL, SCALEHINT_MAPUNITS_PIXEL_DEFAULT, Boolean.class);
    }

    int getMetadataPercentage(MetadataMap metadata, String key, int defaultValue) {
        Integer parsedValue = Converters.convert(metadata.get(key), Integer.class);
        if (parsedValue == null) return defaultValue;
        int value = parsedValue.intValue();
        if (value < 0 || value > 100) {
            LOGGER.warning(
                    "Invalid percertage value for '" + key + "', it should be between 0 and 100");
            return defaultValue;
        }

        return value;
    }

    <T> T getMetadataValue(String key, T defaultValue, Class<T> clazz) {
        if (getServiceInfo() == null) {
            return defaultValue;
        }

        MetadataMap metadata = getServiceInfo().getMetadata();

        T parsedValue = Converters.convert(metadata.get(key), clazz);
        if (parsedValue == null) return defaultValue;

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
     * Returns the maximum number of requested dimension values, picking from the appropriate
     * service configuration
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
        Boolean kmAttr =
                Converters.convert(getServiceInfo().getMetadata().get(KML_KMLATTR), Boolean.class);
        return kmAttr == null ? KML_KMLATTR_DEFAULT : kmAttr.booleanValue();
    }

    public boolean getKmlPlacemark() {
        Boolean kmAttr =
                Converters.convert(
                        getServiceInfo().getMetadata().get(KML_KMLPLACEMARK), Boolean.class);
        return kmAttr == null ? KML_KMLPLACEMARK_DEFAULT : kmAttr.booleanValue();
    }

    public int getKmScore() {
        return getMetadataPercentage(
                getServiceInfo().getMetadata(), KML_KMSCORE, KML_KMSCORE_DEFAULT);
    }

    /** Returns all allowed map output formats. */
    public Collection<GetMapOutputFormat> getAllowedMapFormats() {
        List<GetMapOutputFormat> result = new ArrayList<>();
        for (GetMapOutputFormat producer : WMSExtensions.findMapProducers(applicationContext)) {
            if (isAllowedGetMapFormat(producer)) {
                result.add(producer);
            }
        }
        return result;
    }

    /** Returns all map output formats. */
    public Collection<GetMapOutputFormat> getAvailableMapFormats() {
        return WMSExtensions.findMapProducers(applicationContext);
    }

    /**
     * Grabs the list of allowed MIME-Types for the GetMap operation from the set of {@link
     * GetMapOutputFormat}s registered in the application context.
     *
     * @see GetMapOutputFormat#getContentType()
     */
    public Set<String> getAvailableMapFormatNames() {

        final Collection<GetMapOutputFormat> producers =
                WMSExtensions.findMapProducers(applicationContext);
        final Set<String> formats = new HashSet<>();

        for (GetMapOutputFormat producer : producers) {
            formats.addAll(producer.getOutputFormatNames());
        }
        return formats;
    }

    /** @return all allowed GetMap format names */
    public Set<String> getAllowedMapFormatNames() {

        final Collection<GetMapOutputFormat> producers =
                WMSExtensions.findMapProducers(applicationContext);
        final Set<String> formats = new HashSet<>();

        for (GetMapOutputFormat producer : producers) {
            if (isAllowedGetMapFormat(producer) == false) {
                continue; // skip this producer, its mime type is not allowed
            }
            formats.addAll(producer.getOutputFormatNames());
        }

        return formats;
    }

    /** Checks is a getMap mime type is allowed */
    public boolean isAllowedGetMapFormat(GetMapOutputFormat format) {

        if (getServiceInfo().isGetMapMimeTypeCheckingEnabled() == false) return true;
        Set<String> mimeTypes = getServiceInfo().getGetMapMimeTypes();
        return mimeTypes.contains(format.getMimeType());
    }

    /** Checks is a getFeatureInfo mime type is allowed */
    public boolean isAllowedGetFeatureInfoFormat(GetFeatureInfoOutputFormat format) {
        if (getServiceInfo().isGetFeatureInfoMimeTypeCheckingEnabled() == false) return true;
        Set<String> mimeTypes = getServiceInfo().getGetFeatureInfoMimeTypes();
        return mimeTypes.contains(format.getContentType());
    }

    /** create a {@link ServiceException} for an unallowed GetFeatureInfo format */
    public ServiceException unallowedGetFeatureInfoFormatException(String requestFormat) {
        ServiceException e =
                new ServiceException(
                        "Getting feature info using " + requestFormat + " is not allowed",
                        "ForbiddenFormat");
        e.setCode("ForbiddenFormat");
        return e;
    }

    /** create a {@link ServiceException} for an unallowed GetMap format */
    public ServiceException unallowedGetMapFormatException(String requestFormat) {
        ServiceException e =
                new ServiceException(
                        "Creating maps using " + requestFormat + " is not allowed",
                        "ForbiddenFormat");
        e.setCode("ForbiddenFormat");
        return e;
    }

    public Set<String> getAvailableLegendGraphicsFormats() {

        List<GetLegendGraphicOutputFormat> formats =
                WMSExtensions.findLegendGraphicFormats(applicationContext);

        Set<String> mimeTypes = new HashSet<>();
        for (GetLegendGraphicOutputFormat format : formats) {
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
    }

    /** Returns all {@link ExtendedCapabilitiesProvider} extensions. */
    public List<ExtendedCapabilitiesProvider> getAvailableExtendedCapabilitiesProviders() {
        return WMSExtensions.findExtendedCapabilitiesProviders(applicationContext);
    }

    @Override
    @SuppressFBWarnings("LI_LAZY_INIT_STATIC") // method is not called by multiple threads
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;

        // get the default dimension value selector factory, picking the one with
        // the highest priority (this allows for plugin overrides)
        defaultDimensionValueFactory =
                GeoServerExtensions.extensions(DimensionDefaultValueSelectionStrategyFactory.class)
                        .get(0);

        // enable/disable map wrapping
        if (ENABLE_MAP_WRAPPING == null) {
            String wrapping =
                    GeoServerExtensions.getProperty("ENABLE_MAP_WRAPPING", applicationContext);
            // default to true, but allow switching off
            if (wrapping == null) ENABLE_MAP_WRAPPING = true;
            else ENABLE_MAP_WRAPPING = Boolean.valueOf(wrapping);
        }

        // enable/disable advanced reprojection handling
        if (ENABLE_ADVANCED_PROJECTION == null) {
            String projection =
                    GeoServerExtensions.getProperty(
                            "ENABLE_ADVANCED_PROJECTION", applicationContext);
            // default to true, but allow switching off
            if (projection == null) ENABLE_ADVANCED_PROJECTION = true;
            else ENABLE_ADVANCED_PROJECTION = Boolean.valueOf(projection);
        }
    }

    /**
     * @return a {@link GetFeatureInfoOutputFormat} that can handle the requested mime type or
     *     {@code null} if none if found
     */
    public GetFeatureInfoOutputFormat getFeatureInfoOutputFormat(String requestFormat) {
        List<GetFeatureInfoOutputFormat> outputFormats =
                WMSExtensions.findFeatureInfoFormats(applicationContext);

        for (GetFeatureInfoOutputFormat format : outputFormats) {
            if (format.canProduce(requestFormat)) {
                return format;
            }
        }
        return null;
    }

    /** @return a list of all getFeatureInfo content types */
    public List<String> getAvailableFeatureInfoFormats() {
        List<String> mimeTypes = new ArrayList<>();
        for (GetFeatureInfoOutputFormat format :
                WMSExtensions.findFeatureInfoFormats(applicationContext)) {
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
    }

    /** @return a list of all allowed getFeature info content types */
    public List<String> getAllowedFeatureInfoFormats() {
        List<String> mimeTypes = new ArrayList<>();
        for (GetFeatureInfoOutputFormat format :
                WMSExtensions.findFeatureInfoFormats(applicationContext)) {
            if (isAllowedGetFeatureInfoFormat(format) == false) {
                continue; // skip this format
            }
            mimeTypes.add(format.getContentType());
        }
        return mimeTypes;
    }

    /** @return a list of allowed remote SLD Urls for AuthorizationHeader forwarding. */
    public List<String> getAllowedURLsForAuthForwarding() {
        return getServiceInfo().getAllowedURLsForAuthForwarding();
    }

    /**
     * @param mimeType the mime type to look a GetMapOutputFormat for
     * @return the GetMapOutputFormat that can handle {@code mimeType}, or {@code null} if none is
     *     found
     */
    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
        GetMapOutputFormat outputFormat =
                WMSExtensions.findMapProducer(mimeType, applicationContext);
        return outputFormat;
    }

    /**
     * @param outputFormat desired output format mime type
     * @return the GetLegendGraphicOutputFormat that can handle {@code mimeType}, or {@code null} if
     *     none is found
     */
    public GetLegendGraphicOutputFormat getLegendGraphicOutputFormat(final String outputFormat) {
        GetLegendGraphicOutputFormat format =
                WMSExtensions.findLegendGraphicFormat(outputFormat, applicationContext);
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
     * @param version The version string.
     * @param exact If set to false, a version object will always be returned. If set to true only a
     *     version matching on of the available wms versions will be returned.
     */
    public static Version version(String version, boolean exact) {
        if (version == null || version.trim().isEmpty()) {
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
     *
     * <p>In version 1.3 of WMS geographic coordinate systems are to be ordered y/x or
     * latitude/longitude. The only possible way to represent this internally is to use the explicit
     * epsg namespace "urn:x-ogc:def:crs:EPSG:". This method essentially replaces the traditional
     * "EPSG:" namespace with the explicit.
     */
    public static String toInternalSRS(String srs, Version version) {
        if (srs != null && VERSION_1_3_0.equals(version)) {
            try {
                CoordinateReferenceSystem crs = CRS.decode(srs);
                if (crs != null) {
                    return GML2EncodingUtils.toURI(crs, SrsSyntax.OGC_URN, false);
                }
            } catch (FactoryException e) {
                throw new ServiceException(
                        "Could not decode CRS: " + srs,
                        e,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "crs");
            }
        }

        return srs;
    }

    /** Returns true if the layer can be queried */
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
                if (featureInfo == null
                        || !featureInfo.getFormats().contains("application/vnd.ogc.gml")) {
                    return false;
                }
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                return false;
            }

            return layer.isQueryable();

        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO,
                    "Failed to determine if the layer is queryable, assuming it's not",
                    e);
            return false;
        }
    }

    /** Returns true if the layer is opaque */
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
        if (layerGroup.isQueryDisabled()) return false;

        boolean queryable = false;
        List<PublishedInfo> layers = getLayersForQueryableChecks(layerGroup);
        for (PublishedInfo published : layers) {
            if (published instanceof LayerInfo) {
                queryable |= isQueryable((LayerInfo) published);
            } else if (published instanceof LayerGroupInfo) {
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
        // direct wrapper?
        if (layerGroup instanceof AdvertisedCatalog.AdvertisedLayerGroup) {
            AdvertisedCatalog.AdvertisedLayerGroup wrapper =
                    (AdvertisedCatalog.AdvertisedLayerGroup) layerGroup;
            layerGroup = wrapper.unwrap();
        } else if (layerGroup instanceof Wrapper) {
            // hidden inside some other wrapper?
            Wrapper wrapper = (Wrapper) layerGroup;
            if (wrapper.isWrapperFor(AdvertisedCatalog.AdvertisedLayerGroup.class)) {
                wrapper.unwrap(AdvertisedCatalog.AdvertisedLayerGroup.class);
                AdvertisedCatalog.AdvertisedLayerGroup alg =
                        (AdvertisedCatalog.AdvertisedLayerGroup) layerGroup;
                layerGroup = alg.unwrap();
            }
        }

        // get the full list of layers and groups
        return new LayerGroupHelper(layerGroup).allPublished();
    }

    /**
     * Returns the read parameters for the specified layer, merging some well known request
     * parameters into the read parameters if possible
     */
    public GeneralParameterValue[] getWMSReadParameters(
            final GetMapRequest request,
            final MapLayerInfo mapLayerInfo,
            final Filter layerFilter,
            SortBy[] sortBy,
            final List<Object> times,
            final List<Object> elevations,
            final GridCoverage2DReader reader,
            boolean readGeom)
            throws IOException {
        // setup the scene
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        CoverageInfo coverage = mapLayerInfo.getCoverage();
        MetadataMap metadata = coverage.getMetadata();
        GeneralParameterValue[] readParameters =
                CoverageUtils.getParameters(
                        readParametersDescriptor, coverage.getParameters(), readGeom);
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        // pass down time
        final DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
        // add the descriptors for custom dimensions
        final List<GeneralParameterDescriptor> parameterDescriptors =
                new ArrayList<>(readParametersDescriptor.getDescriptor().descriptors());
        Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
        parameterDescriptors.addAll(dynamicParameters);
        if (timeInfo != null && timeInfo.isEnabled()) {
            // handle "default"
            List<Object> fixedTimes = new ArrayList<>(times);
            for (int i = 0; i < fixedTimes.size(); i++) {
                if (fixedTimes.get(i) == null) {
                    Object defaultTime = getDefaultTime(coverage);
                    fixedTimes.set(i, defaultTime);
                    addWarning(
                            DimensionWarning.defaultValue(
                                    mapLayerInfo.getResource(), ResourceInfo.TIME, defaultTime));
                }

                // nearest time support
                if (timeInfo.isNearestMatchEnabled()) {
                    fixedTimes =
                            getNearestTimeMatch(
                                    coverage, timeInfo, fixedTimes, getMaxRenderingTime());
                }
            }
            // pass down the parameters
            readParameters =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors, readParameters, fixedTimes, "TIME", "Time");
        }

        // pass down elevation
        final DimensionInfo elevationInfo =
                metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevationInfo != null && elevationInfo.isEnabled()) {
            // handle "default"
            List<Object> fixedElevations = new ArrayList<>(elevations);
            for (int i = 0; i < fixedElevations.size(); i++) {
                if (fixedElevations.get(i) == null) {
                    Object defaultElevation = getDefaultElevation(coverage);
                    fixedElevations.set(i, defaultElevation);
                    addWarning(
                            DimensionWarning.defaultValue(
                                    mapLayerInfo.getResource(),
                                    ResourceInfo.ELEVATION,
                                    defaultElevation));
                }
            }
            readParameters =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors,
                            readParameters,
                            fixedElevations,
                            "ELEVATION",
                            "Elevation");
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
                        String sortBySpec =
                                Arrays.stream(sortBy)
                                        .map(this::sortSpecification)
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
        List<String> customDomains = new ArrayList<>(dimensions.getCustomDomains());
        for (String domain : new ArrayList<>(customDomains)) {
            List<String> values = request.getCustomDimension(domain);
            if (values != null) {
                int maxValues = getMaxRequestedDimensionValues();
                if (maxValues > 0 && maxValues < values.size()) {
                    throw new ServiceException(
                            "More than "
                                    + maxValues
                                    + " dimension values specified in the request, bailing out.",
                            ServiceException.INVALID_PARAMETER_VALUE,
                            DimensionInfo.getDimensionKey(domain));
                }

                readParameters =
                        CoverageUtils.mergeParameter(
                                parameterDescriptors,
                                readParameters,
                                dimensions.convertDimensionValue(domain, values),
                                domain);
                customDomains.remove(domain);
            }
        }

        // see if we have any custom domain for which we have to set the default value
        if (!customDomains.isEmpty()) {
            for (String dimensionName : customDomains) {
                final DimensionInfo customInfo =
                        metadata.get(
                                ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName,
                                DimensionInfo.class);
                if (customInfo != null && customInfo.isEnabled()) {
                    Object val =
                            dimensions.convertDimensionValue(
                                    dimensionName,
                                    getDefaultCustomDimensionValue(
                                            dimensionName, coverage, String.class));
                    addWarning(
                            DimensionWarning.defaultValue(
                                    mapLayerInfo.getResource(), dimensionName, val));
                    readParameters =
                            CoverageUtils.mergeParameter(
                                    parameterDescriptors, readParameters, val, dimensionName);
                }
            }
        }

        return readParameters;
    }

    private String sortSpecification(SortBy sb) {
        return sb.getPropertyName().getPropertyName() + " " + sb.getSortOrder().name().charAt(0);
    }

    public Collection<RenderedImageMapResponse> getAvailableMapResponses() {
        return WMSExtensions.findMapResponses(applicationContext);
    }

    /** Query and returns the times for the given layer, in the given time range */
    public TreeSet<Object> queryCoverageTimes(
            CoverageInfo coverage, DateRange queryRange, int maxAnimationSteps) throws IOException {
        // grab the time metadata
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException(
                    "Layer " + coverage.prefixedName() + " does not have time support enabled");
        }

        GridCoverage2DReader reader = null;
        try {
            reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            throw new ServiceException(
                    "Unable to acquire a reader for this coverage " + coverage.prefixedName(), t);
        }
        if (reader == null) {
            throw new ServiceException(
                    "Unable to acquire a reader for this coverage " + coverage.prefixedName());
        }
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        return dimensions.getTimeDomain(queryRange, maxAnimationSteps);
    }

    /** Query and returns the requested times for the given layer */
    public TreeSet<Date> queryCoverageNearestMatchTimes(
            CoverageInfo coverage, List<Object> queryRanges) throws IOException {
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        final TreeSet<Date> foundDates = new TreeSet<>();
        if (time == null || !time.isEnabled()) {
            throw new ServiceException(
                    "Coverage " + coverage.prefixedName() + " does not have time support enabled");
        }

        List<Object> dates =
                getNearestTimeMatch(coverage, time, queryRanges, getMaxRenderingTime());
        dates.stream()
                .filter(d -> d instanceof Date)
                .forEach(
                        d -> {
                            Date date = (Date) d;
                            foundDates.add(date);
                        });
        return foundDates;
    }

    private static List<Object> getNearestTimeMatch(
            ResourceInfo coverage,
            DimensionInfo dimension,
            List<Object> queryRanges,
            int maxRenderingTime)
            throws IOException {
        NearestMatchFinder finder = NearestMatchFinder.get(coverage, dimension, ResourceInfo.TIME);
        if (finder != null) {
            return finder.getMatches(coverage, ResourceInfo.TIME, queryRanges, maxRenderingTime);
        } else {
            return Collections.emptyList();
        }
    }

    /** Query and returns the times for the given layer, in the given time range */
    public TreeSet<Object> queryFeatureTypeTimes(
            FeatureTypeInfo typeInfo, DateRange range, int maxItems) throws IOException {
        return queryFeatureTypeDimension(typeInfo, range, maxItems, ResourceInfo.TIME);
    }

    /**
     * Returns the list of time values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     */
    public TreeSet<Date> getFeatureTypeTimes(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException(
                    "Layer " + typeInfo.prefixedName() + " does not have time support enabled");
        }

        FeatureCollection collection = getDimensionCollection(typeInfo, time);

        TreeSet<Date> result = new TreeSet<>();

        String startValue = time.getStartValue();
        String endValue = time.getEndValue();
        if (!StringUtils.isEmpty(startValue)
                && !StringUtils.isEmpty(endValue)
                && time.getPresentation() != DimensionPresentation.LIST) {
            result.add(DimensionHelper.parseTimeRangeValue(startValue));
            result.add(DimensionHelper.parseTimeRangeValue(endValue));
            return result;
        }

        if (time.getPresentation() == DimensionPresentation.LIST) {
            final UniqueVisitor visitor = new UniqueVisitor(time.getAttribute());
            collection.accepts(visitor, null);

            @SuppressWarnings("unchecked")
            Set<Date> values = visitor.getUnique();
            if (values.isEmpty()) {
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
            // check calcresult first to avoid potential IllegalStateException if no features are in
            // collection
            if (minResult != CalcResult.NULL_RESULT) {
                result.add((Date) min.getMin());
                final MaxVisitor max =
                        new MaxVisitor(
                                time.getEndAttribute() != null
                                        ? time.getEndAttribute()
                                        : time.getAttribute());
                collection.accepts(max, null);
                result.add((Date) max.getMax());
            }
        }

        return result;
    }

    /** Query and returns the elevations for the given layer, in the given time range */
    public TreeSet<Object> queryCoverageElevations(
            CoverageInfo coverage, NumberRange queryRange, int maxAnimationSteps)
            throws IOException {
        // grab the metadata
        DimensionInfo elevation =
                coverage.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + coverage.prefixedName()
                            + " does not have elevation support enabled");
        }

        GridCoverage2DReader reader = null;
        try {
            reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            throw new ServiceException(
                    "Unable to acquire a reader for this coverage " + coverage.prefixedName(), t);
        }
        if (reader == null) {
            throw new ServiceException(
                    "Unable to acquire a reader for this coverage " + coverage.prefixedName());
        }
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        return dimensions.getElevationDomain(queryRange, maxAnimationSteps);
    }

    /**
     * Returns the list of elevation values for the specified typeInfo based on the dimension
     * representation: all values for {@link DimensionPresentation#LIST}, otherwise min and max
     */
    public TreeSet<Double> getFeatureTypeElevations(FeatureTypeInfo typeInfo) throws IOException {
        // grab the time metadata
        DimensionInfo elevation =
                typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevation == null || !elevation.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + typeInfo.prefixedName()
                            + " does not have elevation support enabled");
        }

        FeatureCollection collection = getDimensionCollection(typeInfo, elevation);

        TreeSet<Double> result = new TreeSet<>();

        String startValue = elevation.getStartValue();
        String endValue = elevation.getEndValue();
        if (!StringUtils.isEmpty(startValue)
                && !StringUtils.isEmpty(endValue)
                && elevation.getPresentation() != DimensionPresentation.LIST) {
            result.add(Double.parseDouble(startValue));
            result.add(Double.parseDouble(endValue));
            return result;
        }
        if (elevation.getPresentation() == DimensionPresentation.LIST
                || (elevation.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL
                        && elevation.getResolution() == null)) {
            final UniqueVisitor visitor = new UniqueVisitor(elevation.getAttribute());
            collection.accepts(visitor, null);

            @SuppressWarnings("unchecked")
            Set<Object> values = visitor.getUnique();
            if (values.isEmpty()) {
                result = null;
            } else {
                for (Object value : values) {
                    result.add(((Number) value).doubleValue());
                }
            }
        } else {
            final MinVisitor min = new MinVisitor(elevation.getAttribute());
            collection.accepts(min, null);
            // check calcresult first to avoid potential IllegalStateException if no features are in
            // collection
            CalcResult calcResult = min.getResult();
            if (calcResult != CalcResult.NULL_RESULT) {
                result.add(((Number) min.getMin()).doubleValue());
                final MaxVisitor max =
                        new MaxVisitor(
                                elevation.getEndAttribute() != null
                                        ? elevation.getEndAttribute()
                                        : elevation.getAttribute());
                collection.accepts(max, null);
                result.add(((Number) max.getMax()).doubleValue());
            }
        }

        return result;
    }

    /** Query and returns the times for the given layer, in the given time range */
    public TreeSet<Object> queryFeatureTypeElevations(
            FeatureTypeInfo typeInfo, NumberRange range, int maxItems) throws IOException {
        return queryFeatureTypeDimension(typeInfo, range, maxItems, ResourceInfo.ELEVATION);
    }

    /** Query and returns the dimension values for the given layer, in the given range */
    TreeSet<Object> queryFeatureTypeDimension(
            FeatureTypeInfo typeInfo, Range range, int maxItems, String dimensionName)
            throws IOException {
        // grab the metadata
        DimensionInfo di = typeInfo.getMetadata().get(dimensionName, DimensionInfo.class);
        if (di == null || !di.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + typeInfo.prefixedName()
                            + " does not have "
                            + dimensionName
                            + " support enabled");
        }

        // filter by date range
        FeatureSource fs = getFeatureSource(typeInfo);
        // build query to grab the time values
        final Query query = new Query(fs.getSchema().getName().getLocalPart());
        query.setPropertyNames(Arrays.asList(di.getAttribute()));
        final PropertyName attribute = ff.property(di.getAttribute());
        final PropertyIsBetween rangeFilter =
                ff.between(
                        attribute,
                        ff.literal(range.getMinValue()),
                        ff.literal(range.getMaxValue()));
        query.setFilter(rangeFilter);
        query.setMaxFeatures(maxItems);
        FeatureCollection collection = fs.getFeatures(query);

        // collect all unique values (can't do ranges now, we don't have a multi-attribute unique
        // visitor)
        UniqueVisitor visitor = new UniqueVisitor(attribute);
        collection.accepts(visitor, null);
        @SuppressWarnings("unchecked")
        Set<Comparable> uniques = visitor.getUnique();
        TreeSet<Object> result = new TreeSet<>(uniques);
        return result;
    }

    /** Returns the default value for time dimension. */
    public Object getDefaultTime(ResourceInfo resourceInfo) {
        // check the time metadata
        DimensionInfo time = resourceInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException(
                    "Layer " + resourceInfo.prefixedName() + " does not have time support enabled");
        }
        DimensionDefaultValueSelectionStrategy strategy =
                this.getDefaultValueStrategy(resourceInfo, ResourceInfo.TIME, time);
        return strategy.getDefaultValue(resourceInfo, ResourceInfo.TIME, time, Date.class);
    }

    /** Returns the default value for elevation dimension. */
    public Object getDefaultElevation(ResourceInfo resourceInfo) {
        DimensionInfo elevation = getDimensionInfo(resourceInfo, ResourceInfo.ELEVATION);
        DimensionDefaultValueSelectionStrategy strategy =
                this.getDefaultValueStrategy(resourceInfo, ResourceInfo.ELEVATION, elevation);
        return strategy.getDefaultValue(
                resourceInfo, ResourceInfo.ELEVATION, elevation, Double.class);
    }

    /** Looks up the elevation configuration, throws an exception if not found */
    public DimensionInfo getDimensionInfo(ResourceInfo resourceInfo, String dimensionName) {
        DimensionInfo info = resourceInfo.getMetadata().get(dimensionName, DimensionInfo.class);
        if (info == null || !info.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + resourceInfo.prefixedName()
                            + " does not have "
                            + dimensionName
                            + " support enabled");
        }
        return info;
    }

    /**
     * Returns the default value for the given custom dimension.
     *
     * @param <T>
     */
    public <T> T getDefaultCustomDimensionValue(
            String dimensionName, ResourceInfo resourceInfo, Class<T> clz) {
        DimensionInfo customDim =
                resourceInfo
                        .getMetadata()
                        .get(
                                ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName,
                                DimensionInfo.class);
        if (customDim == null || !customDim.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + resourceInfo.prefixedName()
                            + " does not have support enabled for dimension "
                            + dimensionName);
        }
        DimensionDefaultValueSelectionStrategy strategy =
                this.getDefaultValueStrategy(
                        resourceInfo,
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName,
                        customDim);
        // custom dimensions have no range support
        @SuppressWarnings("unchecked")
        T result =
                (T)
                        strategy.getDefaultValue(
                                resourceInfo,
                                ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName,
                                customDim,
                                clz);
        return result;
    }

    public DimensionDefaultValueSelectionStrategy getDefaultValueStrategy(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        if (defaultDimensionValueFactory != null) {
            return defaultDimensionValueFactory.getStrategy(resource, dimensionName, dimensionInfo);
        } else {
            return null;
        }
    }

    /**
     * Returns the collection of all values of the dimension attribute, eventually sorted if the
     * native capabilities allow for it
     */
    FeatureCollection getDimensionCollection(FeatureTypeInfo typeInfo, DimensionInfo dimension)
            throws IOException {
        FeatureSource source = getFeatureSource(typeInfo);

        // build query to grab the dimension values
        final Query dimQuery = new Query(source.getSchema().getName().getLocalPart());
        List<String> propertyNames = new ArrayList<>();
        propertyNames.add(dimension.getAttribute());
        if (dimension.getEndAttribute() != null
                && dimension.getPresentation() != DimensionPresentation.LIST) {
            propertyNames.add(dimension.getEndAttribute());
        }
        dimQuery.setPropertyNames(propertyNames);
        return source.getFeatures(dimQuery);
    }

    /** Returns the feature source for the given feature type */
    FeatureSource getFeatureSource(FeatureTypeInfo typeInfo) {
        // grab the feature source
        FeatureSource source = null;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Could not get the feauture source to list time info for layer "
                            + typeInfo.prefixedName(),
                    e);
        }
        return source;
    }

    public Filter getTimeFilter(
            List<Object> times, FeatureTypeInfo typeInfo, DimensionFilterBuilder builder)
            throws IOException {
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);

        // handle time support
        if (timeInfo != null && timeInfo.isEnabled() && times != null) {
            List<Object> defaultedTimes = new ArrayList<>(times.size());
            for (Object datetime : times) {
                if (datetime == null) {
                    // this is "default"
                    datetime = getDefaultTime(typeInfo);
                    addWarning(
                            DimensionWarning.defaultValue(typeInfo, ResourceInfo.TIME, datetime));
                }
                defaultedTimes.add(datetime);
            }

            if (timeInfo.isNearestMatchEnabled()) {
                List<Object> nearestMatchedTimes =
                        getNearestTimeMatch(
                                typeInfo, timeInfo, defaultedTimes, getMaxRenderingTime());
                builder.appendFilters(
                        timeInfo.getAttribute(), timeInfo.getEndAttribute(), nearestMatchedTimes);
            } else {
                builder.appendFilters(
                        timeInfo.getAttribute(), timeInfo.getEndAttribute(), defaultedTimes);
            }
        }

        return builder.getFilter();
    }

    public Filter getElevationFilter(
            List<Object> elevations, FeatureTypeInfo typeInfo, DimensionFilterBuilder builder)
            throws IOException {
        DimensionInfo elevationInfo =
                typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);

        // handle elevation support
        if (elevationInfo != null && elevationInfo.isEnabled() && elevations != null) {
            List<Object> defaultedElevations = new ArrayList<>(elevations.size());
            for (Object elevation : elevations) {
                if (elevation == null) {
                    // this is "default"
                    elevation = getDefaultElevation(typeInfo);
                    addWarning(
                            DimensionWarning.defaultValue(
                                    typeInfo, ResourceInfo.ELEVATION, elevation));
                }
                defaultedElevations.add(elevation);
            }
            builder.appendFilters(
                    elevationInfo.getAttribute(),
                    elevationInfo.getEndAttribute(),
                    defaultedElevations);
        }

        return builder.getFilter();
    }

    /** Validates the vector dimensions, if enabled and available */
    public void validateVectorDimensions(
            List<Object> times,
            List<Object> elevations,
            FeatureTypeInfo typeInfo,
            GetMapRequest request)
            throws IOException {
        // value validation and exception throwing enabled?
        if (!exceptionOnInvalidDimension()) return;

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                typeInfo.getFeatureSource(null, null);

        validateTimes(times, typeInfo, fs, request);
        validateElevations(elevations, typeInfo, fs, request);
        validateCustomDimensions(typeInfo, fs, request);
    }

    private void validateCustomDimensions(
            FeatureTypeInfo typeInfo,
            FeatureSource<? extends FeatureType, ? extends Feature> fs,
            GetMapRequest request)
            throws IOException {
        CustomDimensionFilterConverter converter = getCustomDimensionFilterConverter(typeInfo);
        Map<String, DimensionInfo> dimensions = converter.getCustomDimensions();
        for (String dimension : dimensions.keySet()) {
            DimensionFilterBuilder filterBuilder = new DimensionFilterBuilder(ff);
            converter.getDimensionsToFilter(request.getRawKvp(), filterBuilder);
            Query dimensionQuery =
                    getDimensionQuery(
                            filterBuilder.getFilter(),
                            dimensions.get(dimension),
                            typeInfo.getFeatureType().getName().getLocalPart());
            if (DataUtilities.first(fs.getFeatures(dimensionQuery)) == null) {
                String key = (DIM_ + dimension).toUpperCase();
                throwInvalidDimensionValue(request, dimension, key);
            }
        }
    }

    private void validateTimes(
            List<Object> times,
            FeatureTypeInfo typeInfo,
            FeatureSource<? extends FeatureType, ? extends Feature> fs,
            GetMapRequest request)
            throws IOException {
        DimensionInfo timeInfo = typeInfo.getMetadata().get(TIME, DimensionInfo.class);
        if (timeInfo == null || !timeInfo.isEnabled() || timeInfo.isNearestMatchEnabled()) return;

        if (times != null) {
            DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
            List<Object> explicitTimes =
                    times.stream().filter(Objects::nonNull).collect(Collectors.toList());
            Query dimensionQuery =
                    getDimensionQuery(
                            getTimeFilter(explicitTimes, typeInfo, builder),
                            timeInfo,
                            typeInfo.getFeatureType().getName().getLocalPart());
            if (DataUtilities.first(fs.getFeatures(dimensionQuery)) == null) {
                throwInvalidDimensionValue(request, TIME, TIME);
            }
        }
    }

    private void validateElevations(
            List<Object> elevations,
            FeatureTypeInfo typeInfo,
            FeatureSource<? extends FeatureType, ? extends Feature> fs,
            GetMapRequest request)
            throws IOException {
        DimensionInfo elevationInfo = typeInfo.getMetadata().get(ELEVATION, DimensionInfo.class);
        // elevation does not currently support nearest match
        if (elevationInfo == null || !elevationInfo.isEnabled()) return;

        if (elevations != null) {
            DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
            List<Object> explicitElevations =
                    elevations.stream().filter(Objects::nonNull).collect(Collectors.toList());
            Query dimensionQuery =
                    getDimensionQuery(
                            getElevationFilter(explicitElevations, typeInfo, builder),
                            elevationInfo,
                            typeInfo.getFeatureType().getName().getLocalPart());
            if (DataUtilities.first(fs.getFeatures(dimensionQuery)) == null) {
                throwInvalidDimensionValue(request, ELEVATION, ELEVATION);
            }
        }
    }

    /**
     * Wraps the given filter in a query for dimension requests.
     *
     * @param filter the filter
     * @param dimensionInfo the dimension info
     * @param typeName the feature type name
     * @return a query that wraps the filter and restricts the query to the dimension attribute and
     *     sets max features = 1.
     */
    private static Query getDimensionQuery(
            Filter filter, DimensionInfo dimensionInfo, String typeName) {
        List<String> propertyNames = new ArrayList<>();
        propertyNames.add(dimensionInfo.getAttribute());
        if (dimensionInfo.getEndAttribute() != null
                && dimensionInfo.getPresentation() != DimensionPresentation.LIST) {
            propertyNames.add(dimensionInfo.getEndAttribute());
        }
        Query query = new Query(typeName, filter);
        query.setPropertyNames(propertyNames);
        query.setMaxFeatures(1);
        return query;
    }

    /** Returns a filter based on times, elevation and custom dimensions found in the request. */
    public Filter getDimensionFilter(
            List<Object> times,
            List<Object> elevations,
            final FeatureTypeInfo featureTypeInfo,
            final GetMapRequest request)
            throws IOException {
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
        getTimeFilter(times, featureTypeInfo, builder);
        getElevationFilter(elevations, featureTypeInfo, builder);
        getCustomDimensionFilter(request.getRawKvp(), featureTypeInfo, builder);
        return builder.getFilter();
    }

    /**
     * Builds a filter for the current time and elevation, should the layer support them. Only one
     * among time and elevation can be multi-valued
     */
    @Deprecated
    public Filter getTimeElevationToFilter(
            List<Object> times, List<Object> elevations, FeatureTypeInfo typeInfo)
            throws IOException {
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
        getTimeFilter(times, typeInfo, builder);
        getElevationFilter(elevations, typeInfo, builder);
        return builder.getFilter();
    }

    /**
     * Builds the custom dimensions filter in base to type info and KVP.
     *
     * @param rawKVP Request KVP map
     * @param typeInfo Feature type info instance
     * @return builded filter
     * @deprecated Use {@link #getCustomDimensionFilter(Map, FeatureTypeInfo,
     *     DimensionFilterBuilder)} instead
     */
    @Deprecated
    public Filter getDimensionsToFilter(
            final Map<String, String> rawKVP, final FeatureTypeInfo typeInfo) {
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
        return getCustomDimensionFilter(rawKVP, typeInfo, builder);
    }

    /**
     * Builds filters for the custom dimensions, given the request to gather the custom dimension
     * values, the feature type info to check they are configured, and a filter builder to accumlate
     * the filters build at each step
     */
    public Filter getCustomDimensionFilter(
            Map<String, String> rawKVP, FeatureTypeInfo typeInfo, DimensionFilterBuilder builder) {
        CustomDimensionFilterConverter converter = getCustomDimensionFilterConverter(typeInfo);
        return converter.getDimensionsToFilter(rawKVP, builder);
    }

    private CustomDimensionFilterConverter getCustomDimensionFilterConverter(
            FeatureTypeInfo typeInfo) {
        CustomDimensionFilterConverter.DefaultValueStrategyFactory defaultValueStrategyFactory =
                (resource, dimensionName, dimensionInfo) ->
                        getDefaultValueStrategy(resource, dimensionName, dimensionInfo);
        final CustomDimensionFilterConverter converter =
                new CustomDimensionFilterConverter(typeInfo, defaultValueStrategyFactory);
        return converter;
    }

    /**
     * Validates that the provided custom dimension values are within the allowed range, eventually
     * throws ServiceException#INVALID_DIMENSION_VALUE if not.
     *
     * @param times
     * @param elevations
     * @param mapLayerInfo
     * @param request
     */
    public void validateRasterDimensions(
            List<Object> times,
            List<Object> elevations,
            MapLayerInfo mapLayerInfo,
            GetMapRequest request)
            throws IOException {
        // value validation and exception throwing enabled?
        if (!exceptionOnInvalidDimension()) return;

        CoverageInfo coverage = mapLayerInfo.getCoverage();
        Map<String, DimensionInfo> dimensions =
                coverage.getMetadata().entrySet().stream()
                        .filter(e -> e.getValue() instanceof DimensionInfo)
                        .filter(e -> ((DimensionInfo) e.getValue()).isEnabled())
                        .filter(e -> !((DimensionInfo) e.getValue()).isNearestMatchEnabled())
                        .collect(
                                Collectors.toMap(
                                        e -> e.getKey(), e -> (DimensionInfo) e.getValue()));
        ReaderDimensionsAccessor accessor =
                new ReaderDimensionsAccessor(
                        (GridCoverage2DReader) mapLayerInfo.getCoverageReader());
        for (Map.Entry<String, DimensionInfo> entry : dimensions.entrySet()) {
            String key = entry.getKey().replaceFirst(DIM_, "");
            if (key.equalsIgnoreCase(TIME)) {
                List<Object> nonDefaultTimes =
                        times.stream().filter(t -> t != null).collect(Collectors.toList());
                if (nonDefaultTimes.isEmpty()) continue;
                if (!accessor.hasAnyTime(nonDefaultTimes)) {
                    throwInvalidDimensionValue(request, TIME, key);
                }
            } else if (key.equalsIgnoreCase(ELEVATION)) {
                List<Object> nonDefaultElevations =
                        elevations.stream().filter(e -> e != null).collect(Collectors.toList());
                if (nonDefaultElevations.isEmpty()) continue;
                if (!accessor.hasAnyElevation(nonDefaultElevations)) {
                    throwInvalidDimensionValue(request, ELEVATION, key);
                }
            } else {
                key = key.replaceFirst(ResourceInfo.CUSTOM_DIMENSION_PREFIX, "");
                List<String> values = request.getCustomDimension(key);
                if (values == null) continue;
                if (!accessor.hasAnyCustomDimension(key, values)) {
                    throwInvalidDimensionValue(request, key, (DIM_ + key).toUpperCase());
                }
            }
        }
    }

    private static void throwInvalidDimensionValue(
            GetMapRequest request, String dimension, String key) {
        throw new ServiceException(
                String.format(
                        "Could not find a match for '%s' value: '%s'",
                        dimension, request.getRawKvp().get(key)),
                ServiceException.INVALID_DIMENSION_VALUE,
                key);
    }

    /**
     * Returns the max rendering time taking into account the server limits and the request options
     */
    public int getMaxRenderingTime(GetMapRequest request) {
        int localMaxRenderingTime = 0;
        Object timeoutOption = request.getFormatOptions().get("timeout");
        if (timeoutOption != null) {
            try {
                localMaxRenderingTime = Integer.parseInt(timeoutOption.toString());
            } catch (NumberFormatException e) {
                RenderedImageMapOutputFormat.LOGGER.log(
                        Level.WARNING,
                        "Could not parse format_option \"timeout\": " + timeoutOption,
                        e);
            }
        }
        int maxRenderingTime = getMaxRenderingTime(localMaxRenderingTime);
        return maxRenderingTime;
    }

    /**
     * Timeout on the smallest nonzero value of the WMS timeout and the timeout format option If
     * both are zero then there is no timeout
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
     * @param x horizontal coordinate on device space
     * @param y vertical coordinate on device space
     * @param map The map extent
     * @param width image width
     * @param height image height
     * @return The correspondent real world coordinate
     */
    public static Coordinate pixelToWorld(
            double x, double y, ReferencedEnvelope map, double width, double height) {
        // set up the affine transform and calculate scale values
        AffineTransform at = worldToScreenTransform(map, width, height);

        Point2D result = null;

        try {
            result =
                    at.inverseTransform(
                            new java.awt.geom.Point2D.Double(x, y),
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
     * @param mapExtent the map extent
     * @param width the screen size
     * @return a transform that maps from real world coordinates to the screen
     */
    public static AffineTransform worldToScreenTransform(
            ReferencedEnvelope mapExtent, double width, double height) {

        // the transformation depends on an x/y ordering, if we have a lat/lon crs swap it
        CoordinateReferenceSystem crs = mapExtent.getCoordinateReferenceSystem();
        boolean swap = crs != null && CRS.getAxisOrder(crs) == AxisOrder.NORTH_EAST;
        if (swap) {
            mapExtent =
                    new ReferencedEnvelope(
                            mapExtent.getMinY(),
                            mapExtent.getMaxY(),
                            mapExtent.getMinX(),
                            mapExtent.getMaxX(),
                            null);
        }

        double scaleX = width / mapExtent.getWidth();
        double scaleY = height / mapExtent.getHeight();

        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + height;

        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);

        // if we swapped concatenate a transform that swaps back
        if (swap) {
            at.concatenate(new AffineTransform(0, 1, 1, 0, 0, 0));
        }

        return at;
    }

    public static WMS get() {
        return GeoServerExtensions.bean(WMS.class);
    }

    /**
     * Checks if the layer can be drawn, that is, if it's raster, or vector with a geometry
     * attribute
     */
    public static boolean isWmsExposable(LayerInfo lyr) {
        if (lyr.getType() == PublishedType.RASTER
                || lyr.getType() == PublishedType.WMS
                || lyr.getType() == PublishedType.WMTS) {
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
                LOGGER.log(
                        Level.SEVERE,
                        "An error occurred trying to determine if" + " the layer is geometryless",
                        e);
            }
        }

        return false;
    }

    /**
     * Returns available values for a custom dimension.
     *
     * @param typeInfo feature type info that holds the custom dimension.
     * @param dimensionInfo Custom dimension name.
     * @return values list.
     */
    public TreeSet<Object> getDimensionValues(FeatureTypeInfo typeInfo, DimensionInfo dimensionInfo)
            throws IOException {
        final FeatureCollection fcollection = getDimensionCollection(typeInfo, dimensionInfo);

        final TreeSet<Object> result = new TreeSet<>();

        String startValue = dimensionInfo.getStartValue();
        String endValue = dimensionInfo.getEndValue();

        if (dimensionInfo.getPresentation() != DimensionPresentation.LIST
                && !StringUtils.isEmpty(startValue)
                && !StringUtils.isEmpty(endValue)) {
            try {
                result.add(Double.parseDouble(startValue));
                result.add(Double.parseDouble(endValue));
            } catch (NumberFormatException e) {
                result.add(DimensionHelper.parseTimeRangeValue(startValue));
                result.add(DimensionHelper.parseTimeRangeValue(endValue));
            }
        } else if (dimensionInfo.getPresentation() == DimensionPresentation.LIST
                || (dimensionInfo.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL
                        && dimensionInfo.getResolution() == null)) {
            final UniqueVisitor uniqueVisitor = new UniqueVisitor(dimensionInfo.getAttribute());
            fcollection.accepts(uniqueVisitor, null);
            @SuppressWarnings("unchecked")
            Set<Object> uniqueValues = uniqueVisitor.getUnique();
            for (Object obj : uniqueValues) {
                result.add(obj);
            }
        } else {
            final MinVisitor minVisitor = new MinVisitor(dimensionInfo.getAttribute());
            fcollection.accepts(minVisitor, null);
            final CalcResult minResult = minVisitor.getResult();
            if (minResult != CalcResult.NULL_RESULT) {
                result.add(minResult.getValue());
                final MaxVisitor maxVisitor =
                        new MaxVisitor(
                                dimensionInfo.getEndAttribute() != null
                                        ? dimensionInfo.getEndAttribute()
                                        : dimensionInfo.getAttribute());
                fcollection.accepts(maxVisitor, null);
                result.add(maxVisitor.getMax());
            }
        }

        return result;
    }

    public boolean isDefaultGroupStyleEnabled() {
        return getServiceInfo().isDefaultGroupStyleEnabled();
    }

    public boolean isTransformFeatureInfo() {
        return !getServiceInfo().isTransformFeatureInfoDisabled();
    }

    public boolean isAutoEscapeTemplateValues() {
        return getServiceInfo().isAutoEscapeTemplateValues();
    }
}
