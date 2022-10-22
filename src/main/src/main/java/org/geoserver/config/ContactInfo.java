/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Info;
import org.opengis.util.InternationalString;

/**
 * GeoServer contact information.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ContactInfo extends Info {

    /** Identifier. */
    @Override
    String getId();

    /** @uml.property name="address" */
    String getAddress();

    /** @uml.property name="address" */
    void setAddress(String address);

    /** @uml.property name="addressCity" */
    String getAddressCity();

    /** @uml.property name="addressCity" */
    void setAddressCity(String addressCity);

    /** @uml.property name="addressCountry" */
    String getAddressCountry();

    /** @uml.property name="addressCountry" */
    void setAddressCountry(String addressCountry);

    /**
     * Synonym with {@code address}, see {@link #getAddress()}.
     *
     * @uml.property name="addressDeliveryPoint"
     */
    String getAddressDeliveryPoint();

    /**
     * Synonym with {@code address}, see {@link #setAddress(String)}}.
     *
     * @uml.property name="addressDeliveryPoint"
     */
    void setAddressDeliveryPoint(String addressDeliveryPoint);

    /** @uml.property name="addressPostalCode" */
    String getAddressPostalCode();

    /** @uml.property name="addressPostalCode" */
    void setAddressPostalCode(String addressPostalCode);

    /** @uml.property name="addressState" */
    String getAddressState();

    /** @uml.property name="addressState" */
    void setAddressState(String addressState);

    /** @uml.property name="addressType" */
    String getAddressType();

    /** @uml.property name="addressType" */
    void setAddressType(String addressType);

    /** @uml.property name="contactEmail" */
    String getContactEmail();

    /** @uml.property name="contactEmail" */
    void setContactEmail(String contactEmail);

    /** @uml.property name="contactFacsimile" */
    String getContactFacsimile();

    /** @uml.property name="contactFacsimile" */
    void setContactFacsimile(String contactFacsimile);

    /** @uml.property name="contactOrganization" */
    String getContactOrganization();

    /** @uml.property name="contactOrganization" */
    void setContactOrganization(String contactOrganization);

    /** @uml.property name="contactPerson" */
    String getContactPerson();

    /** @uml.property name="contactPerson" */
    void setContactPerson(String contactPerson);

    /** @uml.property name="contactPosition" */
    String getContactPosition();

    /** @uml.property name="contactPosition" */
    void setContactPosition(String contactPosition);

    /** @uml.property name="contactVoice" */
    String getContactVoice();

    /** @uml.property name="contactVoice" */
    void setContactVoice(String contactVoice);

    String getOnlineResource();

    void setOnlineResource(String onlineResource);

    /**
     * Introduction message.
     *
     * @return introduction message
     */
    String getWelcome();

    /**
     * Define introduction message
     *
     * @param welcome Introduction message
     */
    void setWelcome(String welcome);

    // i18n fields
    /** @uml.property name="internationalAddress" */
    InternationalString getInternationalAddress();

    /** @uml.property name="internationalAddress" */
    void setInternationalAddress(InternationalString address);

    /** @uml.property name="internationalAddressCity" */
    InternationalString getInternationalAddressCity();

    /** @uml.property name="internationalAddressCity" */
    void setInternationalAddressCity(InternationalString addressCity);

    /** @uml.property name="internationalAddressCountry" */
    InternationalString getInternationalAddressCountry();

    /** @uml.property name="internationalAddressCountry" */
    void setInternationalAddressCountry(InternationalString addressCountry);

    /** @uml.property name="internationalAddressDeliveryPoint" */
    InternationalString getInternationalAddressDeliveryPoint();

    /** @uml.property name="internationalAddressDeliveryPoint" */
    void setInternationalAddressDeliveryPoint(InternationalString addressDeliveryPoint);

    /** @uml.property name="internationalAddressPostalCode" */
    InternationalString getInternationalAddressPostalCode();

    /** @uml.property name="internationalAddressPostalCode" */
    void setInternationalAddressPostalCode(InternationalString addressPostalCode);

    /** @uml.property name="internationalAddressState" */
    InternationalString getInternationalAddressState();

    /** @uml.property name="internationalAddressState" */
    void setInternationalAddressState(InternationalString addressState);

    /** @uml.property name="internationalAddressType" */
    InternationalString getInternationalAddressType();

    /** @uml.property name="internationalAddressType" */
    void setInternationalAddressType(InternationalString addressType);

    /** @uml.property name="internationalContactEmail" */
    InternationalString getInternationalContactEmail();

    /** @uml.property name="internationalContactEmail" */
    void setInternationalContactEmail(InternationalString contactEmail);

    /** @uml.property name="internationalContactFacsimile" */
    InternationalString getInternationalContactFacsimile();

    /** @uml.property name="internationalContactFacsimile" */
    void setInternationalContactFacsimile(InternationalString contactFacsimile);

    /** @uml.property name="internationalContactOrganization" */
    InternationalString getInternationalContactOrganization();

    /** @uml.property name="internationalContactOrganization" */
    void setInternationalContactOrganization(InternationalString contactOrganization);

    /** @uml.property name="internationalContactPerson" */
    InternationalString getInternationalContactPerson();

    /** @uml.property name="internationalContactPerson" */
    void setInternationalContactPerson(InternationalString contactPerson);

    /** @uml.property name="internationalContactPosition" */
    InternationalString getInternationalContactPosition();

    /** @uml.property name="internationalContactPosition" */
    void setInternationalContactPosition(InternationalString contactPosition);

    /** @uml.property name="internationalContactVoice" */
    InternationalString getInternationalContactVoice();

    /** @uml.property name="internationalContactVoice" */
    void setInternationalContactVoice(InternationalString contactVoice);

    InternationalString getInternationalOnlineResource();

    void setInternationalOnlineResource(InternationalString onlineResource);

    InternationalString getInternationalWelcome();

    void setInternationalWelcome(InternationalString onlineResource);
}
