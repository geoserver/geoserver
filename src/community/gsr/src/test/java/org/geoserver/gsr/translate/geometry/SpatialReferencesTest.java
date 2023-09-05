/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.geometry;

import static org.junit.Assert.assertEquals;

import org.geoserver.gsr.Utils;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.Test;

public class SpatialReferencesTest {
    @Test
    public void testParseSpatialReference() throws FactoryException {
        CoordinateReferenceSystem expectedCRS = CRS.decode("EPSG:3857");

        // Web Mercator
        SpatialReferenceWKID sr =
                (SpatialReferenceWKID)
                        SpatialReferences.fromCRS(Utils.parseSpatialReference("3857"));
        assertEquals(3857, sr.getWkid());
        assertEquals(3857, sr.getLatestWkid());

        // Google Web Mercator
        sr =
                (SpatialReferenceWKID)
                        SpatialReferences.fromCRS(Utils.parseSpatialReference("900913"));
        assertEquals(900913, sr.getWkid());
        assertEquals(900913, sr.getLatestWkid());

        // ESRI Web Mercator
        sr =
                (SpatialReferenceWKID)
                        SpatialReferences.fromCRS(Utils.parseSpatialReference("102100"));
        assertEquals(102100, sr.getWkid());
        assertEquals(102100, sr.getLatestWkid());
    }
}
