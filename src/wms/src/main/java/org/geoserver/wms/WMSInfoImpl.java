/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.util.GrowableInternationalString;
import org.opengis.util.InternationalString;

public class WMSInfoImpl extends ServiceInfoImpl implements WMSInfo {

    public static final int DEFAULT_REMOTE_STYLE_MAX_REQUEST_TIME = 60000;
    public static final int DEFAULT_REMOTE_STYLE_TIMEOUT = 30000;

    List<String> srs = new ArrayList<>();

    List<String> allowedURLsForAuthForwarding = new ArrayList<>();

    Boolean bboxForEachCRS;

    WatermarkInfo watermark = new WatermarkInfoImpl();

    WMSInterpolation interpolation = WMSInterpolation.Nearest;

    boolean getFeatureInfoMimeTypeCheckingEnabled;
    Set<String> getFeatureInfoMimeTypes = new HashSet<>();

    boolean getMapMimeTypeCheckingEnabled;
    Set<String> getMapMimeTypes = new HashSet<>();

    boolean dynamicStylingDisabled;

    // GetFeatureInfo result are reprojected by default
    private boolean featuresReprojectionDisabled = false;

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "authorityURLs", and a not transient in the 2.2.x series.
     *
     * @since 2.1.3
     */
    protected List<AuthorityURLInfo> authorityURLs = new ArrayList<>(2);

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "identifiers", and a not transient in the 2.2.x series.
     *
     * @since 2.1.3
     */
    protected List<LayerIdentifierInfo> identifiers = new ArrayList<>(2);

    int maxBuffer;

    int maxRequestMemory;

    int maxRenderingTime;

    int maxRenderingErrors;

    private String rootLayerTitle;

    private String rootLayerAbstract;

    private GrowableInternationalString internationalRootLayerTitle;

    private GrowableInternationalString internationalRootLayerAbstract;

    private Integer maxRequestedDimensionValues;

    private CacheConfiguration cacheConfiguration = new CacheConfiguration();

    private Integer remoteStyleMaxRequestTime;
    private Integer remoteStyleTimeout;

    private Boolean defaultGroupStyleEnabled;

    private boolean transformFeatureInfoDisabled;

    private boolean autoEscapeTemplateValues;

    public WMSInfoImpl() {
        authorityURLs = new ArrayList<>(2);
        identifiers = new ArrayList<>(2);
    }

    @Override
    public String getType() {
        return "WMS";
    }

    @Override
    public int getMaxRequestMemory() {
        return maxRequestMemory;
    }

    @Override
    public void setMaxRequestMemory(int maxRequestMemory) {
        this.maxRequestMemory = maxRequestMemory;
    }

    @Override
    public WatermarkInfo getWatermark() {
        return watermark;
    }

    @Override
    public void setWatermark(WatermarkInfo watermark) {
        this.watermark = watermark;
    }

    @Override
    public void setInterpolation(WMSInterpolation interpolation) {
        this.interpolation = interpolation;
    }

    @Override
    public WMSInterpolation getInterpolation() {
        return interpolation;
    }

    @Override
    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    @Override
    public Boolean isBBOXForEachCRS() {
        if (bboxForEachCRS != null) {
            return bboxForEachCRS;
        }

        // check the metadata map if upgrading from 2.1.x
        Boolean bool = getMetadata().get("bboxForEachCRS", Boolean.class);
        return bool != null && bool;
    }

    @Override
    public void setBBOXForEachCRS(Boolean bboxForEachCRS) {
        this.bboxForEachCRS = bboxForEachCRS;
    }

    @Override
    public int getMaxBuffer() {
        return maxBuffer;
    }

    @Override
    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    @Override
    public int getMaxRenderingTime() {
        return maxRenderingTime;
    }

    @Override
    public void setMaxRenderingTime(int maxRenderingTime) {
        this.maxRenderingTime = maxRenderingTime;
    }

    @Override
    public int getMaxRenderingErrors() {
        return maxRenderingErrors;
    }

    @Override
    public void setMaxRenderingErrors(int maxRenderingErrors) {
        this.maxRenderingErrors = maxRenderingErrors;
    }

    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return authorityURLs;
    }

    public void setAuthorityURLs(List<AuthorityURLInfo> urls) {
        this.authorityURLs = urls;
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<LayerIdentifierInfo> identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public Set<String> getGetFeatureInfoMimeTypes() {
        return getFeatureInfoMimeTypes;
    }

    public void setGetFeatureInfoMimeTypes(Set<String> getFeatureInfoMimeTypes) {
        this.getFeatureInfoMimeTypes = getFeatureInfoMimeTypes;
    }

    @Override
    public Set<String> getGetMapMimeTypes() {
        return getMapMimeTypes;
    }

    public void setGetMapMimeTypes(Set<String> getMapMimeTypes) {
        this.getMapMimeTypes = getMapMimeTypes;
    }

    @Override
    public boolean isGetFeatureInfoMimeTypeCheckingEnabled() {
        return getFeatureInfoMimeTypeCheckingEnabled;
    }

    @Override
    public void setGetFeatureInfoMimeTypeCheckingEnabled(
            boolean getFeatureInfoMimeTypeCheckingEnabled) {
        this.getFeatureInfoMimeTypeCheckingEnabled = getFeatureInfoMimeTypeCheckingEnabled;
    }

    @Override
    public boolean isGetMapMimeTypeCheckingEnabled() {
        return getMapMimeTypeCheckingEnabled;
    }

    @Override
    public void setGetMapMimeTypeCheckingEnabled(boolean getMapMimeTypeCheckingEnabled) {
        this.getMapMimeTypeCheckingEnabled = getMapMimeTypeCheckingEnabled;
    }

    @Override
    public String getRootLayerTitle() {
        return InternationalStringUtils.getOrDefault(rootLayerTitle, internationalRootLayerTitle);
    }

    @Override
    public void setRootLayerTitle(String rootLayerTitle) {
        this.rootLayerTitle = rootLayerTitle;
    }

    @Override
    public String getRootLayerAbstract() {
        return InternationalStringUtils.getOrDefault(
                rootLayerAbstract, internationalRootLayerAbstract);
    }

    @Override
    public void setRootLayerAbstract(String rootLayerAbstract) {
        this.rootLayerAbstract = rootLayerAbstract;
    }

    /** Sets the status of dynamic styling (SLD and SLD_BODY params) allowance */
    @Override
    public void setDynamicStylingDisabled(Boolean dynamicStylingDisabled) {
        this.dynamicStylingDisabled = dynamicStylingDisabled;
    }

    /** @return the status of dynamic styling (SLD and SLD_BODY params) allowance */
    @Override
    public Boolean isDynamicStylingDisabled() {
        return dynamicStylingDisabled;
    }

    @Override
    public boolean isFeaturesReprojectionDisabled() {
        return featuresReprojectionDisabled;
    }

    @Override
    public void setFeaturesReprojectionDisabled(boolean featuresReprojectionDisabled) {
        this.featuresReprojectionDisabled = featuresReprojectionDisabled;
    }

    @Override
    public int getMaxRequestedDimensionValues() {
        return maxRequestedDimensionValues == null
                ? DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES
                : maxRequestedDimensionValues;
    }

    @Override
    public void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        this.maxRequestedDimensionValues = maxRequestedDimensionValues;
    }

    @Override
    public CacheConfiguration getCacheConfiguration() {
        if (cacheConfiguration == null) {
            cacheConfiguration = new CacheConfiguration();
        }
        return cacheConfiguration;
    }

    @Override
    public void setCacheConfiguration(CacheConfiguration cacheCfg) {
        this.cacheConfiguration = cacheCfg;
    }

    @Override
    public int getRemoteStyleMaxRequestTime() {
        return remoteStyleMaxRequestTime != null
                ? remoteStyleMaxRequestTime
                : DEFAULT_REMOTE_STYLE_MAX_REQUEST_TIME;
    }

    @Override
    public void setRemoteStyleMaxRequestTime(int remoteStyleMaxRequestTime) {
        this.remoteStyleMaxRequestTime = remoteStyleMaxRequestTime;
    }

    @Override
    public int getRemoteStyleTimeout() {
        return remoteStyleTimeout != null ? remoteStyleTimeout : DEFAULT_REMOTE_STYLE_TIMEOUT;
    }

    @Override
    public void setRemoteStyleTimeout(int remoteStyleTimeout) {
        this.remoteStyleTimeout = remoteStyleTimeout;
    }

    @Override
    public boolean isDefaultGroupStyleEnabled() {
        if (defaultGroupStyleEnabled == null) return true;
        return defaultGroupStyleEnabled.booleanValue();
    }

    @Override
    public void setDefaultGroupStyleEnabled(boolean defaultGroupStyleEnabled) {
        this.defaultGroupStyleEnabled = defaultGroupStyleEnabled;
    }

    @Override
    public GrowableInternationalString getInternationalRootLayerTitle() {
        return internationalRootLayerTitle;
    }

    @Override
    public void setInternationalRootLayerTitle(InternationalString rootLayerTitle) {
        this.internationalRootLayerTitle = InternationalStringUtils.growable(rootLayerTitle);
    }

    @Override
    public GrowableInternationalString getInternationalRootLayerAbstract() {
        return this.internationalRootLayerAbstract;
    }

    @Override
    public void setInternationalRootLayerAbstract(InternationalString rootLayerAbstract) {
        this.internationalRootLayerAbstract = InternationalStringUtils.growable(rootLayerAbstract);
    }

    @Override
    public List<String> getAllowedURLsForAuthForwarding() {
        if (allowedURLsForAuthForwarding == null) {
            allowedURLsForAuthForwarding = new ArrayList<>();
        }
        return allowedURLsForAuthForwarding;
    }

    public void setAllowedURLsForAuthForwarding(List<String> allowedURLsForAuthForwarding) {
        this.allowedURLsForAuthForwarding = allowedURLsForAuthForwarding;
    }

    @Override
    public boolean isTransformFeatureInfoDisabled() {
        return transformFeatureInfoDisabled;
    }

    @Override
    public void setTransformFeatureInfoDisabled(boolean transformFeatureInfoDisabled) {
        this.transformFeatureInfoDisabled = transformFeatureInfoDisabled;
    }

    @Override
    public boolean isAutoEscapeTemplateValues() {
        return autoEscapeTemplateValues;
    }

    @Override
    public void setAutoEscapeTemplateValues(boolean autoEscapeTemplateValues) {
        this.autoEscapeTemplateValues = autoEscapeTemplateValues;
    }
}
