/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.junit.Assert.*;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.ContactInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class ContactPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testValues() {
        ContactInfo info =
                getGeoServerApplication().getGeoServer().getGlobal().getSettings().getContact();

        login();
        tester.startPage(ContactPage.class);
        tester.assertComponent("form:contact:address", TextField.class);
        tester.assertModelValue("form:contact:address", info.getAddress());
    }

    @Test
    public void testSave() {
        login();
        tester.startPage(ContactPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("contact:address", "newAddress");
        ft.submit("submit");
        tester.assertRenderedPage(GeoServerHomePage.class);

        ContactInfo info =
                getGeoServerApplication().getGeoServer().getGlobal().getSettings().getContact();
        assertEquals("newAddress", info.getAddress());
    }
}
