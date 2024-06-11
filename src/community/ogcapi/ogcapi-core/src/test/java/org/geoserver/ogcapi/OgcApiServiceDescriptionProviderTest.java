/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;

import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** ALSO SEE DGGSServiceDescriptionProviderTest for more specific tests. */
public class OgcApiServiceDescriptionProviderTest extends GeoServerSystemTestSupport {

    /** Tests really basic info in the object. */
    @Test
    public void testBasic() {
        var provider = new TestWMSOgcApiServiceDescriptionProviderTest(this.getGeoServer());
        assertEquals("TestCaseServiceType", provider.serviceType);
        assertEquals("OGCAPI-TestCase", provider.serviceName);
        assertEquals("TestCase", provider.specificServiceType);

        assertEquals(TestCaseInfo.class, provider.infoClass);
        assertEquals(TestCaseService.class, provider.serviceClass);
    }

    public class TestCaseService {}

    public class TestWMSOgcApiServiceDescriptionProviderTest
            extends OgcApiServiceDescriptionProvider<TestCaseInfo, TestCaseService> {

        public TestWMSOgcApiServiceDescriptionProviderTest(GeoServer gs) {
            super(gs, "TestCaseServiceType", "OGCAPI-TestCase", "TestCase");
        }
    }

    public interface TestCaseInfo extends ServiceInfo {}
}
