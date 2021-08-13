/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.util.GrowableInternationalString;
import org.junit.Before;
import org.junit.Test;
import org.opengis.util.InternationalString;

public class ContactPageTest extends GeoServerWicketTestSupport {

    private GeoServer geoServer;

    @Before
    public void reset() {
        geoServer = getGeoServerApplication().getGeoServer();
        GeoServerInfo info = geoServer.getGlobal();
        ContactInfo contact = new ContactInfoImpl();
        contact.setAddress("My address");
        info.getSettings().setContact(contact);
        geoServer.save(info);
    }

    @Test
    public void testValues() {

        ContactInfo info = geoServer.getGlobal().getSettings().getContact();

        login();
        tester.startPage(ContactPage.class);
        tester.assertComponent("form:contact:address:stringField", TextField.class);
        tester.assertModelValue("form:contact:address:stringField", info.getAddress());
    }

    @Test
    public void testSave() {
        login();
        tester.startPage(ContactPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("contact:address:stringField", "newAddress1");
        ft = tester.newFormTester("form");
        ft.submit("submit");
        tester.assertRenderedPage(GeoServerHomePage.class);

        ContactInfo info =
                getGeoServerApplication().getGeoServer().getGlobal().getSettings().getContact();
        assertEquals("newAddress1", info.getAddress());
    }

    @Test
    public void testApply() {
        login();
        tester.startPage(ContactPage.class);

        FormTester ft = tester.newFormTester("form");
        ft.setValue("contact:address:stringField", "newAddress2");
        ft.submit("apply");
        tester.assertRenderedPage(ContactPage.class);

        ContactInfo info =
                getGeoServerApplication().getGeoServer().getGlobal().getSettings().getContact();
        assertEquals("newAddress2", info.getAddress());
    }

    @Test
    public void testInternationalContent() {

        login();
        tester.startPage(ContactPage.class);
        FormTester ft = tester.newFormTester("form");

        // contact person
        ft.setValue("contact:contactPerson:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:contactPerson:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:contactPerson:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:contactPerson:internationalField:container:addNew", "click");
        ft.select(
                "contact:contactPerson:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:contactPerson:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // organization
        ft.setValue("contact:contactOrganization:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:contactOrganization:labelContainer:labelContainer_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:contact:contactOrganization:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:contactOrganization:internationalField:container:addNew", "click");
        ft.select(
                "contact:contactOrganization:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:contactOrganization:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // email
        ft.setValue("contact:contactEmail:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:contactEmail:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:contactEmail:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:contactEmail:internationalField:container:addNew", "click");

        ft.select(
                "contact:contactEmail:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:contactEmail:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // contactFax
        ft.setValue("contact:contactFacsimile:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:contactFacsimile:labelContainer:labelContainer_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:contact:contactFacsimile:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:contactFacsimile:internationalField:container:addNew", "click");
        ft.select(
                "contact:contactFacsimile:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:contactFacsimile:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // contactVoice
        ft.setValue("contact:contactVoice:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:contactVoice:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:contactVoice:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:contactVoice:internationalField:container:addNew", "click");
        ft.select(
                "contact:contactVoice:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:contactVoice:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // addressType
        ft.setValue("contact:addressType:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:addressType:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:addressType:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:addressType:internationalField:container:addNew", "click");
        ft.select(
                "contact:addressType:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:addressType:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // address

        ft.setValue("contact:address:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:address:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:address:internationalField:container:addNew", "click");

        ft.select(
                "contact:address:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:address:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // addressCity

        ft.setValue("contact:addressCity:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:addressCity:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:addressCity:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:addressCity:internationalField:container:addNew", "click");
        ft.select(
                "contact:addressCity:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:addressCity:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // addressCountry
        ft.setValue("contact:addressCountry:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:addressCountry:labelContainer:labelContainer_i18nCheckbox", "change");
        tester.executeAjaxEvent(
                "form:contact:addressCountry:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:addressCountry:internationalField:container:addNew", "click");
        ft.select(
                "contact:addressCountry:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:addressCountry:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // addressDeliveryPoint
        ft.setValue(
                "contact:addressDeliveryPoint:labelContainer:labelContainer_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "form:contact:addressDeliveryPoint:labelContainer:labelContainer_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "form:contact:addressDeliveryPoint:internationalField:container:addNew", "click");
        tester.executeAjaxEvent(
                "form:contact:addressDeliveryPoint:internationalField:container:addNew", "click");
        ft.select(
                "contact:addressDeliveryPoint:internationalField:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                20);
        ft.select(
                "contact:addressDeliveryPoint:internationalField:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                22);

        // set values to text field all at once to make the form tester flush the select values
        ft = tester.newFormTester("form");

        ft.setValue(
                "contact:contactPerson:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "A i18n contact person");
        ft.setValue(
                "contact:contactPerson:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "Another i18n contact person");
        ft.setValue(
                "contact:contactOrganization:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "A i18n contact organization");
        ft.setValue(
                "contact:contactOrganization:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "Another i18n contact organization");
        ft.setValue(
                "contact:contactEmail:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "first@i18n.org");
        ft.setValue(
                "contact:contactEmail:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "second@i18n.org");

        ft.setValue(
                "contact:contactVoice:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "0000010001");
        ft.setValue(
                "contact:contactVoice:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "111000111000");

        ft.setValue(
                "contact:contactFacsimile:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "0000022220001");
        ft.setValue(
                "contact:contactFacsimile:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "333444443333");

        ft.setValue(
                "contact:addressType:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "work");
        ft.setValue(
                "contact:addressType:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "work");
        ft.setValue(
                "contact:address:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "Avenida Atlantica 101");
        ft.setValue(
                "contact:address:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "via di Sotterra 12");
        ft.setValue(
                "contact:addressCity:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "Eudossia");
        ft.setValue(
                "contact:addressCity:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "Pentesilea");

        ft.setValue(
                "contact:addressCountry:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "Country of Eudossia");
        ft.setValue(
                "contact:addressCountry:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "Country of Pentesilea");

        ft.setValue(
                "contact:addressDeliveryPoint:internationalField:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "Delivery Point of Eudossia");
        ft.setValue(
                "contact:addressDeliveryPoint:internationalField:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "Delivery Point of Pentesilea");
        tester.debugComponentTrees();
        ft.submit("submit");
        ContactInfo contactInfo = getGeoServer().getSettings().getContact();
        assertI18NContent(contactInfo.getInternationalContactPerson());
        assertI18NContent(contactInfo.getInternationalContactOrganization());
        assertI18NContent(contactInfo.getInternationalContactEmail());
        assertI18NContent(contactInfo.getInternationalContactVoice());
        assertI18NContent(contactInfo.getInternationalContactFacsimile());
        assertI18NContent(contactInfo.getInternationalAddress());
        assertI18NContent(contactInfo.getInternationalAddressType());
        assertI18NContent(contactInfo.getInternationalAddressCity());
        assertI18NContent(contactInfo.getInternationalAddressCountry());
        assertI18NContent(contactInfo.getInternationalAddressDeliveryPoint());
    }

    private void assertI18NContent(InternationalString internationalString) {
        GrowableInternationalString growable = (GrowableInternationalString) internationalString;
        growable.getLocales()
                .forEach(l -> assertTrue(internationalString.toString(l).length() > 0));
    }
}
