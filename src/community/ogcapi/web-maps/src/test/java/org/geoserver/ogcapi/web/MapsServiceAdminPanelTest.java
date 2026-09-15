/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.ogcapi.v1.maps.MapsSettings;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMSInfo;
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
            "Dataset map",
            "Collection selection",
            "Collections in a dataset map",
            "Spatial subsetting",
            "Scaling",
            "Display resolution",
            "Date and time",
            "Coordinate reference systems",
            "Background",
            "Orientation",
            "TIFF output",
            "SVG output",
            "Filter",
            "Queryables",
            "Filter on maps (GeoServer extension)",
            "Feature info (GeoServer extension)",
            "Legend (GeoServer extension)",
            "CQL2 Text",
            "CQL2 JSON",
            "ECQL Text"
        }) {
            assertTrue("Missing conformance class row: " + label, markup.contains(label));
        }
    }

    /** The collection count of a default dataset map is edited on the page and stored in the WMS configuration. */
    @Test
    public void testDefaultCollectionsRoundTrip() {
        login();
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:" + mapsTabIndex() + ":link");

        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:extensions:0:content:defaultCollections", "4");
        form.submit("submit");

        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        try {
            assertEquals(
                    Integer.valueOf(4), wms.getMetadata().get(MapsSettings.DEFAULT_COLLECTIONS_KEY, Integer.class));
            assertEquals(4, MapsSettings.defaultCollections(wms));
        } finally {
            wms.getMetadata().remove(MapsSettings.DEFAULT_COLLECTIONS_KEY);
            getGeoServer().save(wms);
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
