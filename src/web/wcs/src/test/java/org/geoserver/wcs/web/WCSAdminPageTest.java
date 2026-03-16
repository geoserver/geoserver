/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geotools.api.util.InternationalString;
import org.junit.Test;

public class WCSAdminPageTest extends GeoServerWicketCoverageTestSupport {
    /** Location of WFSAdminPanel within form */
    final String SERVICE_ADMIN_PANEL = "tabs:panel";

    /** Location of WCSAdminPanel within form */
    final String WCS_ADMIN_PANEL = "tabs:panel:initial";

    @Test
    public void test() throws Exception {
        login();
        WCSInfo wcs = getGeoServerApplication().getGeoServer().getService(WCSInfo.class);

        // start the page, change to WCSAdminPanel
        tester.startPage(new WCSAdminPage());
        tester.assertRenderedPage(WCSAdminPage.class);

        // test that components have been filled as expected
        tester.assertComponent("form:" + SERVICE_ADMIN_PANEL + ":keywords", KeywordsEditor.class);
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":keywords", wcs.getKeywords());
    }

    @Test
    public void testInternationalContent() {
        login();

        // start the page
        tester.startPage(new WCSAdminPage());
        FormTester form = tester.newFormTester("form");

        // enable i18n for title and add two entries
        form.setValue(
                SERVICE_ADMIN_PANEL + ":serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        // enable i18n for abstract and add two entries
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:" + SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");

        // figure out the locales used in the test (might not be stable across JVMs)
        @SuppressWarnings("unchecked")
        DropDownChoice<Locale> select = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(
                        "form:" + SERVICE_ADMIN_PANEL
                                + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select");
        Locale l10 = select.getChoices().get(10);
        Locale l20 = select.getChoices().get(20);

        // fill the form (don't do this in between ajax calls)
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international abstract");

        // change to wcs tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        form = tester.newFormTester("form");
        // mandatory fields
        form.setValue(WCS_ADMIN_PANEL + ":maxInputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxOutputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxRequestedDimensionValues", "1");
        form.setValue(WCS_ADMIN_PANEL + ":defaultDeflateCompressionLevel", "5");

        form.submit("submit");
        tester.assertNoErrorMessage();

        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        InternationalString internationalTitle = wcs.getInternationalTitle();
        assertEquals("an international title", internationalTitle.toString(l10));
        assertEquals("another international title", internationalTitle.toString(l20));
        InternationalString internationalAbstract = wcs.getInternationalAbstract();
        assertEquals("an international abstract", internationalAbstract.toString(l10));
        assertEquals("another international abstract", internationalAbstract.toString(l20));
    }

    @Test
    public void testDefaultLocale() {
        login();
        // start the page
        tester.startPage(WCSAdminPage.class);
        // change to wcs tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        // mandatory fields
        form.setValue(WCS_ADMIN_PANEL + ":maxInputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxOutputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxRequestedDimensionValues", "1");

        form.select(WCS_ADMIN_PANEL + ":defaultLocale", 11);
        form.submit("submit");
        assertNotNull(getGeoServer().getService(WCSInfo.class).getDefaultLocale());
    }

    @Test
    public void testDefaultDeflate() {
        login();
        // start the page
        tester.startPage(WCSAdminPage.class);
        // change to wcs tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        // mandatory fields
        form.setValue(WCS_ADMIN_PANEL + ":maxInputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxOutputMemory", "1");
        form.setValue(WCS_ADMIN_PANEL + ":maxRequestedDimensionValues", "1");

        form.select(WCS_ADMIN_PANEL + ":defaultLocale", 11);
        form.setValue(WCS_ADMIN_PANEL + ":defaultDeflateCompressionLevel", "20");
        form.submit();
        // there should be an error
        tester.assertErrorMessages("The value of 'Default Deflate Compression Level' must be between 1 and 9.");
    }
}
