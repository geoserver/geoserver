/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import static org.junit.Assert.*;

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
        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void verifyRequiredFields() throws Exception {
        checkRequired("maximumRecordsPerPage");
        checkRequired("recordsPerPage");
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

    private void setupPagingValues(int records, int maxRecords) {
        tester.startPage(OSEOAdminPage.class);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("recordsPerPage", "" + records);
        formTester.setValue("maximumRecordsPerPage", "" + maxRecords);
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
