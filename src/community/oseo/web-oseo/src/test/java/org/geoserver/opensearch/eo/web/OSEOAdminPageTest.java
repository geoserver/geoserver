/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.junit.Before;
import org.junit.Test;

public class OSEOAdminPageTest extends OSEOWebTestSupport {

    @Before
    public void startPage() {
        login();
        tester.startPage(OSEOAdminPage.class);
    }

    @Test
    public void smokeTest() throws Exception {
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:maximumRecordsPerPage", OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        tester.assertModelValue("form:recordsPerPage", OSEOInfo.DEFAULT_RECORDS_PER_PAGE);
        tester.assertModelValue("form:aggregatesCacheTTL", OSEOInfo.DEFAULT_AGGR_CACHE_TTL);
        tester.assertModelValue(
                "form:aggregatesCacheTTLUnit", OSEOInfo.DEFAULT_AGGR_CACHE_TTL_UNIT);
        tester.assertModelValue("form:attribution", null);
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
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        String attributionValue = "Attribution test";
        formTester.setValue("attribution", attributionValue);
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
    public void testCacheTTLValidatation() throws Exception {
        setupCacheValues(-10);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        setupCacheValues(10);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testQueryables() throws Exception {
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("globalQueryables", "  a,  b,c");
        formTester.submit("submit");
        tester.assertNoErrorMessage();

        OSEOInfo oseo = getGeoServer().getService(OSEOInfo.class);
        assertEquals(Arrays.asList("a", "b", "c"), oseo.getGlobalQueryables());
    }

    private void setupPagingValues(int records, int maxRecords) {
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("recordsPerPage", "" + records);
        formTester.setValue("maximumRecordsPerPage", "" + maxRecords);
        formTester.submit();
    }

    private void setupCacheValues(int ttl) {
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("aggregatesCacheTTL", "" + ttl);
        formTester.submit();
    }

    private void checkRequired(String componentName) {
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue(componentName, null);
        formTester.submit();
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }
}
