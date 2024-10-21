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

    private WMSInfo wms;

    @Before
    public void setUp() throws Exception {
        wms = getGeoServerApplication().getGeoServer().getService(WMSInfo.class);
        login();
    }

    @Test
    public void testValues() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.assertModelValue("form:keywords", wms.getKeywords());
        tester.assertModelValue("form:srs", new ArrayList<>());
    }

    @Test
    public void testFormSubmit() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testWatermarkLocalFile() throws Exception {
        File f = new File(getClass().getResource("GeoServer_75.png").toURI());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("watermark.uRL", f.getAbsolutePath());
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testFormInvalid() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("srs", "bla");
        ft.submit("submit");
        List errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertTrue(
                ((ValidationErrorFeedback) errors.get(0)).getMessage().toString().contains("bla"));
        tester.assertRenderedPage(WMSAdminPage.class);
    }

    @Test
    public void testBBOXForEachCRS() throws Exception {
        assertFalse(wms.isBBOXForEachCRS());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("bBOXForEachCRS", true);
        ft.submit("submit");
        assertTrue(wms.isBBOXForEachCRS());
    }

    @Test
    public void testRootLayerRemove() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("rootLayerEnabled", false);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY), false);
    }

    @Test
    public void testRootLayerTitle() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("rootLayerTitleAndAbstract:title", "test");
        ft.setValue("rootLayerTitleAndAbstract:abstract", "abstract test");
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getRootLayerTitle(), "test");
        assertEquals(wms.getRootLayerAbstract(), "abstract test");
    }

    @Test
    public void testDensification() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("aph.densify", true);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY), true);
    }

    @Test
    public void testDisableWrappingHeuristic() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("aph.dlh", true);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.DATELINE_WRAPPING_HEURISTIC_KEY), true);
    }

    @Test
    public void testDynamicStylingDisabled() throws Exception {
        assertFalse(wms.isDynamicStylingDisabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("dynamicStyling.disabled", true);
        ft.submit("submit");
        assertTrue(wms.isDynamicStylingDisabled());
    }

    @Test
    public void testCacheConfiguration() throws Exception {
        assertFalse(wms.getCacheConfiguration().isEnabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("cacheConfiguration.enabled", true);
        ft.submit("submit");
        assertTrue(wms.getCacheConfiguration().isEnabled());
    }

    @Test
    public void testFeaturesReprojectionDisabled() throws Exception {
        assertFalse(wms.isFeaturesReprojectionDisabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("disableFeaturesReproject", true);
        ft.submit("submit");
        assertTrue(wms.isFeaturesReprojectionDisabled());
    }

    @Test
    public void testTransformFeatureInfoDisabled() throws Exception {
        assertFalse(wms.isTransformFeatureInfoDisabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("disableTransformFeatureInfo", true);
        ft.submit("submit");
        assertTrue(wms.isTransformFeatureInfoDisabled());
    }

    @Test
    public void testAutoEscapeTemplateValues() throws Exception {
        assertFalse(wms.isAutoEscapeTemplateValues());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("autoEscapeTemplateValues", true);
        ft.submit("submit");
        assertTrue(wms.isAutoEscapeTemplateValues());
    }

    @Test
    public void testIncludeDefaultGroupStyleInCapabilitiesDisabled() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("defaultGroupStyleEnabled", false);
        ft.submit("submit");
        assertFalse(wms.isDefaultGroupStyleEnabled());
    }

    @Test
    public void testIncludeDefaultGroupStyleInCapabilitiesEnabled() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("defaultGroupStyleEnabled", true);
        ft.submit("submit");
        assertTrue(wms.isDefaultGroupStyleEnabled());
    }

    @Test
    public void testInternationalContent() {
        tester.startPage(WMSAdminPage.class);
        FormTester form = tester.newFormTester("form");
        // enable i18n for title and add two entries
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        // enable i18n for abstract and add two entries
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        // figure out the locales used in the test (might not be stable across JVMs)
        @SuppressWarnings("unchecked")
        DropDownChoice<Locale> select =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select");
        Locale l10 = select.getChoices().get(10);
        Locale l20 = select.getChoices().get(20);

        // fill the form (don't do this in between ajax calls)
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title for WMS");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title for WMS");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract for WMS");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international abstract for WMS");

        // mandatory fields
        form.setValue("maxRenderingTime", "999");
        form.setValue("maxRequestMemory", "99999");
        form.setValue("maxRenderingErrors", "1");
        form.setValue("maxBuffer", "99");
        form.setValue("maxRequestedDimensionValues", "2");
        form.setValue("watermark.transparency", "5");
        form.setValue("cacheConfiguration.maxEntries", "1000");
        form.setValue("cacheConfiguration.maxEntrySize", "100000");
        form.setValue("remoteStyleTimeout", "9999");
        form.setValue("remoteStyleMaxRequestTime", "99");

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
        FormTester ft = tester.newFormTester("form");
        ft.select("defaultLocale", 11);
        ft = tester.newFormTester("form");
        ft.submit("submit");
        assertNotNull(getGeoServer().getService(WMSInfo.class).getDefaultLocale());
    }

    @Test
    public void testAllowedUrlsAuth() {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        String allowedUrlForAuthForwarding = "http://localhost:8080/geoserver/rest/sldremote";
        ft.setValue("allowedURLsForAuthForwarding", allowedUrlForAuthForwarding);
        ft.submit("submit");
        assertEquals(
                allowedUrlForAuthForwarding,
                getGeoServer().getService(WMSInfo.class).getAllowedURLsForAuthForwarding().get(0));

        tester.startPage(WMSAdminPage.class);
        ft = tester.newFormTester("form");
        allowedUrlForAuthForwarding =
                "invalid-remote-url\n" + "htPP://invalidhttpurl\n" + "http://validurl";

        ft.setValue("allowedURLsForAuthForwarding", allowedUrlForAuthForwarding);
        ft.submit("submit");
        String reportedInvalidURLs = "invalid-remote-url, htPP://invalidhttpurl";
        tester.assertErrorMessages(
                String.format(
                        "The provided values are not valid HTTP urls: [%s]", reportedInvalidURLs));
    }

    /** Comprehensive test as a drop-down with default value is a tricky component to test. */
    @Test
    public void testInvalidDimensionsFlag() throws Exception {
        wms.setExceptionOnInvalidDimension(true);
        getGeoServer().save(wms);

        // set to false
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("exceptionOnInvalidDimension", 1);
        ft.submit("submit");
        assertFalse(wms.isExceptionOnInvalidDimension());

        // reset to default
        tester.startPage(WMSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("exceptionOnInvalidDimension", null);
        ft.submit("submit");
        assertNull(wms.isExceptionOnInvalidDimension());

        // set to true
        tester.startPage(WMSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.select("exceptionOnInvalidDimension", 0);
        ft.submit("submit");
        assertTrue(wms.isExceptionOnInvalidDimension());
    }
}
