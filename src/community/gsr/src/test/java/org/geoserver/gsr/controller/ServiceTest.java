/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
                        "FeatureServerGetLayers",
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
