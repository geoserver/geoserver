/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.system.status;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.wicket.util.tester.TagTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.admin.StatusPage;
import org.junit.Before;
import org.junit.Test;

public class SystemStatusMonitorPanelTest extends GeoServerWicketTestSupport {

    @Before
    public void setupTests() {
        login();
        tester.getApplication().getMarkupSettings().setStripWicketTags(false);
        tester.startPage(StatusPage.class);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:2:link", true);
        tester.assertContains("CPUs");
    }

    @Test
    public void testUpdate() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat(RefreshedPanel.datePattern);
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:2:link", true);
        TagTester time1 = tester.getTagByWicketId("time");
        assertNotNull(time1);
        Date firstTime = formatter.parse(time1.getValue());
        // System.out.println(firstTime.getTime());
        // Execute timer
        Thread.sleep(1000);
        tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        TagTester time2 = tester.getTagByWicketId("time");
        assertNotNull(time2);
        Date secondTime = formatter.parse(time2.getValue());
        // Check if update time is changed
        assertTrue(secondTime.getTime() > firstTime.getTime());
        Thread.sleep(1000);
        tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        TagTester time3 = tester.getTagByWicketId("time");
        assertNotNull(time3);
        Date thirdTime = formatter.parse(time3.getValue());
        // Check if update time is changed (use 500ms due to time imprecision)
        assertTrue(thirdTime.getTime() > secondTime.getTime());
    }
}
