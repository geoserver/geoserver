/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import org.geoserver.config.impl.ServiceInfoImpl;
import org.geotools.coverage.grid.io.OverviewPolicy;
/**
 * Default implementation for the {@link WCSInfo} bean.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
@SuppressWarnings("unchecked")
public class WCSInfoImpl extends ServiceInfoImpl implements WCSInfo {

    private static final long serialVersionUID = 3721044439071286273L;

    boolean gmlPrefixing;
    
    long maxInputMemory = -1;
    
    long maxOutputMemory = -1;
    
    Boolean subsamplingEnabled = Boolean.TRUE;
    
    OverviewPolicy overviewPolicy;

    public WCSInfoImpl() {
    }

    public boolean isGMLPrefixing() {
        return gmlPrefixing;
    }

    public void setGMLPrefixing(boolean gmlPrefixing) {
        this.gmlPrefixing = gmlPrefixing;
    }

    public long getMaxInputMemory() {
        return maxInputMemory;
    }

    public void setMaxInputMemory(long maxInputSize) {
        this.maxInputMemory = maxInputSize;
    }

    public long getMaxOutputMemory() {
        return maxOutputMemory;
    }

    public void setMaxOutputMemory(long maxOutputSize) {
        this.maxOutputMemory = maxOutputSize;
    }

    public boolean isGmlPrefixing() {
        return gmlPrefixing;
    }

    public void setGmlPrefixing(boolean gmlPrefixing) {
        this.gmlPrefixing = gmlPrefixing;
    }

    public boolean isSubsamplingEnabled() {
        return subsamplingEnabled == null ? true : subsamplingEnabled; 
    }

    public void setSubsamplingEnabled(boolean subsamplingEnabled) {
        this.subsamplingEnabled = subsamplingEnabled;
    }

    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy == null ? OverviewPolicy.IGNORE : overviewPolicy;
    }

    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        this.overviewPolicy = overviewPolicy;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

}
