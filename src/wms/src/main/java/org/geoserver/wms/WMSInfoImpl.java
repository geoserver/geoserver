/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.impl.ServiceInfoImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WMSInfoImpl extends ServiceInfoImpl implements WMSInfo {

    List<String> srs = new ArrayList<String>();

    Boolean bboxForEachCRS;

    WatermarkInfo watermark = new WatermarkInfoImpl();

    WMSInterpolation interpolation = WMSInterpolation.Nearest;
    
    
    boolean getFeatureInfoMimeTypeCheckingEnabled;
    Set<String> getFeatureInfoMimeTypes = new HashSet<String>();
    
    boolean getMapMimeTypeCheckingEnabled;
    Set<String> getMapMimeTypes = new HashSet<String>();
    
    boolean dynamicStylingDisabled;

    // GetFeatureInfo result are reprojected by default
    private boolean featuresReprojectionDisabled = false;

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "authorityURLs", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected List<AuthorityURLInfo> authorityURLs = new ArrayList<AuthorityURLInfo>(2);
    

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "identifiers", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected List<LayerIdentifierInfo> identifiers = new ArrayList<LayerIdentifierInfo>(2);

    int maxBuffer;

    int maxRequestMemory;

    int maxRenderingTime;

    int maxRenderingErrors;

    private String capabilitiesErrorHandling;    
    
    private String rootLayerTitle;
    
    private String rootLayerAbstract;
    
    private Integer maxRequestedDimensionValues;

    public WMSInfoImpl() {
        authorityURLs = new ArrayList<AuthorityURLInfo>(2);
        identifiers = new ArrayList<LayerIdentifierInfo>(2);
    }

    public int getMaxRequestMemory() {
        return maxRequestMemory;
    }

    public void setMaxRequestMemory(int maxRequestMemory) {
        this.maxRequestMemory = maxRequestMemory;
    }

    public WatermarkInfo getWatermark() {
        return watermark;
    }

    public void setWatermark(WatermarkInfo watermark) {
        this.watermark = watermark;
    }

    public void setInterpolation(WMSInterpolation interpolation) {
        this.interpolation = interpolation;
    }

    public WMSInterpolation getInterpolation() {
        return interpolation;
    }

    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    public Boolean isBBOXForEachCRS() {
        if (bboxForEachCRS != null) {
            return bboxForEachCRS;
        }
        
        //check the metadata map if upgrading from 2.1.x
        Boolean bool = getMetadata().get("bboxForEachCRS", Boolean.class);
        return bool != null && bool;
    }

    public void setBBOXForEachCRS(Boolean bboxForEachCRS) {
        this.bboxForEachCRS = bboxForEachCRS;
    }

    public int getMaxBuffer() {
        return maxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    public int getMaxRenderingTime() {
        return maxRenderingTime;
    }

    public void setMaxRenderingTime(int maxRenderingTime) {
        this.maxRenderingTime = maxRenderingTime;
    }

    public int getMaxRenderingErrors() {
        return maxRenderingErrors;
    }

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

    public Set<String> getGetFeatureInfoMimeTypes() {
        return getFeatureInfoMimeTypes;
    }

    public void setGetFeatureInfoMimeTypes(Set<String> getFeatureInfoMimeTypes) {
        this.getFeatureInfoMimeTypes = getFeatureInfoMimeTypes;
    }

    public Set<String> getGetMapMimeTypes() {
        return getMapMimeTypes;
    }

    public void setGetMapMimeTypes(Set<String> getMapMimeTypes) {
        this.getMapMimeTypes = getMapMimeTypes;
    }

    public boolean isGetFeatureInfoMimeTypeCheckingEnabled() {
        return getFeatureInfoMimeTypeCheckingEnabled;
    }

    public void setGetFeatureInfoMimeTypeCheckingEnabled(boolean getFeatureInfoMimeTypeCheckingEnabled) {
        this.getFeatureInfoMimeTypeCheckingEnabled = getFeatureInfoMimeTypeCheckingEnabled;
    }

    public boolean isGetMapMimeTypeCheckingEnabled() {
        return getMapMimeTypeCheckingEnabled;
    }

    public void setGetMapMimeTypeCheckingEnabled(boolean getMapMimeTypeCheckingEnabled) {
        this.getMapMimeTypeCheckingEnabled = getMapMimeTypeCheckingEnabled;
    }

	public String getRootLayerTitle() {
		return rootLayerTitle;
	}

	public void setRootLayerTitle(String rootLayerTitle) {
		this.rootLayerTitle = rootLayerTitle;
	}

	public String getRootLayerAbstract() {
		return rootLayerAbstract;
	}

	public void setRootLayerAbstract(String rootLayerAbstract) {
		this.rootLayerAbstract = rootLayerAbstract;
	}
    
    /**
     * Sets the status of dynamic styling (SLD and SLD_BODY params) allowance
     *
     * @param dynamicStylingDisabled
     */
    @Override
    public void setDynamicStylingDisabled(Boolean dynamicStylingDisabled) {
        this.dynamicStylingDisabled= dynamicStylingDisabled;
    }

    /**
     * @return the status of dynamic styling (SLD and SLD_BODY params) allowance
     */
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

    public int getMaxRequestedDimensionValues() {
        return maxRequestedDimensionValues == null
                ? DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES
                : maxRequestedDimensionValues;
    }

    public void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        this.maxRequestedDimensionValues = maxRequestedDimensionValues;
    }
}
