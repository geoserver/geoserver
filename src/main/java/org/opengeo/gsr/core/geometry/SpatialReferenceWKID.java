package org.opengeo.gsr.core.geometry;

import com.thoughtworks.xstream.annotations.XStreamAlias;


/**
 * 
 * @author Juan Marin - OpenGeo
 *
 */

@XStreamAlias(value = "")
public class SpatialReferenceWKID extends SpatialReference {

    private int wkid;

    public int getWkid() {
        return wkid;
    }

    public void setWkid(int wkid) {
        this.wkid = wkid;
    }

    public SpatialReferenceWKID(int wkid) {
        this.wkid = wkid;
    }
}
