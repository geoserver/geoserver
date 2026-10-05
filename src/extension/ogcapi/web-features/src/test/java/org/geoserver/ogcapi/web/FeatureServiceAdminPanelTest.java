/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ogcapi.ConformanceTable;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.web.WFSAdminPage;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks the Features tab.
 *
 * <p>Initial tests focused on conformance identifiers as included/excluded.
 */
public class FeatureServiceAdminPanelTest extends GeoServerWicketTestSupport {

    private ConformanceTable features;

    private ConformanceTable cql2;

    private ConformanceTable ecql;

    @Before
    public void startPage() {
        login();
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:" + featuresTabIndex() + ":link");
        List<ConformanceTable> tables = new ArrayList<>();
        tester.getLastRenderedPage()
                .visitChildren(ConformanceTable.class, (IVisitor<ConformanceTable, Void>) (c, v) -> tables.add(c));
        assertEquals(3, tables.size());
        features = tables.get(0);
        cql2 = tables.get(1);
        ecql = tables.get(2);
    }

    @Test
    public void testDraftShownDisabledByDefault() {
        String markup = tester.getLastResponseAsString();
        // IDS draft standard is not enabled by default, so it should be shown as disabled
        assertTrue(markup.contains(
                "class=\"gs-conformance-id gs-conformance-disabled\">" + FeatureConformance.IDS.getId() + "<"));
        assertTrue(markup.contains("class=\"gs-conformance-id\">" + FeatureConformance.CRS_BY_REFERENCE.getId() + "<"));
    }

    @Test
    public void testRowStates() {
        String markup = tester.getLastResponseAsString();
        String row = markup.substring(0, markup.indexOf(FeatureConformance.IDS.getId() + "<"));
        row = row.substring(row.lastIndexOf("<tr"));
        assertTrue(row.contains("data-in-effect-unset=\"false\""));
        assertTrue(row.contains("data-in-effect-true=\"true\""));
        assertTrue(row.contains("data-in-effect-false=\"false\""));
    }

    @Test
    public void testFilterRequiresFilterLanguage() {
        String filter = key(features, FeatureConformance.FILTER);
        String featuresFilter = key(features, FeatureConformance.FEATURES_FILTER);
        String[] languages = {
            key(cql2, CQL2Conformance.CQL2_TEXT),
            key(cql2, CQL2Conformance.CQL2_JSON),
            key(ecql, ECQLConformance.ECQL_TEXT)
        };

        String response = recompute(filter, featuresFilter, languages, null);
        assertFalse(inEffect(response, filter, "true"));
        assertFalse(inEffect(response, featuresFilter, "true"));

        for (String language : languages) {
            response = recompute(filter, featuresFilter, languages, language);
            assertTrue(language, inEffect(response, filter, "true"));
            assertTrue(language, inEffect(response, featuresFilter, "true"));
        }
    }

    @Test
    public void testFeaturesFilterRequiresFilter() {
        String filter = key(features, FeatureConformance.FILTER);
        String featuresFilter = key(features, FeatureConformance.FEATURES_FILTER);

        String response = recompute(Map.of(filter, "false", featuresFilter, "true"));
        assertFalse(inEffect(response, featuresFilter, "true"));

        response = recompute(Map.of(filter, "true", featuresFilter, "true"));
        assertTrue(inEffect(response, featuresFilter, "true"));
    }

    @Test
    public void testRecomputeDoesNotSave() {
        recompute(Map.of(key(features, FeatureConformance.FILTER), "false"));
        assertNull(FeatureConformance.configuration(getGeoServer().getService(WFSInfo.class))
                .isFilter());
    }

    private String recompute(String filter, String featuresFilter, String[] languages, String enabledLanguage) {
        tester.getRequest().addParameter("s", filter + "=true");
        tester.getRequest().addParameter("s", featuresFilter + "=true");
        for (String language : languages) {
            tester.getRequest().addParameter("s", language + "=" + language.equals(enabledLanguage));
        }
        return executeRecompute();
    }

    private String recompute(Map<String, String> states) {
        states.forEach((key, state) -> tester.getRequest().addParameter("s", key + "=" + state));
        return executeRecompute();
    }

    private String executeRecompute() {
        tester.executeBehavior(
                features.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
        return tester.getLastResponseAsString();
    }

    private static String key(ConformanceTable table, APIConformance conformance) {
        return table.getMarkupId() + " " + conformance.getId();
    }

    /** Recomputed outcome of a row in the given checkbox state ({@code unset}, {@code true}, {@code false}). */
    private static boolean inEffect(String response, String key, String state) {
        String row = response.substring(response.indexOf("\"" + key + "\":{"));
        return row.substring(0, row.indexOf('}')).contains("\"" + state + "\":true");
    }

    private int featuresTabIndex() {
        @SuppressWarnings("unchecked")
        TabbedPanel<ITab> tabs = (TabbedPanel<ITab>) tester.getComponentFromLastRenderedPage("form:tabs");
        for (int i = 0; i < tabs.getTabs().size(); i++) {
            if ("Features".equals(tabs.getTabs().get(i).getTitle().getObject())) return i;
        }
        throw new AssertionError("No Features tab on the WFS admin page");
    }
}
