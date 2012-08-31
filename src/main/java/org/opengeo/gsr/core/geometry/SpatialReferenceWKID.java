/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
