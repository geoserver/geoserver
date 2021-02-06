/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wps.ProcessGroupInfo;
import org.junit.Test;

public class WPSAccessRulePageTest extends WPSPagesTestSupport {

    @Test
    public void testGroupToWpsLink() throws Exception {
        login();
        tester.startPage(new WPSAccessRulePage());
        tester.assertRenderedPage(WPSAccessRulePage.class);
        tester.clickLink(
                "form:processFilterTable:listContainer:items:1:itemProperties:5:component:link",
                false);
        tester.assertRenderedPage(ProcessSelectionPage.class);
    }

    @Test
    public void testDisableWps() throws Exception {
        login();
        tester.startPage(new WPSAccessRulePage());
        tester.assertRenderedPage(WPSAccessRulePage.class);
        tester.clickLink(
                "form:processFilterTable:listContainer:items:1:itemProperties:5:component:link",
                false);
        FormTester ft = tester.newFormTester("form");
        ft.setValue(
                "selectionTable:listContainer:items:1:itemProperties:0:component:enabled", "false");
        ft.submit("apply");
        @SuppressWarnings("unchecked")
        GeoServerTablePanel<ProcessGroupInfo> processFilterTable =
                (GeoServerTablePanel)
                        tester.getComponentFromLastRenderedPage("form:processFilterTable");
        ProcessFactoryInfoProvider dp =
                (ProcessFactoryInfoProvider) processFilterTable.getDataProvider();
        assertEquals(dp.getItems().get(0).getFilteredProcesses().size(), 1);
    }

    @Test
    public void testCheckGroup() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSAccessRulePage());
        tester.assertRenderedPage(WPSAccessRulePage.class);

        tester.assertComponent("form:processFilterTable", GeoServerTablePanel.class);
        @SuppressWarnings("unchecked")
        GeoServerTablePanel<ProcessGroupInfo> processFilterTable =
                (GeoServerTablePanel)
                        tester.getComponentFromLastRenderedPage("form:processFilterTable");
        ProcessFactoryInfoProvider dp =
                (ProcessFactoryInfoProvider) processFilterTable.getDataProvider();
        for (ProcessGroupInfo pgi : dp.getItems()) {
            assertTrue(pgi.isEnabled());
        }

        FormTester ft = tester.newFormTester("form");
        ft.setValue(
                "processFilterTable:listContainer:items:1:itemProperties:0:component:enabled",
                "false");
        ft.setValue(
                "processFilterTable:listContainer:items:4:itemProperties:0:component:enabled",
                "false");
        ft.submit();
        assertFalse(dp.getItems().get(0).isEnabled());
        assertFalse(dp.getItems().get(3).isEnabled());
    }
}
