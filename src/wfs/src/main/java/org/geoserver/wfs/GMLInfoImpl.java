/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

public class GMLInfoImpl implements GMLInfo {

    SrsNameStyle srsNameStyle = SrsNameStyle.NORMAL;
    Boolean overrideGMLAttributes;
    
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
        result = prime * result
                + ((srsNameStyle == null) ? 0 : srsNameStyle.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!( obj instanceof GMLInfo)) 
            return false;
        
        final GMLInfo other = (GMLInfo) obj;
        if (srsNameStyle == null) {
            if (other.getSrsNameStyle() != null)
                return false;
        } else if (!srsNameStyle.equals(other.getSrsNameStyle()))
            return false;
        return true;
    }

    
}
