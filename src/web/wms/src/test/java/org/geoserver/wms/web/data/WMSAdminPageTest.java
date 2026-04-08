/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.web.WMSAdminPage;
import org.geotools.api.util.InternationalString;
import org.junit.Before;
import org.junit.Test;

public class WMSAdminPageTest extends GeoServerWicketTestSupport {
    /** Location of general service panel within form */
    final String SERVICE_ADMIN_PANEL = "tabs:panel";

    /** Location of WMSAdminPanel within form */
    final String WMS_ADMIN_PANEL = "tabs:panel:initial";

    private WMSInfo wms;

    @Before
    public void setUp() throws Exception {
        wms = getGeoServerApplication().getGeoServer().getService(WMSInfo.class);
        login();
    }

    @Test
    public void testValues() throws Exception {
        tester.startPage(WMSAdminPage.class);

        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":keywords", wms.getKeywords());

        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertModelValue("form:" + WMS_ADMIN_PANEL + ":srs", new ArrayList<>());
    }

    @Test
    public void testFormSubmit() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester form = tester.newFormTester("form");
        form.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testWatermarkLocalFile() throws Exception {
        File f = new File(getClass().getResource("GeoServer_75.png").toURI());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":watermark.uRL", f.getAbsolutePath());
        form.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testFormInvalid() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":srs", "bla");
        form.submit("submit");
        List errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertTrue(((ValidationErrorFeedback) errors.get(0))
                .getMessage()
                .toString()
                .contains("bla"));
        tester.assertRenderedPage(WMSAdminPage.class);
    }

    @Test
    public void testBBOXForEachCRS() throws Exception {
        assertFalse(wms.isBBOXForEachCRS());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":bBOXForEachCRS", true);
        form.submit("submit");
        assertTrue(wms.isBBOXForEachCRS());
    }

    @Test
    public void testRootLayerRemove() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":rootLayerEnabled", false);
        form.submit("submit");
        tester.assertNoErrorMessage();
        assertFalse((Boolean) wms.getMetadata().get(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY));
    }

    @Test
    public void testRootLayerTitle() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":rootLayerTitleAndAbstract:title", "test");
        form.setValue(WMS_ADMIN_PANEL + ":rootLayerTitleAndAbstract:abstract", "abstract test");
        form.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals("test", wms.getRootLayerTitle());
        assertEquals("abstract test", wms.getRootLayerAbstract());
    }

    @Test
    public void testDensification() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":aph.densify", true);
        form.submit("submit");
        tester.assertNoErrorMessage();
        assertTrue((Boolean) wms.getMetadata().get(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY));
    }

    @Test
    public void testDisableWrappingHeuristic() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":aph.dlh", true);
        form.submit("submit");
        tester.assertNoErrorMessage();
        assertTrue((Boolean) wms.getMetadata().get(WMS.DATELINE_WRAPPING_HEURISTIC_KEY));
    }

    @Test
    public void testDynamicStylingDisabled() throws Exception {
        assertFalse(wms.isDynamicStylingDisabled());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":dynamicStyling.disabled", true);
        form.submit("submit");
        assertTrue(wms.isDynamicStylingDisabled());
    }

    @Test
    public void testCacheConfiguration() throws Exception {
        assertFalse(wms.getCacheConfiguration().isEnabled());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":cacheConfiguration.enabled", true);
        form.submit("submit");
        assertTrue(wms.getCacheConfiguration().isEnabled());
    }

    @Test
    public void testFeaturesReprojectionDisabled() throws Exception {
        assertFalse(wms.isFeaturesReprojectionDisabled());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":disableFeaturesReproject", true);
        form.submit("submit");
        assertTrue(wms.isFeaturesReprojectionDisabled());
    }

    @Test
    public void testTransformFeatureInfoDisabled() throws Exception {
        assertFalse(wms.isTransformFeatureInfoDisabled());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":disableTransformFeatureInfo", true);
        form.submit("submit");
        assertTrue(wms.isTransformFeatureInfoDisabled());
    }

    @Test
    public void testAutoEscapeTemplateValues() throws Exception {
        assertFalse(wms.isAutoEscapeTemplateValues());
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":autoEscapeTemplateValues", true);
        form.submit("submit");
        assertTrue(wms.isAutoEscapeTemplateValues());
    }

    @Test
    public void testIncludeDefaultGroupStyleInCapabilitiesDisabled() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":defaultGroupStyleEnabled", false);
        form.submit("submit");
        assertFalse(wms.isDefaultGroupStyleEnabled());
    }

    @Test
    public void testIncludeDefaultGroupStyleInCapabilitiesEnabled() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":defaultGroupStyleEnabled", true);
        form.submit("submit");
        assertTrue(wms.isDefaultGroupStyleEnabled());
    }

    @Test
    public void testInternationalContent() {
        tester.startPage(WMSAdminPage.class);

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
                "an international title for WMS");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title for WMS");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract for WMS");
        form.select(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                SERVICE_ADMIN_PANEL
                        + ":serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international abstract for WMS");

        // mandatory fields
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":maxRenderingTime", "999");
        form.setValue(WMS_ADMIN_PANEL + ":maxRequestMemory", "99999");
        form.setValue(WMS_ADMIN_PANEL + ":maxRenderingErrors", "1");
        form.setValue(WMS_ADMIN_PANEL + ":maxBuffer", "99");
        form.setValue(WMS_ADMIN_PANEL + ":maxRequestedDimensionValues", "2");
        form.setValue(WMS_ADMIN_PANEL + ":watermark.transparency", "5");
        form.setValue(WMS_ADMIN_PANEL + ":cacheConfiguration.maxEntries", "1000");
        form.setValue(WMS_ADMIN_PANEL + ":cacheConfiguration.maxEntrySize", "100000");
        form.setValue(WMS_ADMIN_PANEL + ":remoteStyleTimeout", "9999");
        form.setValue(WMS_ADMIN_PANEL + ":remoteStyleMaxRequestTime", "99");

        form.submit("submit");
        tester.assertNoErrorMessage();
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        InternationalString internationalTitle = wmsInfo.getInternationalTitle();
        assertEquals("an international title for WMS", internationalTitle.toString(l10));
        assertEquals("another international title for WMS", internationalTitle.toString(l20));
        InternationalString internationalAbstract = wmsInfo.getInternationalAbstract();
        assertEquals("an international abstract for WMS", internationalAbstract.toString(l10));
        assertEquals("another international abstract for WMS", internationalAbstract.toString(l20));
    }

    @Test
    public void testDefaultLocale() {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.select(WMS_ADMIN_PANEL + ":defaultLocale", 11);
        form = tester.newFormTester("form");
        form.submit("submit");
        assertNotNull(getGeoServer().getService(WMSInfo.class).getDefaultLocale());
    }

    @Test
    public void testAllowedUrlsAuth() {
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        String allowedUrlForAuthForwarding = "http://localhost:8080/geoserver/rest/sldremote";
        form.setValue(WMS_ADMIN_PANEL + ":allowedURLsForAuthForwarding", allowedUrlForAuthForwarding);
        form.submit("submit");
        assertEquals(
                allowedUrlForAuthForwarding,
                getGeoServer()
                        .getService(WMSInfo.class)
                        .getAllowedURLsForAuthForwarding()
                        .get(0));

        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        form = tester.newFormTester("form");
        allowedUrlForAuthForwarding = "invalid-remote-url\n" + "htPP://invalidhttpurl\n" + "http://validurl";

        form.setValue(WMS_ADMIN_PANEL + ":allowedURLsForAuthForwarding", allowedUrlForAuthForwarding);
        form.submit("submit");
        String reportedInvalidURLs = "invalid-remote-url, htPP://invalidhttpurl";
        tester.assertErrorMessages("The provided values are not valid HTTP urls: [%s]".formatted(reportedInvalidURLs));
    }

    /** Comprehensive test as a drop-down with default value is a tricky component to test. */
    @Test
    public void testInvalidDimensionsFlag() throws Exception {
        wms.setExceptionOnInvalidDimension(true);
        getGeoServer().save(wms);

        // set to false
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");

        FormTester form = tester.newFormTester("form");
        form.select(WMS_ADMIN_PANEL + ":exceptionOnInvalidDimension", 1);
        form.submit("submit");

        assertFalse(wms.isExceptionOnInvalidDimension());

        // reset to default
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form = tester.newFormTester("form");
        form.setValue(WMS_ADMIN_PANEL + ":exceptionOnInvalidDimension", null);
        form.submit("submit");
        assertNull(wms.isExceptionOnInvalidDimension());

        // set to true
        tester.startPage(WMSAdminPage.class);
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form = tester.newFormTester("form");
        form.select(WMS_ADMIN_PANEL + ":exceptionOnInvalidDimension", 0);
        form.submit("submit");
        assertTrue(wms.isExceptionOnInvalidDimension());
    }
}
