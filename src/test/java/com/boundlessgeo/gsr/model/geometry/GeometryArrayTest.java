/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class GeometryArrayTest {


    /*
     * A.3.7: geometry/array - Verify that every array object meets the requirements
     */
    @Test
    public void isValidGeometryTypesTest() {
        Double[] coord1 = { -77.1, 40.07 };
        Double[] coord2 = { -79.1, 38.07 };
        Double[] coord3 = { -105.1, -29.08 };
        SpatialReference spatialRef = new SpatialReferenceWKID(4326);
        Point point1 = new Point(coord1[0], coord1[1], spatialRef);
        Point point2 = new Point(coord2[0], coord2[1], spatialRef);
        Double[][] coords = { coord1, coord2, coord3, coord1 };
        Double[][][] rings = { coords };
        Polygon polygon = new Polygon(rings, spatialRef);
        Geometry[] geometries1 = { point1, point2 };
        Geometry[] geometries2 = { point1, point2, polygon };
        GeometryArray geometryArray1 = new GeometryArray(GeometryTypeEnum.POINT, geometries1,
                spatialRef);
        GeometryArray geometryArray2 = new GeometryArray(GeometryTypeEnum.POINT, geometries2,
                spatialRef);

        assertEquals(true, geometryArray1.isValidGeometryTypes());
        assertEquals(false, geometryArray2.isValidGeometryTypes());
    }
}
