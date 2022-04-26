/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void testCheckButtonsLink() {
        final TagTester threadDumpBtn = tester.getTagByWicketId("dumpThread");
        assertNotNull("dump thread button not present", threadDumpBtn);
        assertTrue(
                "href wrong on dump head button",
                threadDumpBtn.getAttribute("href").contains("tabs-panel-dumpThread"));
        final TagTester heapDumpBtn = tester.getTagByWicketId("dumpHeap");
        assertNotNull("heap thread button not present", heapDumpBtn);
        assertTrue(
                "href wrong on dump heap button",
                heapDumpBtn.getAttribute("href").contains("tabs-panel-dumpHeap"));
    }

    @Test
    public void testLoadThreadDump() {
        tester.clickLink("tabs:panel:dumpThread");
        final String threadDump = tester.getTagByWicketId("dumpContent").getValue();
        assertTrue(threadDump.contains("RUNNABLE"));
    }

    @Test
    public void testLoadHeapDump() {
        tester.clickLink("tabs:panel:dumpHeap");
        final String headDumpStr = tester.getTagByWicketId("dumpContent").getValue();
        assertTrue("heap doesn't contains column num", headDumpStr.contains("num"));
        assertTrue("heap doesn't contains column instances", headDumpStr.contains("#instances"));
        assertTrue("heap doesn't contains column bytes", headDumpStr.contains("#bytes"));
    }
}
