/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.*;

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
        ft.setValue("rootLayerTitle", "test");
        ft.setValue("rootLayerAbstract", "abstract test");
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
}
