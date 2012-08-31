package org.opengeo.gsr.core.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class GeometryArrayTest {

    
    /*
     * A.3.7: geometry/array - Verify that every array object meets the requirements
     */
    @Test
    public void isValidGeometryTypesTest() {
        double[] coord1 = { -77.1, 40.07 };
        double[] coord2 = { -79.1, 38.07 };
        double[] coord3 = { -105.1, -29.08 };
        SpatialReference spatialRef = new SpatialReferenceWKID(4326);
        Point point1 = new Point(coord1[0], coord1[1], spatialRef);
        Point point2 = new Point(coord2[0], coord2[1], spatialRef);
        double[][] coords = new double[4][2];
        coords[0] = coord1;
        coords[1] = coord2;
        coords[2] = coord3;
        coords[3] = coord1;
        double[][][] rings = new double[1][4][2];
        rings[0] = coords;
        Polygon polygon = new Polygon(rings, spatialRef);
        Geometry[] geometries1 = new Geometry[2];
        geometries1[0] = point1;
        geometries1[1] = point2;
        Geometry[] geometries2 = new Geometry[3];
        geometries2[0] = point1;
        geometries2[1] = point2;
        geometries2[2] = polygon;
        GeometryArray geometryArray1 = new GeometryArray(GeometryType.POINT, geometries1,
                spatialRef);
        GeometryArray geometryArray2 = new GeometryArray(GeometryType.POINT, geometries2,
                spatialRef);

        assertEquals(true, geometryArray1.isValidGeometryTypes());
        assertEquals(false, geometryArray2.isValidGeometryTypes());

    }
}
