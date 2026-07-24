/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.store.OSEOPostGISResource;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class OSEOAdminPageTest extends OSEOWebTestSupport {

    @ClassRule
    public static final OSEOPostGISResource postgis = new OSEOPostGISResource(false);

    @Override
    protected OSEOPostGISResource getOSEOPostGIS() {
        return postgis;
    }

    /** Location of the OSEOAdminPanel within the form: it lives in the service tab of the tabbed presentation */
    private static final String PANEL = "tabs:panel:initial:";

    @Before
    public void startPage() {
        login();
        openAdminPage();
    }

    /** Starts the page and selects the service tab, where the OSEO specific panel is shown */
    private void openAdminPage() {
        tester.startPage(OSEOAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
    }

    @Test
    public void smokeTest() throws Exception {
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:" + PANEL + "maximumRecordsPerPage", OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        tester.assertModelValue("form:" + PANEL + "recordsPerPage", OSEOInfo.DEFAULT_RECORDS_PER_PAGE);
        tester.assertModelValue("form:" + PANEL + "aggregatesCacheTTL", OSEOInfo.DEFAULT_AGGR_CACHE_TTL);
        tester.assertModelValue("form:" + PANEL + "aggregatesCacheTTLUnit", OSEOInfo.DEFAULT_AGGR_CACHE_TTL_UNIT);
        tester.assertModelValue("form:" + PANEL + "attribution", null);
        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void verifyRequiredFields() throws Exception {
        checkRequired("maximumRecordsPerPage");
        checkRequired("recordsPerPage");
        checkRequired("aggregatesCacheTTL");
        checkRequired("aggregatesCacheTTLUnit");
    }

    @Test
    public void testAttribution() throws Exception {
        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        String attributionValue = "Attribution test";
        formTester.setValue(PANEL + "attribution", attributionValue);
        formTester.submit("submit");
        tester.assertNoErrorMessage();

        OSEOInfo oseo = getGeoServer().getService(OSEOInfo.class);
        assertEquals(attributionValue, oseo.getAttribution());
    }

    @Test
    public void testPagingValidation() throws Exception {
        setupPagingValues(-10, 100);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        setupPagingValues(100, -10);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        setupPagingValues(100, 10);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        setupPagingValues(2, 1000);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testProductClassValidation() throws Exception {
        openAdminPage();
        // the add link appends a product class with empty name, prefix and namespace, which
        // must be rejected on save
        tester.clickLink("form:" + PANEL + "addClass");
        FormTester formTester = tester.newFormTester("form");
        formTester.submit("submit");
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }

    @Test
    public void testCacheTTLValidatation() throws Exception {
        setupCacheValues(-10);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        setupCacheValues(10);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testQueryables() throws Exception {
        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(PANEL + "globalQueryables", "  a,  b,c");
        formTester.submit("submit");
        tester.assertNoErrorMessage();

        OSEOInfo oseo = getGeoServer().getService(OSEOInfo.class);
        assertEquals(Arrays.asList("a", "b", "c"), oseo.getGlobalQueryables());
    }

    private void setupPagingValues(int records, int maxRecords) {
        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(PANEL + "recordsPerPage", "" + records);
        formTester.setValue(PANEL + "maximumRecordsPerPage", "" + maxRecords);
        formTester.submit("submit");
    }

    private void setupCacheValues(int ttl) {
        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(PANEL + "aggregatesCacheTTL", "" + ttl);
        formTester.submit();
    }

    private void checkRequired(String componentName) {
        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(PANEL + componentName, null);
        formTester.submit();
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }

    @Test
    public void testSkipNumberMatched() throws Exception {
        GeoServer gs = getGeoServer();
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        oseo.setSkipNumberMatched(false);
        gs.save(oseo);

        openAdminPage();
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(PANEL + "skipNumberMatched", "true");
        formTester.submit("submit");
        tester.assertNoErrorMessage();

        oseo = gs.getService(OSEOInfo.class);
        assertTrue(oseo.isSkipNumberMatched());
    }
}
