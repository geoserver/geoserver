/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.list.ListView;
import org.geoserver.fips.FipsSetup;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.admin.StatusPage;
import org.junit.Before;
import org.junit.Test;

/**
 * The tab is the only place where an admin can see that a deployment runs under FIPS. It has to render once the module
 * is installed, and it has to name what is in use, not merely say that something is.
 */
public class FipsStatusPanelTest extends GeoServerWicketTestSupport {

    /** The four core tabs come first, so the extension tab this module contributes is the fifth. */
    private static final String FIPS_TAB_LINK = "tabs:tabs-container:tabs:4:link";

    @Before
    public void openTheFipsTab() {
        // the panel reads the state of the FIPS module, which cannot be read while the regular jars are present
        assumeTrue(FipsSetup.isFipsClasspath());
        login();
        tester.startPage(StatusPage.class);
        tester.clickLink(FIPS_TAB_LINK, true);
    }

    @Test
    public void testTabIsTitledFips() {
        tester.assertRenderedPage(StatusPage.class);
        assertEquals(
                "FIPS",
                tester.getComponentFromLastRenderedPage(FIPS_TAB_LINK + ":title")
                        .getDefaultModelObjectAsString());
    }

    @Test
    public void testPanelReportsTheProviderAndTheKeystoreType() {
        List<String> values = factValues();

        assertThat(values, hasItem(containsString("BCFIPS")));
        assertThat(values, hasItem("BCFKS"));
        assertThat(values, hasItem("AES-GCM"));
    }

    @Test
    public void testProviderIsAheadOfTheJdkProviders() {
        assertEquals("first", factValue("Provider position"));
    }

    /** A random source from another provider means the random bytes do not come from the validated module. */
    @Test
    public void testRandomNumbersComeFromTheFipsProvider() {
        assertThat(factValue("Random source"), containsString("BCFIPS"));
    }

    @Test
    public void testSelfTestsPassed() {
        assertEquals("READY", factValue("Crypto module"));
    }

    @Test
    public void testKeystoreAlgorithmIsReportedAvailable() {
        ListView<?> algorithms = algorithmList();
        assertEquals(4, algorithms.size());

        assertEquals("BCFKS", labelAt(algorithms, 0, "algorithm"));
        assertEquals("yes", labelAt(algorithms, 0, "available"));
    }

    private List<String> factValues() {
        ListView<?> facts = factList();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < facts.size(); i++) {
            values.add(labelAt(facts, i, "value"));
        }
        return values;
    }

    private String factValue(String label) {
        ListView<?> facts = factList();
        for (int i = 0; i < facts.size(); i++) {
            if (label.equals(labelAt(facts, i, "label"))) {
                return labelAt(facts, i, "value");
            }
        }
        throw new AssertionError("No fact labelled " + label);
    }

    private ListView<?> factList() {
        return (ListView<?>) tester.getComponentFromLastRenderedPage("tabs:panel:facts");
    }

    private ListView<?> algorithmList() {
        return (ListView<?>) tester.getComponentFromLastRenderedPage("tabs:panel:algorithms");
    }

    private String labelAt(ListView<?> list, int index, String cell) {
        return tester.getComponentFromLastRenderedPage(list.getPageRelativePath() + ":" + index + ":" + cell)
                .getDefaultModelObjectAsString();
    }
}
