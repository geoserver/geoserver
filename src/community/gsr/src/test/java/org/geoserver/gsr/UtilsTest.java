/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class UtilsTest {

    @Test
    public void testParseSpatialReference() throws FactoryException {
        CoordinateReferenceSystem expectedCRS = CRS.decode("EPSG:3857");

        // Web Mercator
        CoordinateReferenceSystem testCRS = Utils.parseSpatialReference("3857");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));

        // Google Web Mercator
        testCRS = Utils.parseSpatialReference("900913");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));

        // ESRI Web Mercator
        testCRS = Utils.parseSpatialReference("102100");
        assertFalse(CRS.isTransformationRequired(expectedCRS, testCRS));

        // Test non-matching
        testCRS = Utils.parseSpatialReference("4326");
        assertTrue(CRS.isTransformationRequired(expectedCRS, testCRS));
    }
}
