/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;
import org.geoserver.web.EmailAddressValidator;
import org.geoserver.web.StringAndInternationalStringPanel;

public class ContactPanel extends Panel {

    public ContactPanel(String id, final IModel<ContactInfo> model) {
        super(id, model);
        String contactPerson = "contactPerson";
        add(
                new StringAndInternationalStringPanel(
                        contactPerson,
                        model,
                        contactPerson,
                        contactPerson,
                        "internationalContactPerson",
                        this,
                        null));
        String contactOrg = "contactOrganization";
        add(
                new StringAndInternationalStringPanel(
                        contactOrg,
                        model,
                        contactOrg,
                        contactOrg,
                        "internationalContactOrganization",
                        this,
                        null));
        String contactPosition = "contactPosition";
        add(
                new StringAndInternationalStringPanel(
                        contactPosition,
                        model,
                        contactPosition,
                        contactPosition,
                        "internationalContactPosition",
                        this,
                        null));
        String addressType = "addressType";
        add(
                new StringAndInternationalStringPanel(
                        addressType,
                        model,
                        addressType,
                        addressType,
                        "internationalAddressType",
                        this,
                        null));
        String address = "address";
        add(
                new StringAndInternationalStringPanel(
                        address, model, address, address, "internationalAddress", this, null));
        String addressDeliveryPt = "addressDeliveryPoint";
        add(
                new StringAndInternationalStringPanel(
                        addressDeliveryPt,
                        model,
                        addressDeliveryPt,
                        addressDeliveryPt,
                        "internationalAddressDeliveryPoint",
                        this,
                        null));
        String addressCity = "addressCity";
        add(
                new StringAndInternationalStringPanel(
                        addressCity,
                        model,
                        addressCity,
                        addressCity,
                        "internationalAddressCity",
                        this,
                        null));
        String addressState = "addressState";
        add(
                new StringAndInternationalStringPanel(
                        addressState,
                        model,
                        addressState,
                        addressState,
                        "internationalAddressState",
                        this,
                        null));
        String addressPostalCode = "addressPostalCode";
        add(
                new StringAndInternationalStringPanel(
                        addressPostalCode,
                        model,
                        addressPostalCode,
                        addressPostalCode,
                        "internationalAddressPostalCode",
                        this,
                        null));
        String addressCountry = "addressCountry";
        add(
                new StringAndInternationalStringPanel(
                        addressCountry,
                        model,
                        addressCountry,
                        addressCountry,
                        "internationalAddressCountry",
                        this,
                        null));
        String contactVoice = "contactVoice";
        add(
                new StringAndInternationalStringPanel(
                        contactVoice,
                        model,
                        contactVoice,
                        contactVoice,
                        "internationalContactVoice",
                        this,
                        null));
        String contactFacsimile = "contactFacsimile";
        add(
                new StringAndInternationalStringPanel(
                        contactFacsimile,
                        model,
                        contactFacsimile,
                        contactFacsimile,
                        "internationalContactFacsimile",
                        this,
                        null));

        String contactEmail = "contactEmail";
        add(
                new StringAndInternationalStringPanel(
                        contactEmail,
                        model,
                        contactEmail,
                        contactEmail,
                        "internationalContactEmail",
                        this,
                        EmailAddressValidator.getInstance()));
    }
}
