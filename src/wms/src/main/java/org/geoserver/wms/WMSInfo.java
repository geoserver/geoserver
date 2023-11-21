/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.ServiceInfo;
import org.geotools.api.util.InternationalString;
import org.geotools.util.GrowableInternationalString;

/**
 * Configuration object for Web Map Service.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WMSInfo extends ServiceInfo {

    public static final String EXCEPTION_ON_INVALID_DIMENSION_KEY =
            "org.geoserver.wms.exceptionOnInvalidDimension";

    /** Default value for the exceptionOnInvalidDimension */
    public static final boolean EXCEPTION_ON_INVALID_DIMENSION_DEFAULT =
            Boolean.valueOf(System.getProperty(EXCEPTION_ON_INVALID_DIMENSION_KEY, "false"));

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

    /** Returns the remote style max request time in milliseconds */
    int getRemoteStyleMaxRequestTime();

    /** Sets the remote style max request time in milliseconds */
    void setRemoteStyleMaxRequestTime(int time);

    /** Returns the remote SLD Urls that are allowed for authorization forwarding */
    default List<String> getAllowedURLsForAuthForwarding() {
        return Collections.emptyList();
    }

    /** Returns the remote style timeout in milliseconds * */
    int getRemoteStyleTimeout();

    /** Sets the remote style timeout in milliseconds * */
    void setRemoteStyleTimeout(int time);

    default boolean isDefaultGroupStyleEnabled() {
        return true;
    }

    default void setDefaultGroupStyleEnabled(boolean defaultGroupStyleEnabled) {
        // if not implemented nothing to do
    }

    /** @return the international title of the root layer */
    GrowableInternationalString getInternationalRootLayerTitle();

    /** Sets the international title of the root layer */
    void setInternationalRootLayerTitle(InternationalString rootLayerTitle);

    /** @return the international abstract of the root layer */
    GrowableInternationalString getInternationalRootLayerAbstract();

    /** Sets the international title of the root layer */
    void setInternationalRootLayerAbstract(InternationalString rootLayerAbstract);

    /** @return whether to apply rendering transformations for WMS GetFeatureInfo requests */
    boolean isTransformFeatureInfoDisabled();

    /** Sets whether to apply rendering transformations for WMS GetFeatureInfo requests */
    void setTransformFeatureInfoDisabled(boolean transformFeatureInfoDisabled);

    /** @return whether to enable auto-escaping HTML FreeMarker template values */
    boolean isAutoEscapeTemplateValues();

    /** Sets whether to enable auto-escaping HTML FreeMarker template values */
    void setAutoEscapeTemplateValues(boolean autoEscapeTemplateValues);

    /**
     * Same as {@link #isExceptionOnInvalidDimension()}, but in case of null, uses the default value
     * for the field which can be overrideen using EXCEPTION_ON_INVALID_DIMENSION_DEFAULT
     *
     * @return
     */
    public default boolean exceptionOnInvalidDimension() {
        return Optional.ofNullable(isExceptionOnInvalidDimension())
                .orElse(EXCEPTION_ON_INVALID_DIMENSION_DEFAULT);
    }

    /**
     * This property controls the behavior when an invalid dimension is encountered. If set to
     * <code>true</code>, an <code>InvalidDimensionException</code> will be thrown. If set to <code>
     * false</code>, an empty response will be used. The standard compliant behavior is obtained
     * with <code>true</code>.
     *
     * @return true if an exception should be thrown on invalid dimension, false otherwise
     */
    Boolean isExceptionOnInvalidDimension();

    /**
     * Sets the behavior when an invalid dimension is encountered.
     *
     * @param exceptionOnInvalidDimension true if an exception should be thrown on invalid dimension
     *     value, false otherwise
     */
    void setExceptionOnInvalidDimension(Boolean exceptionOnInvalidDimension);
}
