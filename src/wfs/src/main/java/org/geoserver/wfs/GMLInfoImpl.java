/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Optional;

public class GMLInfoImpl implements GMLInfo {

    SrsNameStyle srsNameStyle = SrsNameStyle.NORMAL;
    Boolean overrideGMLAttributes;

    private String mimeTypeToForce;

    public SrsNameStyle getSrsNameStyle() {
        return srsNameStyle;
    }

    public void setSrsNameStyle(SrsNameStyle srsNameStyle) {
        this.srsNameStyle = srsNameStyle;
    }

    public Boolean getOverrideGMLAttributes() {
        return overrideGMLAttributes;
    }

    public void setOverrideGMLAttributes(Boolean overrideGMLAttributes) {
        this.overrideGMLAttributes = overrideGMLAttributes;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((srsNameStyle == null) ? 0 : srsNameStyle.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof GMLInfo)) return false;

        final GMLInfo other = (GMLInfo) obj;
        if (srsNameStyle == null) {
            if (other.getSrsNameStyle() != null) return false;
        } else if (!srsNameStyle.equals(other.getSrsNameStyle())) return false;
        return true;
    }

    @Override
    public Optional<String> getMimeTypeToForce() {
        return Optional.ofNullable(mimeTypeToForce);
    }

    @Override
    public void setMimeTypeToForce(String mimeTypeToForce) {
        this.mimeTypeToForce = mimeTypeToForce;
    }
}
