/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
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

public class WFSReprojectionUtilTest {

    private final FeatureType featureType = createFeatureType();
    private final FilterFactory filterFactory = createFilterFactory();

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
            return DataUtilities.createType("testType", "geom:Point:srid=4326,line:LineString,name:String,id:int");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static FilterFactory createFilterFactory() {
        try {
            return CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void testNullCheck() {
        // used to throw an NPE if the feature type was null
        CoordinateReferenceSystem crs = WFSReprojectionUtil.getDeclaredCrs((FeatureType) null, "1.1.0");
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
                new ReferencedEnvelope(1669792.36, 2782987.269831839, 1118889.97, 2273030.92, webMercator);
        assertTrue(JTS.equals(expected, clonedBbox.getBounds(), 0.1));
    }

    @Test
    public void testAxisOrderWKT() throws Exception {
        String wkt =
                "GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]";
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        assertEquals(CRS.AxisOrder.EAST_NORTH, CRS.getAxisOrder(crs));

        // the code uses an EPSG database lookup to find an equivalent official code
        CoordinateReferenceSystem declared = WFSReprojectionUtil.getDeclaredCrs(crs, "1.1.0");
        assertEquals(CRS.AxisOrder.NORTH_EAST, CRS.getAxisOrder(declared));
    }
}
