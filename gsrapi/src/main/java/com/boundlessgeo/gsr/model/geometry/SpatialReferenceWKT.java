/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

/**
 *
 * @author Juan Marin - OpenGeo
 * @author Brett Antonides - LMN Solutions
 *
 */
public class SpatialReferenceWKT implements SpatialReference {
    private String wkt;

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    public SpatialReferenceWKT(String wkt) {
        this.wkt = wkt;
    }

}
