/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Info;
import org.geotools.api.util.InternationalString;

/**
 * GeoServer contact information associated with gloabal services or workspace.
 *
 * <p>This information is used on both welcome page, and in service description (e.g. WMS GetCapabilities).
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ContactInfo extends Info {

    /** Identifier. */
    @Override
    String getId();

    /**
     * Contact address field.
     *
     * @return Contact address.
     * @uml.property name="address"
     */
    String getAddress();

    /**
     * Contact address field.
     *
     * @param address Contact address.
     * @uml.property name="address"
     */
    void setAddress(String address);

    /**
     * Contact address city field.
     *
     * @return Contact address city.
     * @uml.property name="addressCity"
     */
    String getAddressCity();

    /**
     * Contract address city field.
     *
     * @param addressCity Contact address city.
     * @uml.property name="addressCity"
     */
    void setAddressCity(String addressCity);

    /**
     * Contract address country field.
     *
     * @return Contact address country.
     * @uml.property name="addressCountry"
     */
    String getAddressCountry();

    /**
     * Contract address country field.
     *
     * @param addressCountry Contact address country.
     * @uml.property name="addressCountry"
     */
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

    /**
     * Contact address postal code.
     *
     * @return postal code
     * @uml.property name="addressPostalCode"
     */
    String getAddressPostalCode();

    /**
     * Contact address postal code.
     *
     * @param addressPostalCode Postal code
     * @uml.property name="addressPostalCode"
     */
    void setAddressPostalCode(String addressPostalCode);

    /**
     * Contract address state field.
     *
     * @return Contact address state.
     * @uml.property name="addressState"
     */
    String getAddressState();

    /**
     * Contract address state field.
     *
     * @param addressState Contact address state.
     * @uml.property name="addressState"
     */
    void setAddressState(String addressState);

    /**
     * Contract address type field.
     *
     * @return Contact address type.
     * @uml.property name="addressType"
     */
    String getAddressType();

    /**
     * Contract address type field.
     *
     * @param addressType Contact address type.
     * @uml.property name="addressType"
     */
    void setAddressType(String addressType);

    /**
     * Contract email address.
     *
     * @return Contact email address.
     * @uml.property name="contactEmail"
     */
    String getContactEmail();

    /**
     * Contact email address.
     *
     * @param contactEmail Contact email address.
     * @uml.property name="contactEmail"
     */
    void setContactEmail(String contactEmail);

    /**
     * Contact fax number.
     *
     * @return Contaxt fax number.
     * @uml.property name="contactFacsimile"
     */
    String getContactFacsimile();

    /**
     * Contact fax number.
     *
     * @param contactFacsimile Contact fax number.
     * @uml.property name="contactFacsimile"
     */
    void setContactFacsimile(String contactFacsimile);

    /**
     * Contact organization.
     *
     * @return Contact organization.
     * @uml.property name="contactOrganization"
     */
    String getContactOrganization();

    /**
     * Contact organziation.
     *
     * @param contactOrganization Contact organization.
     * @uml.property name="contactOrganization"
     */
    void setContactOrganization(String contactOrganization);

    /**
     * Contact person or role for web service.
     *
     * @return Contact person or role.
     * @uml.property name="contactPerson"
     */
    String getContactPerson();

    /**
     * Contact person or role for web service.
     *
     * @param contactPerson Contact person or role
     * @uml.property name="contactPerson"
     */
    void setContactPerson(String contactPerson);

    /**
     * Providers job position or role.
     *
     * @return Providers job position or role.
     * @uml.property name="contactPosition"
     */
    String getContactPosition();

    /**
     * Providers job position or role.
     *
     * @param contactPosition Providers job position or role.
     * @uml.property name="contactPosition"
     */
    void setContactPosition(String contactPosition);

    /**
     * Providers voice phone number.
     *
     * @return Providers voice phone number.
     * @uml.property name="contactVoice"
     */
    String getContactVoice();

    /**
     * Providers voice phone number.
     *
     * @param contactVoice Providers voice phone number.
     * @uml.property name="contactVoice"
     */
    void setContactVoice(String contactVoice);

    /**
     * Link to providers website.
     *
     * @return Link to the providers website, or {#@code null}
     */
    String getOnlineResource();

    /**
     * Link to providers website.
     *
     * @param onlineResource Link to providers website, or {@code null}
     */
    void setOnlineResource(String onlineResource);

    /**
     * Title used globally, or for individual workspace.
     *
     * @return global or workspace title.
     */
    String getTitle();

    /**
     * Title used global, or for individual workspace.
     *
     * @param title gobal or workspace title
     */
    void setTitle(String title);

    /**
     * Title used globally, or for individual workspace.
     *
     * <p>Human readable title used to identify the instance of GeoServer, or virutal workspace to the public.
     *
     * @return Global or workspace title
     */
    InternationalString getInternationalTitle();

    /**
     * Title used globally, or for individual workspace.
     *
     * <p>Human readable title used to identify the instance of GeoServer, or virutal workspace to the public.
     *
     * @param title Global or workspace title
     */
    void setInternationalTitle(InternationalString title);

    /**
     * Global or worksapce introduction message, in the default language.
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

    /**
     * Global or workspace introduction message.
     *
     * @return introduction message
     */
    InternationalString getInternationalWelcome();

    /**
     * Global or worksapce introduction message.
     *
     * @param welcome introduction message
     */
    void setInternationalWelcome(InternationalString welcome);

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
}
