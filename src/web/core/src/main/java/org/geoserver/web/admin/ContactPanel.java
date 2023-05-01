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
                        this));
        String contactOrg = "contactOrganization";
        add(
                new StringAndInternationalStringPanel(
                        contactOrg,
                        model,
                        contactOrg,
                        contactOrg,
                        "internationalContactOrganization",
                        this));
        String contactPosition = "contactPosition";
        add(
                new StringAndInternationalStringPanel(
                        contactPosition,
                        model,
                        contactPosition,
                        contactPosition,
                        "internationalContactPosition",
                        this));
        String addressType = "addressType";
        add(
                new StringAndInternationalStringPanel(
                        addressType,
                        model,
                        addressType,
                        addressType,
                        "internationalAddressType",
                        this));
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
                        this));
        String addressCity = "addressCity";
        add(
                new StringAndInternationalStringPanel(
                        addressCity,
                        model,
                        addressCity,
                        addressCity,
                        "internationalAddressCity",
                        this));
        String addressState = "addressState";
        add(
                new StringAndInternationalStringPanel(
                        addressState,
                        model,
                        addressState,
                        addressState,
                        "internationalAddressState",
                        this));
        String addressPostalCode = "addressPostalCode";
        add(
                new StringAndInternationalStringPanel(
                        addressPostalCode,
                        model,
                        addressPostalCode,
                        addressPostalCode,
                        "internationalAddressPostalCode",
                        this));
        String addressCountry = "addressCountry";
        add(
                new StringAndInternationalStringPanel(
                        addressCountry,
                        model,
                        addressCountry,
                        addressCountry,
                        "internationalAddressCountry",
                        this));
        String contactVoice = "contactVoice";
        add(
                new StringAndInternationalStringPanel(
                        contactVoice,
                        model,
                        contactVoice,
                        contactVoice,
                        "internationalContactVoice",
                        this));
        String contactFacsimile = "contactFacsimile";
        add(
                new StringAndInternationalStringPanel(
                        contactFacsimile,
                        model,
                        contactFacsimile,
                        contactFacsimile,
                        "internationalContactFacsimile",
                        this));

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
