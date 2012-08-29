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
public enum GeometryType {

    GeometryPoint("GeometryPoint"), GeometryMultiPoint("GeometryMultiPoint"), GeometryPolyline("GeometryPolyline"), GeometryPolygon(
            "GeometryPolygon"), GeometryEnvelope("GeometryEnvelope");
    private String geometryType;

    public String getGeometryType() {
        return geometryType;
    }

    private GeometryType(String geomType) {
        this.geometryType = geomType;
    }

};