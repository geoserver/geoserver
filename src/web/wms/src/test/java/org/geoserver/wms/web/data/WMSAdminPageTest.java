package org.geoserver.wms.web.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.web.WMSAdminPage;

public class WMSAdminPageTest extends GeoServerWicketTestSupport {
    
    private WMSInfo wms;

    @Override
    protected void setUpInternal() throws Exception {
        wms = getGeoServerApplication().getGeoServer().getService(WMSInfo.class);
        login();
    }
    
    public void testValues() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.assertModelValue("form:keywords", wms.getKeywords());
        tester.assertModelValue("form:srs", new ArrayList<String>());
    }
    
    public void testFormSubmit() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }
    
    public void testWatermarkLocalFile() throws Exception {
        File f = new File(getClass().getResource("GeoServer_75.png").toURI());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("watermark.uRL", f.getAbsolutePath());
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }
    
    public void testFormInvalid() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("srs", "bla");
        ft.submit("submit");
        List errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertTrue(((ValidationErrorFeedback)errors.get(0)).getMessage().contains("bla"));
        tester.assertRenderedPage(WMSAdminPage.class);
    }

    public void testBBOXForEachCRS() throws Exception {
        assertFalse(wms.isBBOXForEachCRS());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("bBOXForEachCRS", true);
        ft.submit("submit");
        assertTrue(wms.isBBOXForEachCRS());
    }
}
