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
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.web.WMSAdminPage;
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
        tester.assertModelValue("form:srs", new ArrayList<String>());
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
        assertTrue(wms.isDefaultGroupStyleEnabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("defaultGroupStyleEnabled", false);
        ft.submit("submit");
        assertFalse(wms.isDefaultGroupStyleEnabled());
    }

    @Test
    public void testInternationalContent() {
        tester.startPage(WMSAdminPage.class);
        FormTester form = tester.newFormTester("form");
        // enable i18n for title
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalTitle:container:addNew",
                "click");
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
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        form.select(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "form:serviceTitleAndAbstract:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
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
