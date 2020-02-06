/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.FeatureSource;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.type.FeatureType;

/**
 * This test makes use of mock RetypeFeatureTypeCallback implementation provided in
 *
 * @author ImranR
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
        super.onSetUp(testData);
        setupBasicLayer(testData);
    }

    private void setupBasicLayer(SystemTestData testData) throws IOException {
        // Loading a layer with location given as latitude and longitude and no geometry
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                new HashMap(),
                LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER + ".properties",
                getClass(),
                getCatalog());
    }

    @Test
    public void testCallBacks() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog()
                        .getFeatureTypeByName(
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getNamespaceURI(),
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getLocalPart());

        FeatureType ft1 = pool.getFeatureType(info);

        // assert that feature type is returned with a point geometry
        assertTrue(ft1.getUserData().containsKey(MockRetypeFeatureTypeCallback.RETYPED));
        assertTrue(ft1.getGeometryDescriptor().getType().getBinding().equals(Point.class));

        // assert that feature source is wrapped
        FeatureSource retyped = pool.getFeatureSource(info, null);
        assertTrue(
                retyped
                        instanceof
                        MockRetypeFeatureTypeCallback.MockRetypeFeatureTypedFeatureSource);
    }
}
