/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.urlchecker.URLEntry;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;

/** @author ImranR */
public class ControlPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        login();
        tester.startPage(ControlPage.class);
        tester.assertRenderedPage(ControlPage.class);

        // verify table is loaded with defaults

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(
                dv.size(),
                geoserverURLConfigServiceBean.getGeoserverURLChecker().getRegexList().size());
    }

    @Test
    public void testAddNew() throws Exception {
        final String urlEntryName = "test";
        login();
        tester.startPage(ControlPage.class);
        tester.assertRenderedPage(ControlPage.class);
        tester.clickLink("headerPanel:addNew");
        tester.assertRenderedPage(URLEntryPage.class);

        // Check validation is working
        // try to add a new entry with a taken name name
        FormTester ft = tester.newFormTester("form");
        ft.setValue("name", urlEntryName);
        ft.setValue("description", "test123");
        ft.setValue("regexExpression", "file");
        ft.submit("submit");
        // validate...cannot add empty name
        tester.assertNoErrorMessage();

        tester.assertRenderedPage(ControlPage.class);
        URLEntry testURLEntry = geoserverURLConfigServiceBean.getGeoserverURLChecker().get("test");
        assertNotNull(testURLEntry);
        // finally remove
        geoserverURLConfigServiceBean.removeAndsave(Arrays.asList(testURLEntry));
    }

    @Test
    public void testValidationsAddNew() throws Exception {

        // add an entry already to test duplication check

        geoserverURLConfigServiceBean
                .getGeoserverURLChecker()
                .getRegexList()
                .add(new URLEntry("files", "test123", "file"));

        // tests for duplicate entry and invalid regex expression
        login();
        tester.startPage(ControlPage.class);
        tester.assertRenderedPage(ControlPage.class);
        tester.clickLink("headerPanel:addNew");
        tester.assertRenderedPage(URLEntryPage.class);

        // Check validation is working
        // try to add a new entry with a taken name name
        FormTester ft = tester.newFormTester("form");
        ft.setValue("name", "files");
        ft.setValue("description", "test123");
        ft.setValue("regexExpression", "file");
        ft.submit("submit");
        // validate...cannot use existing name
        tester.assertErrorMessages("Another URL Entry with name files exists already");

        // try to add an invalid
        ft = tester.newFormTester("form");
        ft.setValue("name", "bad_regex");
        ft.setValue("description", "test123");
        ft.setValue("regexExpression", "file(");
        ft.submit("submit");
        // validate...do not allow bad
        tester.assertErrorMessages("file( is not a valid Regex expression");
    }

    @Test
    public void testModify() throws Exception {
        login();
        tester.startPage(ControlPage.class);
        tester.assertRenderedPage(ControlPage.class);
        tester.clickLink("headerPanel:addNew");

        // add a new URL entry
        FormTester ft = tester.newFormTester("form");
        ft.setValue("name", "test");
        ft.setValue("description", "test123");
        ft.setValue("regexExpression", "file");
        ft.submit("submit");
        tester.assertRenderedPage(ControlPage.class);
        URLEntry testURLEntry = geoserverURLConfigServiceBean.getGeoserverURLChecker().get("test");
        assertNotNull(testURLEntry);

        PageParameters params = new PageParameters();
        params.add("name", "test");
        tester.startPage(new URLEntryPage(params));
        tester.assertRenderedPage(URLEntryPage.class);

        ft = tester.newFormTester("form");
        ft.setValue("name", "test");
        ft.setValue("description", "modfied");
        ft.setValue("regexExpression", "file");
        ft.submit("submit");

        // find the modified bean and assert changes
        URLEntry modified = geoserverURLConfigServiceBean.getGeoserverURLChecker().get("test");
        assertTrue(modified.getDescription().equalsIgnoreCase("modfied"));

        // clean up
        geoserverURLConfigServiceBean.removeAndsave(Arrays.asList(testURLEntry));
    }

    @Test
    public void testRemoveSelected() throws Exception {

        login();
        tester.startPage(ControlPage.class);
        tester.assertRenderedPage(ControlPage.class);
        tester.clickLink("headerPanel:addNew");

        // add a new URL entry
        FormTester ft = tester.newFormTester("form");
        ft.setValue("name", "test");
        ft.setValue("description", "test123");
        ft.setValue("regexExpression", "file");
        ft.submit("submit");
        tester.assertRenderedPage(ControlPage.class);
        URLEntry testURLEntry = geoserverURLConfigServiceBean.getGeoserverURLChecker().get("test");
        assertNotNull(testURLEntry);

        GeoServerTablePanel<URLEntry> table =
                (GeoServerTablePanel<URLEntry>) tester.getComponentFromLastRenderedPage("table");
        // select last
        table.selectIndex(table.getDataProvider().fullSize() - 1);

        // click on the delete
        tester.clickLink("headerPanel:removeSelected", true);

        // assert the dialog shows up
        tester.assertVisible("dialog");

        // click submit
        tester.clickLink("dialog:dialog:content:form:submit", true);

        tester.assertNoInfoMessage();

        assertEquals(
                table.getDataProvider().size(),
                geoserverURLConfigServiceBean.getGeoserverURLChecker().getRegexList().size());
    }
}
