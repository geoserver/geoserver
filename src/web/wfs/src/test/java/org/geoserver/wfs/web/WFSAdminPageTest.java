/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import static org.geoserver.web.services.BaseServiceAdminPage.WORKSPACE_ADMIN_SERVICE_ACCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.web.GeoServerLoginPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geotools.api.util.InternationalString;
import org.junit.Before;
import org.junit.Test;

public class WFSAdminPageTest extends GeoServerWicketTestSupport {
    /** Location of WFSAdminPanel within form */
    final String SERVICE_ADMIN_PANEL = "tabs:panel";
    /** Location of WFSAdminPanel within form */
    final String WFS_ADMIN_PANEL = "tabs:panel:initial";

    /** Location of GMLPanel within form */
    final String GML_ADMIN_PANEL = "tabs:panel:initial";

    private static final String ROLE_CITE = "ROLE_CITE";
    public static final String CITE_WFS_TITLE = "This is the CITE WFS service";
    public static final String GLOBAL_WFS_TITLE = "This is the global WFS service";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void cleanupService() {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setTitle(null);
        gs.save(wfs);

        WorkspaceInfo citeWorkspace = getCatalog().getWorkspaceByName("cite");
        WFSInfo citeWFS = gs.getService(citeWorkspace, WFSInfo.class);
        if (citeWFS != null) {
            gs.remove(citeWFS);
        }
    }

    @Test
    public void testValues() throws Exception {
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);

        login();
        tester.startPage(WFSAdminPage.class);
        // initial service tab
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":keywords", wfs.getKeywords());

        // change to wfs tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertModelValue("form:" + WFS_ADMIN_PANEL + ":maxFeatures", wfs.getMaxFeatures());
        tester.assertModelValue("form:" + WFS_ADMIN_PANEL + ":csvDateFormat", wfs.getCsvDateFormat());
        tester.assertModelValue(
                "form:" + WFS_ADMIN_PANEL + ":maxNumberOfFeaturesForPreview", wfs.getMaxNumberOfFeaturesForPreview());
        tester.assertModelValue(
                "form:" + WFS_ADMIN_PANEL + ":getFeatureOutputTypes:outputTypeCheckingEnabled",
                wfs.isGetFeatureOutputTypeCheckingEnabled());
    }

    @Test
    public void testChangesToValues() throws Exception {
        WFSInfo wfs;

        login();

        // open page, and change to WFS tab
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        FormTester form = tester.newFormTester("form");

        // test maxNumberOfFeaturesForPreview
        form.setValue(WFS_ADMIN_PANEL + ":maxNumberOfFeaturesForPreview", "100");
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int) wfs.getMaxNumberOfFeaturesForPreview());

        // test maxNumberOfFeaturesForPreview
        form = tester.newFormTester("form");
        // tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form.setValue(WFS_ADMIN_PANEL + ":maxNumberOfFeaturesForPreview", "0");
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue2 = 0", 0, (int) wfs.getMaxNumberOfFeaturesForPreview());

        // test allowGlobalQueries
        form = tester.newFormTester("form");
        // tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form.setValue(WFS_ADMIN_PANEL + ":allowGlobalQueries", false);
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("allowGlobalQueries = false", false, wfs.getAllowGlobalQueries());

        // test includeWFSRequestDumpFile
        form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":includeWFSRequestDumpFile", false);
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertFalse("includeWFSRequestDumpFile= false", wfs.getIncludeWFSRequestDumpFile());

        // test includeOutputTypes
        form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":getFeatureOutputTypes:outputTypeCheckingEnabled", true);
        form.getForm()
                .get(WFS_ADMIN_PANEL + ":getFeatureOutputTypes:palette")
                .setDefaultModelObject(Collections.singleton("KML"));
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertTrue("getFeatureOutputTypeCheckingEnabled= true", wfs.isGetFeatureOutputTypeCheckingEnabled());
        assertEquals("getFeatureOutputTypes= KML", Collections.singleton("KML"), wfs.getGetFeatureOutputTypes());

        // test disableStoredQueries
        form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":disableStoredQueriesManagement", true);
        form.submit("apply");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertTrue("disableStoredQueriesManagement = true", wfs.isDisableStoredQueriesManagement());
    }

    @Test
    public void testSaveVsApply() throws Exception {
        WFSInfo wfs;

        login();
        // open page, and change to WFS tab
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        // Test Apply maxNumberOfFeaturesForPreview
        FormTester form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":maxNumberOfFeaturesForPreview", "100");
        form.submit("apply");
        // did not switch
        tester.assertRenderedPage(WFSAdminPage.class);
        // value was updated
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int) wfs.getMaxNumberOfFeaturesForPreview());

        // Test Save maxNumberOfFeaturesForPreview
        form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":maxNumberOfFeaturesForPreview", "100");
        form.submit("submit");
        // switched to different page
        Page page = tester.getLastRenderedPage();
        assertFalse("not wfs admin page", page.getClass().isAssignableFrom(WFSAdminPage.class));
        // value was updated
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int) wfs.getMaxNumberOfFeaturesForPreview());

        // test Apply includeWFSRequestDumpFile
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form = tester.newFormTester("form");
        form.setValue(WFS_ADMIN_PANEL + ":includeWFSRequestDumpFile", true);
        form.submit("apply");
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
        // start WFS service administration page, change to WFS Panel
        tester.startPage(new WFSAdminPage());
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        // check that GML MIME type overriding is disabled
        // form:tabs:panel:initial:gml32:forceGmlMimeType
        tester.assertComponent("form:" + GML_ADMIN_PANEL + ":gml32:forceGmlMimeType", CheckBox.class);
        CheckBox checkbox = (CheckBox)
                tester.getComponentFromLastRenderedPage("form:" + GML_ADMIN_PANEL + ":gml32:forceGmlMimeType");
        assertThat(checkbox.getModelObject(), is(false));

        // MIME type drop down choice should be invisible
        tester.assertInvisible("form:" + GML_ADMIN_PANEL + ":gml32:mimeTypeToForce");
        // activate MIME type overriding by clicking in the checkbox

        FormTester form = tester.newFormTester("form");
        form.setValue(GML_ADMIN_PANEL + ":gml32:forceGmlMimeType", true);
        tester.executeAjaxEvent("form:" + GML_ADMIN_PANEL + ":gml32:forceGmlMimeType", "click");

        form = tester.newFormTester("form");
        form.submit("submit");
        // Return to WFS service administration page, change to WFS Panel
        tester.startPage(new WFSAdminPage());
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        // GML MIME typing overriding should be activated now
        // tester.startPage(new WFSAdminPage());
        assertThat(checkbox.getModelObject(), is(true));
        tester.assertVisible("form:" + GML_ADMIN_PANEL + ":gml32:mimeTypeToForce");

        // WFS global service configuration should have been updated too
        info = getGeoServer().getService(WFSInfo.class);
        gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        assertThat(gmlInfo.getMimeTypeToForce().isPresent(), is(true));

        // select text / xml as MIME type to force
        form = tester.newFormTester("form");
        form.select(GML_ADMIN_PANEL + ":gml32:mimeTypeToForce", 2);
        tester.executeAjaxEvent("form:" + GML_ADMIN_PANEL + ":gml32:mimeTypeToForce", "change");

        form = tester.newFormTester("form");
        form.submit("submit");

        // Return to WFS service administration page, change to WFS Panel
        tester.startPage(new WFSAdminPage());
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        // WFS global service configuration should be forcing text / xml
        info = getGeoServer().getService(WFSInfo.class);
        gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        assertThat(gmlInfo.getMimeTypeToForce().isPresent(), is(true));
        assertThat(gmlInfo.getMimeTypeToForce().get(), is("text/xml"));

        // deactivate GML MIME type overriding by clicking in the checkbox
        form = tester.newFormTester("form");
        form.setValue(GML_ADMIN_PANEL + ":gml32:forceGmlMimeType", false);
        tester.executeAjaxEvent("form:" + GML_ADMIN_PANEL + ":gml32:forceGmlMimeType", "click");

        form = tester.newFormTester("form");
        form.submit("submit");

        // Return to WFS service administration page, change to WFS Panel
        tester.startPage(new WFSAdminPage());
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        // GML MIME type overriding should be deactivated now
        assertThat(checkbox.getModelObject(), is(true));
        tester.assertInvisible("form:" + GML_ADMIN_PANEL + ":gml32:mimeTypeToForce");

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

        // enable i18n for title and add two entries
        form.setValue("tabs:panel:serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox", true);

        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        // enable i18n for abstract and add two entries
        form.setValue(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        // figure out the locales used in the test (might not be stable across JVMs)
        @SuppressWarnings("unchecked")
        DropDownChoice<Locale> select = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(
                        "form:tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select");
        Locale l10 = select.getChoices().get(10);
        Locale l20 = select.getChoices().get(20);

        // fill the form (don't do this in between ajax calls)
        form.select(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title for WFS");
        form.select(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title for WFS");
        form.select(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract for WFS");
        form.select(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "tabs:panel:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international abstract for WFS");

        // Change to WFS Tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form = tester.newFormTester("form");

        // mandatory fields
        form.setValue("tabs:panel:initial:maxFeatures", "999");
        form.select("tabs:panel:initial:encodeFeatureMember", 0);

        form.submit("submit");
        tester.assertNoErrorMessage();

        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        InternationalString internationalTitle = wfsInfo.getInternationalTitle();
        assertEquals("an international title for WFS", internationalTitle.toString(l10));
        assertEquals("another international title for WFS", internationalTitle.toString(l20));
        InternationalString internationalAbstract = wfsInfo.getInternationalAbstract();
        assertEquals("an international abstract for WFS", internationalAbstract.toString(l10));
        assertEquals("another international abstract for WFS", internationalAbstract.toString(l20));
    }

    @Test
    public void testDefaultLocale() {
        login();

        // open page, and change to WFS Tab
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        FormTester form = tester.newFormTester("form");

        form.select("tabs:panel:initial:defaultLocale", 11);
        form.submit("submit");
        assertNotNull(getGeoServer().getService(WFSInfo.class).getDefaultLocale());
    }

    @Test
    public void testDateFormat() {
        login();

        // open page, and change to WFS Tab
        tester.startPage(WFSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        FormTester form = tester.newFormTester("form");

        form.setValue(WFS_ADMIN_PANEL + ":csvDateFormat", "yyyy-MM-dd'T'HH:mm:ss'Z'");
        form.submit("apply");
        assertNotNull(getGeoServer().getService(WFSInfo.class).getCsvDateFormat());
        assertEquals(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                getGeoServer().getService(WFSInfo.class).getCsvDateFormat());
    }

    @Test
    public void testWorkspaceAdminFlagOn() throws IOException {
        System.setProperty(WORKSPACE_ADMIN_SERVICE_ACCESS, "true");
        // setup a CITE workspace WFS service
        WorkspaceInfo citeWorkspace = getCatalog().getWorkspaceByName("cite");
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setTitle(GLOBAL_WFS_TITLE);
        gs.save(wfs);

        WFSInfo citeWfs = gs.getFactory().create(WFSInfo.class);
        OwsUtils.copy(wfs, citeWfs, WFSInfo.class);
        citeWfs.setWorkspace(citeWorkspace);
        citeWfs.setTitle(CITE_WFS_TITLE);
        gs.add(citeWfs);

        // setup a sandbox by security config
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("cite.*.a", ROLE_CITE);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
        }
        DefaultFileAccessManager fam = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
        fam.reload();

        logout();

        // global service page, cannot be accessed as anonymous
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);

        // login as workspace admin (logout happens as @After in base class)
        login("cite", "pwd", ROLE_CITE);

        // global service page, still no joy, not a global admin
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(UnauthorizedPage.class);

        // ok, can access with the right workspace
        tester.startPage(WFSAdminPage.class, new PageParameters().add("workspace", "cite"));
        tester.assertRenderedPage(WFSAdminPage.class);

        // now log as admin and check the global service page is rendered
        loginAsAdmin();
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(WFSAdminPage.class);

        System.clearProperty(WORKSPACE_ADMIN_SERVICE_ACCESS);
    }

    @Test
    public void testWorkspaceAdminFlagOff() throws IOException {
        System.clearProperty(WORKSPACE_ADMIN_SERVICE_ACCESS);

        // setup a CITE workspace WFS service
        WorkspaceInfo citeWorkspace = getCatalog().getWorkspaceByName("cite");
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setTitle(GLOBAL_WFS_TITLE);
        gs.save(wfs);

        WFSInfo citeWfs = gs.getFactory().create(WFSInfo.class);
        OwsUtils.copy(wfs, citeWfs, WFSInfo.class);
        citeWfs.setWorkspace(citeWorkspace);
        citeWfs.setTitle(CITE_WFS_TITLE);
        gs.add(citeWfs);

        // setup a sandbox by security config
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("cite.*.a", ROLE_CITE);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
        }
        DefaultFileAccessManager fam = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
        fam.reload();

        logout();

        // global service page, cannot be accessed as anonymous
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);

        // login as workspace admin (logout happens as @After in base class)
        login("cite", "pwd", ROLE_CITE);

        // global service page, still no joy, not a global admin
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(UnauthorizedPage.class);

        // cannot access with the workspace
        tester.startPage(WFSAdminPage.class, new PageParameters().add("workspace", "cite"));
        tester.assertRenderedPage(UnauthorizedPage.class);

        // now log as admin and check the global service page is rendered
        loginAsAdmin();
        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(WFSAdminPage.class);
    }
}
