/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

/**
 * @author Juan Marin - OpenGeo
 */
public enum GeometryTypeEnum {

    POINT("esriGeometryPoint"),
    MULTIPOINT("esriGeometryMultiPoint"),
    POLYLINE("esriGeometryPolyline"),
    POLYGON("esriGeometryPolygon"),
    ENVELOPE("esriGeometryEnvelope");
    
    private final String geometryType;

    public String getGeometryType() {
        return geometryType;
    }

    private GeometryTypeEnum(String geomType) {
        this.geometryType = geomType;
    }

    public static GeometryTypeEnum forJTSClass(Class<?> jtsClass) {
        if (jtsClass.equals(com.vividsolutions.jts.geom.Point.class)) {
            return POINT;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.MultiPoint.class)) {
            return MULTIPOINT;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.MultiLineString.class)) {
            return POLYLINE;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.Polygon.class)) {
            return POLYGON;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.MultiPolygon.class)) {
            return POLYGON;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.Geometry.class)) {
            return POLYGON;
        } else if (jtsClass.equals(com.vividsolutions.jts.geom.Envelope.class)) {
            return ENVELOPE;
        } else {
            throw new UnsupportedOperationException("No GeoServices Geometry equivalent known for " + jtsClass);
        }
    }
};
