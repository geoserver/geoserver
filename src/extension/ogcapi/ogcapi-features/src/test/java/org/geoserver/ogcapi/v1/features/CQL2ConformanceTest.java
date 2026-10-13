/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.junit.Test;

public class CQL2ConformanceTest {

    @Test
    public void testBuiltInIgnoresConfiguration() {
        WFSInfo service = new WFSInfoImpl();
        CQL2Conformance cql2 = new CQL2Conformance();
        cql2.setBasic(false);
        cql2.setText(false);

        assertTrue(cql2.basic(service));
        assertFalse(cql2.text(service));
    }

    @Test
    public void testNotImplementedNeverEnabled() {
        WFSInfo service = new WFSInfoImpl();
        CQL2Conformance cql2 = new CQL2Conformance();

        assertEquals(Boolean.FALSE, CQL2Conformance.CQL2_TEMPORAL.getBuiltIn());
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
