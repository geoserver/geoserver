/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.v1.styles.StylesConformance;
import org.geoserver.ogcapi.v1.styles.StylesServiceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ogcapi.ConformanceTable;
import org.junit.Test;

public class StylesAdminPageTest extends GeoServerWicketTestSupport {

    private static final String ENABLED_ID = "class=\"gs-conformance-id\">";
    private static final String DISABLED_ID = "class=\"gs-conformance-id gs-conformance-disabled\">";

    @Test
    public void testConformanceTable() {
        startServiceTab();

        ConformanceTable table = tester.getLastRenderedPage()
                .visitChildren(
                        ConformanceTable.class, (IVisitor<ConformanceTable, ConformanceTable>) (c, v) -> v.stop(c));
        assertEquals(1, table.getDataProvider().size());
        String markup = tester.getLastResponseAsString();
        assertThat(markup, not(containsString("Warning: Property")));
        assertThat(markup, containsString("Manage styles (create, update and delete)"));
    }

    @Test
    public void testEnableManageStyles() {
        GeoServer gs = getGeoServer();
        assertNull(gs.getService(StylesServiceInfo.class).getMetadata().get(StylesConformance.METADATA_KEY));

        startServiceTab();
        FormComponent<?> checkbox = tester.getLastRenderedPage()
                .visitChildren(FormComponent.class, (IVisitor<FormComponent<?>, FormComponent<?>>) (c, v) -> {
                    if ("checkbox".equals(c.getId())) v.stop(c);
                });
        String path = checkbox.getPageRelativePath().substring("form:".length());
        FormTester form = tester.newFormTester("form");
        form.setValue(path + ":value", "true");
        form.submit("submit");

        StylesServiceInfo service = gs.getService(StylesServiceInfo.class);
        try {
            assertEquals(Boolean.TRUE, StylesConformance.configuration(service).getManageStyles());
            assertTrue(StylesConformance.configuration(service).manageStyles(service));
        } finally {
            service.getMetadata().remove(StylesConformance.METADATA_KEY);
            gs.save(service);
        }
    }

    /** Manage styles is off unless set, so its identifier is shown disabled until enabled. */
    @Test
    public void testManageStylesShownDisabledByDefault() {
        String manage = StylesConformance.MANAGE_STYLES.getId() + "<";
        startServiceTab();
        assertThat(tester.getLastResponseAsString(), containsString(DISABLED_ID + manage));

        GeoServer gs = getGeoServer();
        StylesServiceInfo service = gs.getService(StylesServiceInfo.class);
        StylesConformance conformance = new StylesConformance();
        conformance.setManageStyles(true);
        service.getMetadata().put(StylesConformance.METADATA_KEY, conformance);
        gs.save(service);
        try {
            startServiceTab();
            String markup = tester.getLastResponseAsString();
            assertThat(markup, containsString(ENABLED_ID + manage));
            assertThat(markup, not(containsString(DISABLED_ID + manage)));
        } finally {
            service = gs.getService(StylesServiceInfo.class);
            service.getMetadata().remove(StylesConformance.METADATA_KEY);
            gs.save(service);
        }
    }

    /** Opens the page on the Styles tab, which holds the conformance table. */
    private void startServiceTab() {
        login();
        tester.startPage(StylesAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertRenderedPage(StylesAdminPage.class);
    }
}
