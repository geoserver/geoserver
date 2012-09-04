/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;



/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */

public class SpatialReferenceWKID implements SpatialReference {

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
