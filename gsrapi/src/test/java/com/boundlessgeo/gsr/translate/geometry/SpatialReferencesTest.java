package com.boundlessgeo.gsr.translate.geometry;

import com.boundlessgeo.gsr.Utils;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.junit.Assert.assertEquals;

public class SpatialReferencesTest {
    @Test
    public void testParseSpatialReference() throws FactoryException {
        CoordinateReferenceSystem expectedCRS = CRS.decode("EPSG:3857");

        //Web Mercator
        SpatialReferenceWKID sr = (SpatialReferenceWKID) SpatialReferences.fromCRS(Utils.parseSpatialReference("3857"));
        assertEquals(3857, sr.getWkid());
        assertEquals(3857, sr.getLatestWkid());

        //Google Web Mercator
        sr = (SpatialReferenceWKID) SpatialReferences.fromCRS(Utils.parseSpatialReference("900913"));
        assertEquals(900913, sr.getWkid());
        assertEquals(900913, sr.getLatestWkid());

        //ESRI Web Mercator
        sr = (SpatialReferenceWKID) SpatialReferences.fromCRS(Utils.parseSpatialReference("102100"));
        assertEquals(102100, sr.getWkid());
        assertEquals(102100, sr.getLatestWkid());
    }
}
