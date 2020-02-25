/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;
import java.util.Set;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.ServiceInfo;

/**
 * Configuration object for Web Map Service.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WMSInfo extends ServiceInfo {

    enum WMSInterpolation {
        Nearest,
        Bilinear,
        Bicubic
    }

    /** The watermarking configuration. */
    WatermarkInfo getWatermark();

    /** Sets the watermarking configuration. */
    void setWatermark(WatermarkInfo watermark);

    WMSInterpolation getInterpolation();

    void setInterpolation(WMSInterpolation interpolation);

    /** The srs's that the wms service supports. */
    List<String> getSRS();

    /**
     * A set of mime types allowed for a getMap request. Active if {@link
     * #isGetMapMimeTypeCheckingEnabled()} returns <code>true</code>
     */
    Set<String> getGetMapMimeTypes();

    boolean isGetMapMimeTypeCheckingEnabled();

    void setGetMapMimeTypeCheckingEnabled(boolean getMapMimeTypeCheckingEnabled);

    /**
     * A set of mime types allowed for a getFeatureInfo request. Active if {@link
     * #isGetFeatureInfoMimeTypeCheckingEnabled()} returns <code>true</code>
     */
    Set<String> getGetFeatureInfoMimeTypes();

    boolean isGetFeatureInfoMimeTypeCheckingEnabled();

    void setGetFeatureInfoMimeTypeCheckingEnabled(boolean getFeatureInfoMimeTypeCheckingEnabled);

    /**
     * Flag controlling whether the WMS service, for each layer, should declare a bounding box for
     * every CRS supported, in it's capabilities document.
     *
     * <p>By default the number of CRS's supported is huge which does not make this option
     * practical. This flag is only respected in cases there {@link #getSRS()} is non empty.
     */
    Boolean isBBOXForEachCRS();

    /**
     * Sets flag controlling whether the WMS service, for each layer, should declare a bounding box
     * for every CRS supported.
     *
     * @see #isBBOXForEachCRS()
     */
    void setBBOXForEachCRS(Boolean bboxForEachCRS);

    /** The maximum search radius for GetFeatureInfo */
    int getMaxBuffer();

    /**
     * Sets the maximum search radius for GetFeatureInfo (if 0 or negative no maximum is enforced)
     */
    void setMaxBuffer(int buffer);

    /**
     * Returns the max amount of memory, in kilobytes, that each WMS request can allocate (each
     * output format will make a best effort attempt to respect it, but there are no guarantees)
     *
     * @return the limit, or 0 if no limit
     */
    int getMaxRequestMemory();

    /**
     * Sets the max amount of memory, in kilobytes, that each WMS request can allocate. Set it to 0
     * if no limit is desired.
     */
    void setMaxRequestMemory(int max);

    /**
     * The max time, in seconds, a WMS request is allowed to spend rendering the map. Various output
     * formats will do a best effort to respect it (raster formats, for example, will account just
     * rendering time, but not image encoding time)
     */
    int getMaxRenderingTime();

    /** Sets the max allowed rendering time, in seconds */
    void setMaxRenderingTime(int maxRenderingTime);

    /**
     * The max number of rendering errors that will be tolerated before stating the rendering
     * operation failed by throwing a service exception back to the client
     */
    int getMaxRenderingErrors();

    /** Sets the max number of rendering errors tolerated */
    void setMaxRenderingErrors(int maxRenderingTime);

    /**
     * Defines the list of authority URLs for the root WMS layer
     *
     * @return the list of WMS root layer's authority URLs
     */
    List<AuthorityURLInfo> getAuthorityURLs();

    /** @return the list of identifiers for the WMS root layer */
    List<LayerIdentifierInfo> getIdentifiers();

    /** @return the title of the root layer */
    String getRootLayerTitle();

    /** Sets the title of the root layer */
    void setRootLayerTitle(String rootLayerTitle);

    /** @return the abstract of the root layer */
    public String getRootLayerAbstract();

    /** Sets the abstract of the root layer */
    public void setRootLayerAbstract(String rootLayerAbstract);

    /** Sets the status of dynamic styling (SLD and SLD_BODY params) allowance */
    void setDynamicStylingDisabled(Boolean dynamicStylesDisabled);

    /** @return the status of dynamic styling (SLD and SLD_BODY params) allowance */
    Boolean isDynamicStylingDisabled();

    /**
     * If set to TRUE GetFeatureInfo results will NOT be reprojected.
     *
     * @param featuresReprojectionDisabled features reprojection allowance
     */
    default void setFeaturesReprojectionDisabled(boolean featuresReprojectionDisabled) {
        // if not implemented nothing is done
    }

    /**
     * Flag that controls if GetFeatureInfo results should NOT be reprojected to the map coordinate
     * reference system.
     *
     * @return GetFeatureInfo features reprojection allowance
     */
    default boolean isFeaturesReprojectionDisabled() {
        // deactivate features reprojection by default
        return true;
    }

    /**
     * Returns the maximum number of dimension items that can be requested by a client without
     * getting a service exception. The default is
     * {DimensionInfo#DEFAULT_MAX_REQUESTED_DIMENSION_VALUES} that is, no limit.
     */
    default int getMaxRequestedDimensionValues() {
        return DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES;
    }

    /**
     * Sets the maximum number of dimension items that can be requested by a client without. Zero or
     * negative will disable the limit.
     *
     * @param maxRequestedDimensionValues Any integer number
     */
    default void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        // if not implemented nothing is done
    }

    /** Returns WMS cache configuration for remote resources. */
    CacheConfiguration getCacheConfiguration();

    /** Set current WMS cache configuration for remote resources. */
    void setCacheConfiguration(CacheConfiguration cacheCfg);
}
