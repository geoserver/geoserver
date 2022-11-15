/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geotools.coverage.grid.io.OverviewPolicy;

/**
 * Default implementation for the {@link WCSInfo} bean.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class WCSInfoImpl extends ServiceInfoImpl implements WCSInfo {

    private static final long serialVersionUID = 3721044439071286273L;

    List<String> srs = new ArrayList<>();

    boolean gmlPrefixing;

    private boolean latLon = false;

    long maxInputMemory = -1;

    long maxOutputMemory = -1;

    Integer defaultDeflateCompressionLevel = WCSInfo.DEFAULT_DEFLATE_COMPRESSION_LEVEL;

    Boolean subsamplingEnabled = Boolean.TRUE;

    OverviewPolicy overviewPolicy;

    Integer maxRequestedDimensionValues;

    public WCSInfoImpl() {}

    @Override
    public String getType() {
        return "WCS";
    }

    @Override
    public boolean isGMLPrefixing() {
        return gmlPrefixing;
    }

    @Override
    public void setGMLPrefixing(boolean gmlPrefixing) {
        this.gmlPrefixing = gmlPrefixing;
    }

    @Override
    public long getMaxInputMemory() {
        return maxInputMemory;
    }

    @Override
    public void setMaxInputMemory(long maxInputSize) {
        this.maxInputMemory = maxInputSize;
    }

    @Override
    public long getMaxOutputMemory() {
        return maxOutputMemory;
    }

    @Override
    public void setMaxOutputMemory(long maxOutputSize) {
        this.maxOutputMemory = maxOutputSize;
    }

    @Override
    public boolean isSubsamplingEnabled() {
        return subsamplingEnabled == null ? true : subsamplingEnabled;
    }

    @Override
    public void setSubsamplingEnabled(boolean subsamplingEnabled) {
        this.subsamplingEnabled = subsamplingEnabled;
    }

    @Override
    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy == null ? OverviewPolicy.IGNORE : overviewPolicy;
    }

    @Override
    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        this.overviewPolicy = overviewPolicy;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public void setLatLon(boolean latLon) {
        this.latLon = latLon;
    }

    @Override
    public boolean isLatLon() {
        return latLon;
    }

    @Override
    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (gmlPrefixing ? 1231 : 1237);
        result = prime * result + (latLon ? 1231 : 1237);
        result = prime * result + (int) (maxInputMemory ^ (maxInputMemory >>> 32));
        result = prime * result + (int) (maxOutputMemory ^ (maxOutputMemory >>> 32));
        result = prime * result + ((overviewPolicy == null) ? 0 : overviewPolicy.hashCode());
        result = prime * result + ((srs == null) ? 0 : srs.hashCode());
        result =
                prime * result + ((subsamplingEnabled == null) ? 0 : subsamplingEnabled.hashCode());
        result =
                prime * result
                        + ((maxRequestedDimensionValues == null)
                                ? 0
                                : maxRequestedDimensionValues.hashCode());
        result =
                prime * result
                        + ((defaultDeflateCompressionLevel == null)
                                ? 0
                                : defaultDeflateCompressionLevel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WCSInfoImpl other = (WCSInfoImpl) obj;
        if (gmlPrefixing != other.gmlPrefixing) return false;
        if (latLon != other.latLon) return false;
        if (maxInputMemory != other.maxInputMemory) return false;
        if (maxOutputMemory != other.maxOutputMemory) return false;
        if (overviewPolicy != other.overviewPolicy) return false;
        if (srs == null) {
            if (other.srs != null) return false;
        } else if (!srs.equals(other.srs)) return false;
        if (subsamplingEnabled == null) {
            if (other.subsamplingEnabled != null) return false;
        } else if (!subsamplingEnabled.equals(other.subsamplingEnabled)) return false;
        if (maxRequestedDimensionValues == null) {
            if (other.maxRequestedDimensionValues != null) return false;
        } else if (!maxRequestedDimensionValues.equals(other.maxRequestedDimensionValues))
            return false;
        if (defaultDeflateCompressionLevel == null) {
            if (other.defaultDeflateCompressionLevel != null) return false;
        } else if (!defaultDeflateCompressionLevel.equals(other.defaultDeflateCompressionLevel))
            return false;

        return true;
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
    public void setDefaultDeflateCompressionLevel(int defaultDeflateCompressionLevel) {
        this.defaultDeflateCompressionLevel = defaultDeflateCompressionLevel;
    }

    @Override
    public int getDefaultDeflateCompressionLevel() {
        return defaultDeflateCompressionLevel == null
                ? DEFAULT_DEFLATE_COMPRESSION_LEVEL
                : defaultDeflateCompressionLevel;
    }
}
