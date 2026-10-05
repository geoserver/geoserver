/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.junit.Test;

public class APIConformanceTest {

    private static final APIConformance CORE =
            new APIConformance("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core");

    @Test
    public void testExtendName() {
        APIConformance html = CORE.extend("html");

        assertEquals("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/html", html.getId());
        assertEquals("html", html.getProperty());
    }

    @Test
    public void testExtendHyphenatedName() {
        APIConformance spatialSubsetting = CORE.extend("spatial-subsetting");

        assertEquals(
                "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting", spatialSubsetting.getId());
        assertEquals("spatialSubsetting", spatialSubsetting.getProperty());
    }

    @Test
    public void testExtendURI() {
        APIConformance filter = CORE.extend(ConformanceClass.FILTER);

        assertEquals(ConformanceClass.FILTER, filter.getId());
    }

    @Test
    public void testBuiltInIgnoresConfiguration() {
        ServiceInfo service = new ServiceInfoImpl();
        CQL2Conformance cql2 = new CQL2Conformance();
        cql2.setBasic(false);
        cql2.setText(false);

        assertTrue(cql2.basic(service));
        assertFalse(cql2.text(service));
    }

    @Test
    public void testNotImplementedNeverEnabled() {
        ServiceInfo service = new ServiceInfoImpl();
        CQL2Conformance cql2 = new CQL2Conformance();

        assertFalse(cql2.isEnabled(service, true, CQL2Conformance.CQL2_TEMPORAL));
        assertFalse(cql2.conformances(service).contains(CQL2Conformance.CQL2_TEMPORAL));
    }

    @Test
    public void testConfigurableConformances() {
        List<APIConformance> configurable = new CQL2Conformance().configurableConformances();

        assertEquals(
                List.of(CQL2Conformance.CQL2_TEXT, CQL2Conformance.CQL2_JSON, CQL2Conformance.CQL2_FUNCTIONS),
                configurable);
        assertTrue(configurable.stream().allMatch(APIConformance::isConfigurable));
    }
}
