/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.geometry;

/**
 *
 * @author Juan Marin - OpenGeo
 *
 */
public interface Geometry {

    GeometryTypeEnum getGeometryType();

    // protected GeometryTypeEnum geometryType;
    //
    // public GeometryTypeEnum getGeometryType() {
    // return geometryType;
    // }
    //
    // public void setGeometryType(GeometryTypeEnum geometryType) {
    // this.geometryType = geometryType;
    // }
}
