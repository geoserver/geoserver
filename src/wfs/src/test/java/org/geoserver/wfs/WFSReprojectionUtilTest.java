/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class WFSReprojectionUtilTest {

    private final FeatureType featureType = createFeatureType();
    private final FilterFactory2 filterFactory = createFilterFactory();

    @BeforeClass
    public static void before() {
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, false);
        Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, true);
    }

    @AfterClass
    public static void after() {
        Hints.removeSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER);
        Hints.removeSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING);
    }

    private static FeatureType createFeatureType() {
        try {
            return DataUtilities.createType(
                    "testType", "geom:Point:srid=4326,line:LineString,name:String,id:int");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static FilterFactory2 createFilterFactory() {
        try {
            return CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void testNullCheck() {
        // used to throw an NPE if the feature type was null
        CoordinateReferenceSystem crs =
                WFSReprojectionUtil.getDeclaredCrs((FeatureType) null, "1.1.0");
        assertNull(crs);
    }

    @Test
    public void testReprojectFilterWithTargetCrs() throws FactoryException {
        BBOX bbox = filterFactory.bbox(filterFactory.property("geom"), 10, 15, 20, 25, "EPSG:4326");
        CoordinateReferenceSystem webMercator = CRS.decode("EPSG:3857");
        Filter clone = WFSReprojectionUtil.reprojectFilter(bbox, featureType, webMercator);
        assertNotSame(bbox, clone);
        BBOX clonedBbox = (BBOX) clone;
        assertEquals(bbox.getExpression1(), clonedBbox.getExpression1());
        ReferencedEnvelope expected =
                new ReferencedEnvelope(
                        1669792.36, 2782987.269831839, 1118889.97, 2273030.92, webMercator);
        assertTrue(JTS.equals(expected, clonedBbox.getBounds(), 0.1));
    }
}
