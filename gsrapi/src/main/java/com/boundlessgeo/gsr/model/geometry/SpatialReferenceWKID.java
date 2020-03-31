/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;



/**
 *
 * @author Juan Marin - OpenGeo
 *
 */

public class SpatialReferenceWKID implements SpatialReference {

    private int wkid;

    private int latestWkid;

    public int getWkid() {
        return wkid;
    }

    public void setWkid(int wkid) {
        this.wkid = wkid;
    }

    public SpatialReferenceWKID(int wkid) {
        this.wkid = wkid;
        this.latestWkid = wkid;
    }

    public int getLatestWkid() {
        return latestWkid;
    }

    public void setLatestWkid(int latestWkid) {
        this.latestWkid = latestWkid;
    }
}
