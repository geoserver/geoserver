/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class ConfirmationAjaxLinkTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testMessageEscape() {

        tester.startPage(ConfirmationAjaxLinkTestPage.class);
        print(tester.getLastRenderedPage(), true, true);
        tester.executeAjaxEvent("form:confirmationLink", "click");
        String html = tester.getLastResponseAsString();
        // check the message has been escaped
        assertTrue(html.contains("\\'confirmation\\'"));
    }
}
