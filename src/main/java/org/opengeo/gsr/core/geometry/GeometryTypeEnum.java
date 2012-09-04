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
public enum GeometryTypeEnum {

    POINT("GeometryPoint"), MULTIPOINT("GeometryMultiPoint"), POLYLINE("GeometryPolyline"), POLYGON(
            "GeometryPolygon"), ENVELOPE("GeometryEnvelope");
    private final String geometryType;

    public String getGeometryType() {
        return geometryType;
    }

    private GeometryTypeEnum(String geomType) {
        this.geometryType = geomType;
    }

};