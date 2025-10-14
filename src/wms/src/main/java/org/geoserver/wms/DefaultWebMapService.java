/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.sld.GetStyles;
import org.geoserver.sld.GetStylesRequest;
import org.geoserver.wms.capabilities.Capabilities_1_3_0_Transformer;
import org.geoserver.wms.capabilities.GetCapabilitiesTransformer;
import org.geoserver.wms.describelayer.DescribeLayerModel;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.xml.transform.TransformerBase;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A default implementation of a {@link WebMapService}
 *
 * <p>This implementations relies on the code setting up the instance to provide the operation beans through the
 * following properties:
 *
 * <ul>
 *   <li>{@link #setDescribeLayer DescribeLayer} for the {@link #describeLayer(DescribeLayerRequest)} operation
 *   <li>{@link #setGetCapabilities GetCapabilities} for the {@link #getCapabilities(GetCapabilitiesRequest)} operation
 *   <li>{@link #setGetFeatureInfo GetFeatureInfo} for the {@link #getFeatureInfo(GetFeatureInfoRequest)} operation
 *   <li>{@link #setGetLegendGraphic GetLegendGraphic} for the {@link #getLegendGraphic(GetLegendGraphicRequest)}
 *       operation
 *   <li>{@link #setGetMap GetMap} for the {@link #getMap(GetMapRequest)} operation
 *   <li>{@link #setGetStyles GetStyles} for the {@link #getStyles(GetStylesRequest)} operation
 * </ul>
 *
 * If an operation is called for which its corresponding operation bean is not set, the call will result in an
 * {@link UnsupportedOperationException}
 *
 * @author Andrea Aime
 * @author Justin Deoliveira
 * @author Gabriel Roldan
 */
public class DefaultWebMapService implements WebMapService, ApplicationContextAware, DisposableBean {
    /** default for 'format' parameter. */
    public static String FORMAT = "image/png";

    /** default for 'styles' parameter. */
    public static List<Style> STYLES = Collections.emptyList();

    /** longest side for the preview */
    public static int MAX_SIDE = 768;

    /** minimum height to have a reasonable looking OL preview */
    public static int MIN_OL_HEIGHT = 330;

    /** minimum width to have a reasonable looking OL preview */
    public static int MIN_OL_WIDTH = 330;

    /** max height to have a reasonable looking OL preview */
    public static int MAX_OL_HEIGHT = 768;

    /** max width to have a reasonable looking OL preview */
    public static int MAX_OL_WIDTH = 1024;

    /** default for 'srs' parameter. */
    public static String SRS = "EPSG:4326";

    /** default for 'transparent' parameter. */
    public static Boolean TRANSPARENT = Boolean.TRUE;

    /** default for 'transparent' parameter. */
    public static volatile ExecutorService RENDERING_POOL;

    /** default for 'bbox' paramter */
    public static ReferencedEnvelope BBOX =
            new ReferencedEnvelope(new Envelope(-180, 180, -90, 90), DefaultGeographicCRS.WGS84);

    /** wms configuration */
    private final WMS wms;

    /** Temporary field that handles the usage of the line width optimization code */
    private static Boolean OPTIMIZE_LINE_WIDTH = null;

    /** This variable is used to bypass direct raster rendering. */
    private static boolean BYPASS_DIRECT = Boolean.getBoolean("org.geoserver.render.raster.direct.disable");

    /** Max number of rule filters to be used against the data source */
    private static Integer MAX_FILTER_RULES = null;

    /** Use a global rendering pool, or use a new pool each time */
    private static Boolean USE_GLOBAL_RENDERING_POOL = null;

    private GetCapabilities getCapabilities;

    private DescribeLayer describeLayer;

    private GetMap getMap;

    private GetFeatureInfo getFeatureInfo;

    private GetStyles getStyles;

    private GetLegendGraphic getLegendGraphic;

    public DefaultWebMapService(WMS wms) {
        this.wms = wms;
    }

    /** @see WebMapService#getServiceInfo() */
    @Override
    public WMSInfo getServiceInfo() {
        return wms.getServiceInfo();
    }

    /** Establishes the operation bean responsible for executing the GetCapabilities requests */
    public void setGetCapabilities(GetCapabilities getCapabilities) {
        this.getCapabilities = getCapabilities;
    }

    /** Establishes the operation bean responsible for executing the DescribeLayer requests */
    public void setDescribeLayer(DescribeLayer describeLayer) {
        this.describeLayer = describeLayer;
    }

    /** Establishes the operation bean responsible for executing the GetMap requests */
    public void setGetMap(GetMap getMap) {
        this.getMap = getMap;
    }

    /** Establishes the operation bean responsible for executing the GetFeatureInfo requests */
    public void setGetFeatureInfo(GetFeatureInfo getFeatureInfo) {
        this.getFeatureInfo = getFeatureInfo;
    }

    /** Establishes the operation bean responsible for executing the GetStyles requests */
    public void setGetStyles(GetStyles getStyles) {
        this.getStyles = getStyles;
    }

    /** Establishes the operation bean responsible for executing the GetLegendGraphics requests */
    public void setGetLegendGraphic(GetLegendGraphic getLegendGraphic) {
        this.getLegendGraphic = getLegendGraphic;
    }

    /** @see ApplicationContextAware#setApplicationContext(ApplicationContext) */
    @Override
    @SuppressFBWarnings("LI_LAZY_INIT_STATIC") // method is not called by multiple threads
    public void setApplicationContext(ApplicationContext context) throws BeansException {

        // first time initialization of line width optimization flag
        if (OPTIMIZE_LINE_WIDTH == null) {
            String enabled = GeoServerExtensions.getProperty("OPTIMIZE_LINE_WIDTH", context);
            // default to true, but allow switching off
            if (enabled == null) OPTIMIZE_LINE_WIDTH = false;
            else OPTIMIZE_LINE_WIDTH = Boolean.valueOf(enabled);
        }

        // initialization of the renderer choice flag
        if (MAX_FILTER_RULES == null) {
            String rules = GeoServerExtensions.getProperty("MAX_FILTER_RULES", context);
            // default to true, but allow switching off
            if (rules == null) MAX_FILTER_RULES = 20;
            else MAX_FILTER_RULES = Integer.valueOf(rules);
        }

        // control usage of the global rendering thread pool
        if (USE_GLOBAL_RENDERING_POOL == null) {
            String usePool = GeoServerExtensions.getProperty("USE_GLOBAL_RENDERING_POOL", context);
            // default to true, but allow switching off
            if (usePool == null) USE_GLOBAL_RENDERING_POOL = true;
            else USE_GLOBAL_RENDERING_POOL = Boolean.valueOf(usePool);
        }
    }

    /**
     * Checks wheter the line width optimization is enabled, or not (defaults to true unless the user sets the
     * OPTIMIZE_LINE_WIDTH property to false)
     */
    public static boolean isLineWidthOptimizationEnabled() {
        return OPTIMIZE_LINE_WIDTH;
    }

    /**
     * If true (default) use the sld rule filters to compose the query to the DB, otherwise don't and get down only with
     * the bbox and eventual definition filter)
     */
    public static int getMaxFilterRules() {
        return MAX_FILTER_RULES;
    }

    /** If true (default) the direct raster rendering path is enabled */
    public static boolean isDirectRasterPathEnabled() {
        return !BYPASS_DIRECT;
    }

    /**
     * @see WebMapService#getCapabilities(GetCapabilitiesRequest)
     * @see GetCapabilitiesTransformer
     * @see Capabilities_1_3_0_Transformer
     */
    @Override
    public TransformerBase getCapabilities(GetCapabilitiesRequest request) {
        if (null == getCapabilities) {
            throw new UnsupportedOperationException(
                    "Operation not properly configured, make sure the operation bean has been set");
        }
        return getCapabilities.run(request);
    }

    /** @see WebMapService#capabilities(GetCapabilitiesRequest) */
    @Override
    public TransformerBase capabilities(GetCapabilitiesRequest request) {
        return getCapabilities(request);
    }

    /** @see WebMapService#describeLayer(DescribeLayerRequest) */
    @Override
    public DescribeLayerModel describeLayer(DescribeLayerRequest request) {
        if (null == describeLayer) {
            throw new UnsupportedOperationException(
                    "Operation not properly configured, make sure the operation bean has been set");
        }
        return describeLayer.run(request);
    }

    /** @see WebMapService#getMap(GetMapRequest) */
    @Override
    public WebMap getMap(GetMapRequest request) {
        if (null == getMap) {
            throw new UnsupportedOperationException(
                    "Operation not properly configured, make sure the operation bean has been set");
        }
        return getMap.run(request);
    }

    /** @see WebMapService#map(GetMapRequest) */
    @Override
    public WebMap map(GetMapRequest request) {
        return getMap(request);
    }

    /** @see WebMapService#getFeatureInfo(GetFeatureInfoRequest) */
    @Override
    public FeatureCollectionType getFeatureInfo(final GetFeatureInfoRequest request) {
        if (null == getFeatureInfo) {
            throw new UnsupportedOperationException(
                    "Operation not properly configured, make sure the operation bean has been set");
        }
        return getFeatureInfo.run(request);
    }

    /** @see WebMapService#getLegendGraphic(GetLegendGraphicRequest) */
    @Override
    public Object getLegendGraphic(GetLegendGraphicRequest request) {
        if (null == getLegendGraphic) {
            throw new UnsupportedOperationException(
                    "Operation not properly configured, make sure the operation bean has been set");
        }
        return getLegendGraphic.run(request);
    }

    @Override
    public WebMap kml(GetMapRequest getMap) {
        throw new ServiceException("kml service is not available, please include a KML module in WEB-INF/lib");
    }

    /** @see WebMapService#reflect(GetMapRequest) */
    @Override
    public WebMap reflect(GetMapRequest request) {
        return getMapReflect(request);
    }

    /** @see org.geoserver.wms.WebMapService#getStyles(org.geoserver.sld.GetStylesRequest) */
    @Override
    public StyledLayerDescriptor getStyles(GetStylesRequest request) {
        return getStyles.run(request);
    }

    /**
     * s
     *
     * @see WebMapService#getMapReflect(GetMapRequest)
     */
    @Override
    public WebMap getMapReflect(GetMapRequest request) {

        GetMapRequest getMap = autoSetMissingProperties(request);

        return getMap(getMap);
    }

    public static GetMapRequest autoSetMissingProperties(GetMapRequest getMap) {
        return new GetMapDefaults().autoSetMissingProperties(getMap);
    }

    /**
     * This method tries to automatically determine SRS, bounding box and output size based on the layers provided by
     * the user and any other parameters.
     *
     * <p>If bounds are not specified by the user, they are automatically se to the union of the bounds of all layers.
     *
     * <p>The size of the output image defaults to 512 pixels, the height is automatically determined based on the width
     * to height ratio of the requested layers. This is also true if either height or width are specified by the user.
     * If both height and width are specified by the user, the automatically determined bounding box will be adjusted to
     * fit inside these bounds.
     *
     * <p>General idea 1) Figure out whether SRS has been specified, fall back to EPSG:4326 2) Determine whether all
     * requested layers use the same SRS, - if so, try to do bounding box calculations in native coordinates 3)
     * Aggregate the bounding boxes (in EPSG:4326 or native) 4a) If bounding box has been specified, adjust height of
     * image to match 4b) If bounding box has not been specified, but height has, adjust bounding box
     */
    public static void autoSetBoundsAndSize(GetMapRequest getMap) {
        new GetMapDefaults().autoSetBoundsAndSize(getMap);
    }

    /** Returns a app wide cached rendering pool that can be used for parallelized rendering */
    public static ExecutorService getRenderingPool() {
        if (USE_GLOBAL_RENDERING_POOL && RENDERING_POOL == null) {
            synchronized (DefaultWebMapService.class) {
                if (RENDERING_POOL == null) {
                    RENDERING_POOL = new ThreadLocalTransferExecutor();
                }
            }
        }

        return RENDERING_POOL;
    }

    @Override
    public void destroy() throws Exception {
        if (RENDERING_POOL != null) {
            RENDERING_POOL.shutdown();
            RENDERING_POOL.awaitTermination(10, TimeUnit.SECONDS);
            RENDERING_POOL = null;
        }
    }
}
