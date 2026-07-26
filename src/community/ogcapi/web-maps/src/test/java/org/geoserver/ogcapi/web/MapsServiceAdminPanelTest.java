/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.WMSAdminPage;
import org.junit.Test;

public class MapsServiceAdminPanelTest extends GeoServerWicketTestSupport {

    /** The panel is contributed to a dedicated Maps tab (serviceClass WMSInfo, specificServiceType Maps). */
    @Test
    public void testMapsTabListsConformanceClasses() {
        login();
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:" + mapsTabIndex() + ":link");

        Component panel = tester.getLastRenderedPage()
                .visitChildren(MapsServiceAdminPanel.class, (IVisitor<Component, Component>) (c, v) -> v.stop(c));
        assertNotNull("Maps conformance panel not contributed to the WMS admin page", panel);

        // the conformance table lists every configurable class, with its localized label
        String markup = tester.getLastResponseAsString();
        for (String label : new String[] {
            "Spatial subsetting",
            "Scaling",
            "Display resolution",
            "Date and time",
            "Coordinate reference systems",
            "Background",
            "Orientation",
            "TIFF output",
            "SVG output",
            "Feature info (GeoServer extension)"
        }) {
            assertTrue("Missing conformance class row: " + label, markup.contains(label));
        }
    }

    private int mapsTabIndex() {
        @SuppressWarnings("unchecked")
        TabbedPanel<ITab> tabs = (TabbedPanel<ITab>) tester.getComponentFromLastRenderedPage("form:tabs");
        for (int i = 0; i < tabs.getTabs().size(); i++) {
            if ("Maps".equals(tabs.getTabs().get(i).getTitle().getObject())) return i;
        }
        throw new AssertionError("No Maps tab on the WMS admin page");
    }
}
