/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi.provider;

import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class TestCaseOgcApiServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<TestCaseInfo, TestCaseService> {

    public TestCaseOgcApiServiceDescriptionProvider(GeoServer gs) {
        super(gs, "TestCaseServiceType", "OGCAPI-TestCase", "TestCase");
    }

    @Override
    protected boolean isAvailable(String serviceType, ServiceInfo info, PublishedInfo layerInfo) {
        return true;
    }
}
