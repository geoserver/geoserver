/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vfny.global;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class TolerantStartupTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        QName name = SystemTestData.BASIC_POLYGONS;
        String styleName = name.getLocalPart();
        Map<LayerProperty, Object> props = new HashMap<LayerProperty, Object>();
        props.put(LayerProperty.STYLE, styleName);
        props.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.REPROJECT_TO_DECLARED);
        props.put(LayerProperty.SRS, 123456);
        testData.setUpVectorLayer(
                name, props, name.getLocalPart() + ".properties", SystemTestData.class);

        testData.setUpVectorLayer(SystemTestData.BUILDINGS);
        testData.setUpSecurity();
    }

    //    @Override
    //    protected String getLogConfiguration() {
    //        return "/DEFAULT_LOGGING.properties";
    //    }

    @Test
    public void testContextStartup() {
        GeoServer config = (GeoServer) applicationContext.getBean("geoServer");
        assertNotNull(
                config.getCatalog()
                        .getFeatureTypeByName(
                                MockData.BUILDINGS.getNamespaceURI(),
                                MockData.BUILDINGS.getLocalPart()));
        assertNotNull(
                config.getCatalog()
                        .getFeatureTypeByName(
                                MockData.BASIC_POLYGONS.getNamespaceURI(),
                                MockData.BASIC_POLYGONS.getLocalPart()));
    }
}
