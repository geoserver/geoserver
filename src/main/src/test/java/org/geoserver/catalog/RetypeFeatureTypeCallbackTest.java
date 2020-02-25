/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.global.GeoServerFeatureSource;

/**
 * This test asserts the an implementation of RetypeFeatureTypeCallback is integrated properly with
 * GeoServerFeatureSource wrapper. The test asserts that when a feature type is retyped through
 * RetypeFeatureTypeCallback implementation, it does not break or override the functionality
 * provided by GeoServerFeatureSource wrapper.
 */
public class RetypeFeatureTypeCallbackTest extends GeoServerSystemTestSupport {

    public static final String LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER = "longlat";
    public static final QName LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME =
            new QName(
                    MockData.DEFAULT_PREFIX,
                    LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER,
                    MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // registering singleton for this test only
        MockRetypeFeatureTypeCallback o2 = new MockRetypeFeatureTypeCallback();
        GeoServerExtensionsHelper.singleton(
                "retypeFeatureTypeCallbackTest", o2, RetypeFeatureTypeCallback.class);

        super.onSetUp(testData);
        setUpNonGeometryLayer(testData);
    }

    private void setUpNonGeometryLayer(SystemTestData testData) throws IOException {
        // Loading a vector layer with location given as latitude and longitude and no geometry
        Map<LayerProperty, Object> props = new HashMap<LayerProperty, Object>();
        // declaring a different CRS to test re-projection
        props.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.REPROJECT_TO_DECLARED);
        props.put(LayerProperty.SRS, 3857);
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                props,
                LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER + ".properties",
                getClass(),
                getCatalog());
    }

    /**
     * The test asserts the following operations
     *
     * <p>1. A feature type with no geometry field is correctly re-typed by adding a Geometry field
     * (e.g Point) when requested through ResourcePool
     *
     * <p>2. GeoServerFeatureSource wrapper is intact when wrapping Re-typed Feature Source a
     * -Re-projection works b -FeatureTypeInfo level CQL filter filters records
     */
    @Test
    public void testResourcePoolOperations() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog()
                        .getFeatureTypeByName(
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getNamespaceURI(),
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getLocalPart());
        // setting a feature type level CQL to allow only ONE feature
        info.setCqlFilter("data = 'd1'");
        getCatalog().save(info);
        FeatureType ft1 = pool.getFeatureType(info);

        // assert that feature type is returned with a point geometry
        assertTrue(ft1.getUserData().containsKey(MockRetypeFeatureTypeCallback.RETYPED));
        assertTrue(ft1.getGeometryDescriptor().getType().getBinding().equals(Point.class));

        FeatureSource retyped = pool.getFeatureSource(info, null);
        // assert FeatureSource is nicely wrapped inside Geoserver wrapper
        assertTrue(retyped instanceof GeoServerFeatureSource);
        CoordinateReferenceSystem reprojected = CRS.decode("EPSG:" + 3857);

        // asserting re-projection occurred
        int count = 0;
        try (FeatureIterator iterator = retyped.getFeatures().features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                // check if resulting feature are in same EPSG
                assertFalse(
                        CRS.isTransformationRequired(
                                reprojected, feature.getType().getCoordinateReferenceSystem()));
                count++;
            }
        }
        // asserting CQL filter was effective
        assertTrue(count == 1);
    }
}
