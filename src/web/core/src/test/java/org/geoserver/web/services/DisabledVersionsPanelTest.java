/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.util.Version;
import org.junit.Test;

public class DisabledVersionsPanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testPanelModelUpdate() throws Exception {
        final List<Version> disabledVersions = new ArrayList<>();
        tester.startPage(new FormTestPage(
                (ComponentBuilder) id -> new DisabledVersionsPanel(id, Model.ofList(disabledVersions), "WMS")));

        print(tester.getLastRenderedPage(), true, true);

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:palette:recorder", "1.3.0");
        formTester.submit();

        assertNotNull("disabled versions list should not be null", disabledVersions);

        if (!disabledVersions.isEmpty()) {
            assertEquals("Should have 1 disabled version", 1, disabledVersions.size());
            assertTrue("Version 1.3.0 should be in disabled list", disabledVersions.contains(new Version("1.3.0")));
        } else {
            fail("Panel did not update the model - disabledVersions list is empty after form submission");
        }
    }

    @Test
    public void testInPlaceListModification() {
        List<Version> list = new ArrayList<>();
        list.add(new Version("1.3.0"));

        assertEquals(1, list.size());
        assertTrue(list.contains(new Version("1.3.0")));

        list.clear();
        list.add(new Version("1.1.1"));

        assertEquals(1, list.size());
        assertFalse(list.contains(new Version("1.3.0")));
        assertTrue(list.contains(new Version("1.1.1")));
    }
}
