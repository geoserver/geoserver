/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.ogcapi.v1.maps.MapsConformance;
import org.geoserver.ogcapi.v1.maps.MapsSettings;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ogcapi.ConformanceTable;
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

        MapsServiceAdminPanel panel = tester.getLastRenderedPage()
                .visitChildren(MapsServiceAdminPanel.class, (IVisitor<MapsServiceAdminPanel, MapsServiceAdminPanel>)
                        (c, v) -> v.stop(c));
        assertNotNull("Maps conformance panel not contributed to the WMS admin page", panel);

        // every configurable class gets a row, and every label resolves: a key with no property renders as a
        // Wicket warning rather than failing, so grep for that too
        List<ConformanceTable> tables = new ArrayList<>();
        panel.visitChildren(ConformanceTable.class, (IVisitor<ConformanceTable, Void>) (c, v) -> tables.add(c));
        assertEquals(3, tables.size());
        assertEquals(
                MapsConformance.configuration(getGeoServer().getService(WMSInfo.class))
                        .configurableConformances()
                        .size(),
                tables.get(0).getDataProvider().size());

        String markup = tester.getLastResponseAsString();
        assertThat(markup, not(containsString("Warning: Property")));
        for (String label : new String[] {
            "Enabled",
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
            assertThat("Missing conformance class row: " + label, markup, containsString(label));
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

    /**
     * In strict mode a community class is off by default, so its identifier is shown disabled without a stored flag.
     */
    @Test
    public void testDefaultDisabledConformanceShownDisabled() {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setCiteCompliant(true);
        getGeoServer().save(wms);
        try {
            login();
            tester.startPage(WMSAdminPage.class);
            tester.clickLink("form:tabs:tabs-container:tabs:" + mapsTabIndex() + ":link");

            String markup = tester.getLastResponseAsString();
            String featureInfo = "http://geoserver.org/spec/ogcapi-maps/1.0/conf/featureinfo<";
            assertTrue(markup.contains("class=\"gs-conformance-id gs-conformance-disabled\">" + featureInfo));
            assertTrue(markup.contains(
                    "class=\"gs-conformance-id\">https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/dataset-map<"));

            // the checkbox cell of the same row tells the page what each click would mean
            String row = markup.substring(0, markup.indexOf(featureInfo));
            row = row.substring(row.lastIndexOf("<tr"));
            assertTrue(row.contains("data-in-effect-unset=\"false\""));
            assertTrue(row.contains("data-in-effect-true=\"true\""));
            assertTrue(row.contains("data-in-effect-false=\"false\""));
        } finally {
            wms.setCiteCompliant(false);
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
