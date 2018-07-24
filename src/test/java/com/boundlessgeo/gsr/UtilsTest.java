package com.boundlessgeo.gsr;

import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void testParseSpatialReference() throws FactoryException {
        CoordinateReferenceSystem expectedCRS = CRS.decode("EPSG:3857");

        //Web Mercator
        CoordinateReferenceSystem testCRS = Utils.parseSpatialReference("3857");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));

        //Google Web Mercator
        testCRS = Utils.parseSpatialReference("900913");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));

        //ESRI Web Mercator
        testCRS = Utils.parseSpatialReference("102100");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));


        //Test non-matching
        testCRS = Utils.parseSpatialReference("4326");
        assertTrue(CRS.isTransformationRequired(expectedCRS, testCRS));


    }
}
