/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.Arrays;
import org.easymock.EasyMock;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.data.test.MockCatalogBuilder;
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;

public class WMSValidatorTest extends GeoServerMockTestSupport {

    @Override
    protected MockTestData createTestData() throws Exception {
        MockTestData td = new MockTestData();
        td.setMockCreator(
                new MockCreator() {

                    @Override
                    public void onResource(
                            String name, ResourceInfo r, StoreInfo s, MockCatalogBuilder b) {
                        if (name.equals("Buildings")) {
                            FeatureTypeInfo info = (FeatureTypeInfo) r;
                            AttributeTypeInfoImpl geom1 = new AttributeTypeInfoImpl();
                            geom1.setName("geom");
                            EasyMock.expect(info.getAttributes())
                                    .andReturn(Arrays.asList(geom1))
                                    .anyTimes();
                            AttributeTypeInfoImpl geom2 = new AttributeTypeInfoImpl();
                            geom2.setName("geom");
                            geom2.setBinding(Polygon.class);
                            try {
                                EasyMock.expect(info.attributes())
                                        .andReturn(Arrays.asList(geom2))
                                        .anyTimes();
                            } catch (IOException e) {
                                // will not happen
                            }
                        }
                        super.onResource(name, r, s, b);
                    }
                });

        return td;
    }

    @Test
    public void testGeometryCheckLegacyDataDir() {
        // used to NPE
        LayerInfo layer = getCatalog().getLayerByName("Buildings");
        new WMSValidator().validate(layer, false);
    }
}
