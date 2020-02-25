/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class ServiceProviderTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addVectorLayer(SystemTestData.POLYGONS, getCatalog());
        testData.addDefaultRasterLayer(SystemTestData.TASMANIA_DEM, getCatalog());
    }

    @Test
    public void testHideServiceConfigVector() {
        final ServiceResourceProvider provider =
                GeoServerExtensions.bean(ServiceResourceProvider.class);
        final List<String> services =
                provider.getServicesForLayerName(getLayerId(SystemTestData.POLYGONS));
        assertThat(services, not(contains("OSEO")));
        assertThat(services, containsInAnyOrder("WMS", "WFS"));
    }

    @Test
    public void testHideServiceConfigRaster() {
        final ServiceResourceProvider provider =
                GeoServerExtensions.bean(ServiceResourceProvider.class);
        final List<String> services =
                provider.getServicesForLayerName(getLayerId(SystemTestData.TASMANIA_DEM));
        assertThat(services, not(contains("OSEO")));
        assertThat(services, contains("WMS"));
    }
}
