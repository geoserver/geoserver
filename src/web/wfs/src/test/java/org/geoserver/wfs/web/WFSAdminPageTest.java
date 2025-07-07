/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.junit.Test;

public class WFSAdminPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() throws Exception {
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);

        login();
        tester.startPage(WFSAdminPage.class);
        tester.assertModelValue("form:maxFeatures", wfs.getMaxFeatures());
        tester.assertModelValue("form:csvDateFormat", wfs.getCsvDateFormat());
        tester.assertModelValue("form:maxNumberOfFeaturesForPreview", wfs.getMaxNumberOfFeaturesForPreview());
        tester.assertModelValue("form:keywords", wfs.getKeywords());
        tester.assertModelValue(
                "form:getFeatureOutputTypes:outputTypeCheckingEnabled", wfs.isGetFeatureOutputTypeCheckingEnabled());
    }

    @Test
    public void testChangesToValues() throws Exception {
        String testValue1 = "100", testValue2 = "0";
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        login();
        tester.startPage(WFSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("maxNumberOfFeaturesForPreview", testValue1);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int) wfs.getMaxNumberOfFeaturesForPreview());
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("maxNumberOfFeaturesForPreview", testValue2);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue2 = 0", 0, (int) wfs.getMaxNumberOfFeaturesForPreview());
        // test allowGlobalQueries
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("allowGlobalQueries", false);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("allowGlobalQueries = false", false, wfs.getAllowGlobalQueries());
        // test includeWFSRequestDumpFile
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("includeWFSRequestDumpFile", false);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertFalse("includeWFSRequestDumpFile= false", wfs.getIncludeWFSRequestDumpFile());
        // test includeOutputTypes
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("getFeatureOutputTypes:outputTypeCheckingEnabled", true);
        ft.getForm().get("getFeatureOutputTypes:palette").setDefaultModelObject(Collections.singleton("KML"));
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertTrue("getFeatureOutputTypeCheckingEnabled= true", wfs.isGetFeatureOutputTypeCheckingEnabled());
        assertEquals("getFeatureOutputTypes= KML", Collections.singleton("KML"), wfs.getGetFeatureOutputTypes());
        // test disableStoredQueries
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("disableStoredQueriesManagement", true);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertTrue("disableStoredQueriesManagement = true", wfs.isDisableStoredQueriesManagement());
    }

    @Test
    public void testApply() throws Exception {
        String testValue1 = "100";
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        login();
        tester.startPage(WFSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("maxNumberOfFeaturesForPreview", testValue1);
        ft.submit("apply");
        // did not switch
        tester.assertRenderedPage(WFSAdminPage.class);
        // value was updated
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int) wfs.getMaxNumberOfFeaturesForPreview());
        // test Apply includeWFSRequestDumpFile
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("includeWFSRequestDumpFile", true);
        ft.submit("apply");
        // did not switch
        tester.assertRenderedPage(WFSAdminPage.class);
        // value was updated
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertTrue("includeWFSRequestDumpFile = true", wfs.getIncludeWFSRequestDumpFile());
    }

    @Test
    public void testGML32ForceMimeType() throws Exception {
        // make sure GML MIME type overriding is disabled
        WFSInfo info = getGeoServer().getService(WFSInfo.class);
        GMLInfo gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        gmlInfo.setMimeTypeToForce(null);
        getGeoServer().save(info);
        // login with administrator privileges
        login();
        // start WFS service administration page
        tester.startPage(new WFSAdminPage());
        // check that GML MIME type overriding is disabled
        tester.assertComponent("form:gml32:forceGmlMimeType", CheckBox.class);
        CheckBox checkbox = (CheckBox) tester.getComponentFromLastRenderedPage("form:gml32:forceGmlMimeType");
        assertThat(checkbox.getModelObject(), is(false));
        // MIME type drop down choice should be invisible
        tester.assertInvisible("form:gml32:mimeTypeToForce");
        // activate MIME type overriding by clicking in the checkbox
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("gml32:forceGmlMimeType", true);
        tester.executeAjaxEvent("form:gml32:forceGmlMimeType", "click");
        formTester = tester.newFormTester("form");
        formTester.submit("submit");
        // GML MIME typing overriding should be activated now
        tester.startPage(new WFSAdminPage());
        assertThat(checkbox.getModelObject(), is(true));
        tester.assertVisible("form:gml32:mimeTypeToForce");
        // WFS global service configuration should have been updated too
        info = getGeoServer().getService(WFSInfo.class);
        gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        assertThat(gmlInfo.getMimeTypeToForce().isPresent(), is(true));
        // select text / xml as MIME type to force
        formTester = tester.newFormTester("form");
        formTester.select("gml32:mimeTypeToForce", 2);
        tester.executeAjaxEvent("form:gml32:mimeTypeToForce", "change");
        formTester = tester.newFormTester("form");
        formTester.submit("submit");
        // WFS global service configuration should be forcing text / xml
        info = getGeoServer().getService(WFSInfo.class);
        gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        assertThat(gmlInfo.getMimeTypeToForce().isPresent(), is(true));
        assertThat(gmlInfo.getMimeTypeToForce().get(), is("text/xml"));
        // deactivate GML MIME type overriding by clicking in the checkbox
        tester.startPage(new WFSAdminPage());
        formTester = tester.newFormTester("form");
        formTester.setValue("gml32:forceGmlMimeType", false);
        tester.executeAjaxEvent("form:gml32:forceGmlMimeType", "click");
        formTester = tester.newFormTester("form");
        formTester.submit("submit");
        // GML MIME type overriding should be deactivated now
        tester.startPage(new WFSAdminPage());
        assertThat(checkbox.getModelObject(), is(true));
        tester.assertInvisible("form:gml32:mimeTypeToForce");
        // WFS global service configuration should have been updated too
        info = getGeoServer().getService(WFSInfo.class);
        gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        assertThat(gmlInfo.getMimeTypeToForce().isPresent(), is(false));
    }

    @Test
    public void testInternationalContent() {
        login();
        // start WFS service administration page
        tester.startPage(new WFSAdminPage());
        FormTester form = tester.newFormTester("form");
        // enable i18n for title
        form.setValue("serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew", "click");

        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew", "click");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");

        // enable i18n for abstract
        form.setValue("serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew", "click");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew", "click");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");
        form = tester.newFormTester("form");
        form.submit("submit");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testDefaultLocale() {
        login();
        tester.startPage(WFSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("defaultLocale", 11);
        ft.submit("submit");
        assertNotNull(getGeoServer().getService(WFSInfo.class).getDefaultLocale());
    }

    @Test
    public void testDateFormat() {
        login();
        tester.startPage(WFSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("csvDateFormat", "yyyy-MM-dd'T'HH:mm:ss'Z'");
        ft.submit("submit");
        assertNotNull(getGeoServer().getService(WFSInfo.class).getCsvDateFormat());
        assertEquals(getGeoServer().getService(WFSInfo.class).getCsvDateFormat(), "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }
}
