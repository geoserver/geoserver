/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import static org.junit.Assert.assertNotNull;

import org.apache.wicket.util.tester.TagTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.admin.StatusPage;
import org.junit.Before;
import org.junit.Test;

public class JVMConsolePanelTest extends GeoServerWicketTestSupport {

    @Before
    public void setupTests() {
        login();
        tester.getApplication().getMarkupSettings().setStripWicketTags(false);
        tester.startPage(StatusPage.class);
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:3:link", true);
    }

    @Test
    public void testLoadConsoleTab() {
        TagTester threadDumpBtn = tester.getTagByWicketId("dumpThread");
        assertNotNull(threadDumpBtn);
        TagTester heapDumpBtn = tester.getTagByWicketId("dumpHeap");
        assertNotNull(heapDumpBtn);
    }
}
