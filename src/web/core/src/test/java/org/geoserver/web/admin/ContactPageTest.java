package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.ContactInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;

public class ContactPageTest extends GeoServerWicketTestSupport {

    public void testValues() {
        ContactInfo info = getGeoServerApplication().getGeoServer().getGlobal().getContact();

        login();
        tester.startPage(ContactPage.class);
        tester.assertComponent("form:contact:address", TextField.class);
        tester.assertModelValue("form:contact:address", info.getAddress());
    }

    public void testSave() {
        login();
        tester.startPage(ContactPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("contact:address", "newAddress");
        ft.submit("submit");
        tester.assertRenderedPage(GeoServerHomePage.class);

        ContactInfo info = getGeoServerApplication().getGeoServer().getGlobal().getContact();
        assertEquals("newAddress", info.getAddress());
    }
}
