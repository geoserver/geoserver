package org.geoserver.gsr.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.geoserver.gsr.api.catalog.CatalogServiceController;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

/** Checks the GSR service is available */
public class ServiceTest extends GeoServerSystemTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("GSR", new Version("10.51"));
        assertNotNull(service);
        assertEquals("GSR", service.getId());
        assertEquals(new Version("10.51"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(CatalogServiceController.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "FeatureServerAddFeatures",
                        "FeatureServerApplyEdits",
                        "FeatureServerDeleteFeatures",
                        "FeatureServerGetFeature",
                        "FeatureServerUpdateFeatures",
                        "FeatureServesApplyEdits",
                        "GetServices",
                        "MapServerExportMap",
                        "MapServerExportMapImage",
                        "MapServerFind",
                        "MapServerGetLayer",
                        "MapServerGetLayers",
                        "MapServerGetLegend",
                        "MapServerGetService",
                        "MapServerIdentify",
                        "MapServerExportLayerMap",
                        "MapServerQuery"));
    }
}
