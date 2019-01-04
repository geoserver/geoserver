/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import junit.framework.TestCase;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test case for the {@link ROIManager} class.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ROIManagerTest extends TestCase {

    public void testBase() throws Exception {

        // example in wgs84
        final CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        Envelope env = new Envelope(8, 9, 40, 41);
        Polygon roi = JTS.toGeometry(env);
        assertTrue(roi.isRectangle());
        roi.setSRID(4326);
        roi.setUserData(wgs84);

        // go to 3857
        final CoordinateReferenceSystem googlem = CRS.decode("EPSG:3857", true);
        final Geometry roiGoogle = JTS.transform(roi, CRS.findMathTransform(wgs84, googlem));
        assertTrue(roiGoogle.isRectangle());

        // target crs is 3003
        final CoordinateReferenceSystem boaga = CRS.decode("EPSG:3003", true);
        final Geometry roiBoaga = JTS.transform(roi, CRS.findMathTransform(wgs84, boaga));
        assertFalse(roiBoaga.isRectangle());
        assertTrue(roiBoaga.getEnvelope().isRectangle());

        // create manager
        final ROIManager roiManager = new ROIManager(roiGoogle, googlem);
        assertTrue(roiManager.isROIBBOX());

        // provide native CRS
        roiManager.useNativeCRS(wgs84);
        final Geometry roiNativeCRS = roiManager.getSafeRoiInNativeCRS();
        assertTrue(roiNativeCRS.isRectangle());
        assertTrue(roiNativeCRS.getEnvelope().equalsExact(roi.getEnvelope(), 1E-9));

        // provide target CRS
        roiManager.useTargetCRS(boaga);
        final Geometry roiTargetCRS = roiManager.getSafeRoiInTargetCRS();
        assertTrue(roiTargetCRS.isRectangle());
    }
}
