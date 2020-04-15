/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.geometry;

import com.fasterxml.jackson.annotation.JsonValue;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.opengis.feature.type.GeometryDescriptor;

/** @author Juan Marin - OpenGeo */
public enum GeometryTypeEnum {
    POINT("esriGeometryPoint"),
    MULTIPOINT("esriGeometryMultipoint"),
    POLYLINE("esriGeometryPolyline"),
    POLYGON("esriGeometryPolygon"),
    ENVELOPE("esriGeometryEnvelope");

    private final String geometryType;

    public String getGeometryType() {
        return geometryType;
    }

    GeometryTypeEnum(String geomType) {
        this.geometryType = geomType;
    }

    public static GeometryTypeEnum forJTSClass(Class<?> jtsClass) {
        if (jtsClass.equals(org.locationtech.jts.geom.Point.class)) {
            return POINT;
        } else if (jtsClass.equals(org.locationtech.jts.geom.MultiPoint.class)) {
            return MULTIPOINT;
        } else if (jtsClass.equals(org.locationtech.jts.geom.LineString.class)) {
            return POLYLINE;
        } else if (jtsClass.equals(org.locationtech.jts.geom.MultiLineString.class)) {
            return POLYLINE;
        } else if (jtsClass.equals(org.locationtech.jts.geom.Polygon.class)) {
            return POLYGON;
        } else if (jtsClass.equals(org.locationtech.jts.geom.MultiPolygon.class)) {
            return POLYGON;
        } else if (jtsClass.equals(org.locationtech.jts.geom.Geometry.class)) {
            return POLYGON;
        } else if (jtsClass.equals(org.locationtech.jts.geom.Envelope.class)) {
            return ENVELOPE;
        } else {
            throw new NoSuchElementException(
                    "No GeoServices Geometry equivalent known for " + jtsClass);
        }
    }

    public static GeometryTypeEnum forResourceDefaultGeometry(ResourceInfo resource)
            throws IOException {
        if (resource instanceof CoverageInfo) {
            return POLYGON;
        } else if (resource instanceof FeatureTypeInfo) {
            GeometryDescriptor gDesc =
                    ((FeatureTypeInfo) resource).getFeatureType().getGeometryDescriptor();
            if (gDesc == null) {
                return null;
            } else {
                return forJTSClass(gDesc.getType().getBinding());
            }
        } else {
            return null;
        }
    }

    @JsonValue
    public String value() {
        return this.geometryType;
    }
}
