/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.geoserver.config.impl.ServiceInfoImpl;

public class WFSInfoImpl extends ServiceInfoImpl implements WFSInfo {

    protected Map<Version, GMLInfo> gml = new HashMap<>();
    protected ServiceLevel serviceLevel = ServiceLevel.COMPLETE;
    protected int maxFeatures = Integer.MAX_VALUE;
    protected boolean featureBounding = true;
    protected boolean canonicalSchemaLocation = false;
    protected boolean encodeFeatureMember = false;
    protected boolean hitsIgnoreMaxFeatures = false;
    protected boolean includeWFSRequestDumpFile = true;
    protected List<String> srs = new ArrayList<>();
    protected Boolean allowGlobalQueries = true;
    protected Boolean simpleConversionEnabled = false;

    public WFSInfoImpl() {}

    @Override
    public Map<Version, GMLInfo> getGML() {
        return gml;
    }

    public void setGML(Map<Version, GMLInfo> gml) {
        this.gml = gml;
    }

    @Override
    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    @Override
    public void setServiceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    @Override
    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    @Override
    public int getMaxFeatures() {
        return maxFeatures;
    }

    @Override
    public void setFeatureBounding(boolean featureBounding) {
        this.featureBounding = featureBounding;
    }

    @Override
    public boolean isFeatureBounding() {
        return featureBounding;
    }

    /** @see org.geoserver.wfs.WFSInfo#isCanonicalSchemaLocation() */
    @Override
    public boolean isCanonicalSchemaLocation() {
        return canonicalSchemaLocation;
    }

    /** @see org.geoserver.wfs.WFSInfo#setCanonicalSchemaLocation(boolean) */
    @Override
    public void setCanonicalSchemaLocation(boolean canonicalSchemaLocation) {
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }

    @Override
    public void setIncludeWFSRequestDumpFile(boolean includeWFSRequestDumpFile) {
        this.includeWFSRequestDumpFile = includeWFSRequestDumpFile;
    }

    @Override
    public boolean getIncludeWFSRequestDumpFile() {
        return includeWFSRequestDumpFile;
    }
    /*
     * @see org.geoserver.wfs.WFSInfo#isEncodingFeatureMember()
     */
    @Override
    public boolean isEncodeFeatureMember() {
        return this.encodeFeatureMember;
    }

    /*
     * @see org.geoserver.wfs.WFSInfo#setEncodeFeatureMember(java.lang.Boolean)
     */
    @Override
    public void setEncodeFeatureMember(boolean encodeFeatureMember) {
        this.encodeFeatureMember = encodeFeatureMember;
    }

    @Override
    public boolean isHitsIgnoreMaxFeatures() {
        return hitsIgnoreMaxFeatures;
    }

    @Override
    public void setHitsIgnoreMaxFeatures(boolean hitsIgnoreMaxFeatures) {
        this.hitsIgnoreMaxFeatures = hitsIgnoreMaxFeatures;
    }

    @Override
    public Integer getMaxNumberOfFeaturesForPreview() {
        Integer i = getMetadata().get("maxNumberOfFeaturesForPreview", Integer.class);
        return i != null ? i : 50;
    }

    @Override
    public void setMaxNumberOfFeaturesForPreview(Integer maxNumberOfFeaturesForPreview) {
        getMetadata().put("maxNumberOfFeaturesForPreview", maxNumberOfFeaturesForPreview);
    }

    @Override
    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    @Override
    public Boolean getAllowGlobalQueries() {
        return allowGlobalQueries == null ? Boolean.TRUE : allowGlobalQueries;
    }

    @Override
    public void setAllowGlobalQueries(Boolean allowGlobalQueries) {
        this.allowGlobalQueries = allowGlobalQueries;
    }

    @Override
    public boolean isSimpleConversionEnabled() {
        return simpleConversionEnabled == null ? false : simpleConversionEnabled;
    }

    @Override
    public void setSimpleConversionEnabled(boolean simpleConversionEnabled) {
        this.simpleConversionEnabled = simpleConversionEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (canonicalSchemaLocation ? 1231 : 1237);
        result = prime * result + (encodeFeatureMember ? 1231 : 1237);
        result = prime * result + (featureBounding ? 1231 : 1237);
        result = prime * result + ((gml == null) ? 0 : gml.hashCode());
        result = prime * result + (hitsIgnoreMaxFeatures ? 1231 : 1237);
        result = prime * result + maxFeatures;
        result = prime * result + (includeWFSRequestDumpFile ? 1231 : 1237);
        result = prime * result + ((serviceLevel == null) ? 0 : serviceLevel.hashCode());
        result = prime * result + ((srs == null) ? 0 : srs.hashCode());
        result = prime * result + (allowGlobalQueries == null ? 0 : allowGlobalQueries.hashCode());
        result =
                prime * result
                        + (simpleConversionEnabled == null
                                ? 0
                                : simpleConversionEnabled.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof WFSInfo)) return false;
        final WFSInfo other = (WFSInfo) obj;
        if (gml == null) {
            if (other.getGML() != null) return false;
        } else if (!gml.equals(other.getGML())) return false;
        if (maxFeatures != other.getMaxFeatures()) return false;
        if (featureBounding != other.isFeatureBounding()) return false;
        if (canonicalSchemaLocation != other.isCanonicalSchemaLocation()) return false;
        if (serviceLevel == null) {
            if (other.getServiceLevel() != null) return false;
        } else if (!serviceLevel.equals(other.getServiceLevel())) return false;
        if (encodeFeatureMember != other.isEncodeFeatureMember()) return false;
        if (hitsIgnoreMaxFeatures != other.isHitsIgnoreMaxFeatures()) return false;
        if (includeWFSRequestDumpFile != other.getIncludeWFSRequestDumpFile()) return false;
        if (srs == null) {
            if (other.getSRS() != null) return false;
        } else if (!srs.equals(other.getSRS())) return false;
        if (allowGlobalQueries == null && other.getAllowGlobalQueries() != null
                || !Objects.equals(allowGlobalQueries, other.getAllowGlobalQueries())) {
            return false;
        }
        return true;
    }
}
