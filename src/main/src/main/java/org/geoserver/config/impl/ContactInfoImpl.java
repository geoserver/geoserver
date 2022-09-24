/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.config.ContactInfo;
import org.geoserver.util.InternationalStringUtils;
import org.opengis.util.InternationalString;

public class ContactInfoImpl implements ContactInfo {

    private String id = "contact";

    private String address;

    private String addressCity;

    private String addressCountry;

    private String addressDeliveryPoint;

    private String addressPostalCode;

    private String addressState;

    private String addressType;

    private String contactEmail;

    private String contactFacsimile;

    private String contactOrganization;

    private String contactPerson;

    private String contactPosition;

    private String contactVoice;

    private String onlineResource;

    private String welcome;

    private InternationalString internationalAddress;

    private InternationalString internationalAddressCity;

    private InternationalString internationalAddressCountry;

    private InternationalString internationalAddressDeliveryPoint;

    private InternationalString internationalAddressPostalCode;

    private InternationalString internationalAddressState;

    private InternationalString internationalAddressType;

    private InternationalString internationalContactEmail;

    private InternationalString internationalContactFacsimile;

    private InternationalString internationalContactOrganization;

    private InternationalString internationalContactPerson;

    private InternationalString internationalContactPosition;

    private InternationalString internationalContactVoice;

    private InternationalString internationalOnlineResource;

    private InternationalString internationalWelcome;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getAddress() {
        return InternationalStringUtils.getOrDefault(address, internationalAddress);
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getAddressCity() {
        return InternationalStringUtils.getOrDefault(addressCity, internationalAddressCity);
    }

    @Override
    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    @Override
    public String getAddressCountry() {
        return InternationalStringUtils.getOrDefault(addressCountry, internationalAddressCountry);
    }

    @Override
    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    @Override
    public String getAddressDeliveryPoint() {
        return InternationalStringUtils.getOrDefault(
                addressDeliveryPoint, internationalAddressDeliveryPoint);
    }

    @Override
    public void setAddressDeliveryPoint(String addressDeliveryPoint) {
        this.addressDeliveryPoint = addressDeliveryPoint;
    }

    @Override
    public String getAddressPostalCode() {
        return InternationalStringUtils.getOrDefault(
                addressPostalCode, internationalAddressPostalCode);
    }

    @Override
    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    @Override
    public String getAddressState() {
        return InternationalStringUtils.getOrDefault(addressState, internationalAddressState);
    }

    @Override
    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    @Override
    public String getAddressType() {
        return InternationalStringUtils.getOrDefault(addressType, internationalAddressType);
    }

    @Override
    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    @Override
    public String getContactEmail() {
        return InternationalStringUtils.getOrDefault(contactEmail, internationalContactEmail);
    }

    @Override
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Override
    public String getContactFacsimile() {
        return InternationalStringUtils.getOrDefault(
                contactFacsimile, internationalContactFacsimile);
    }

    @Override
    public void setContactFacsimile(String contactFacsimile) {
        this.contactFacsimile = contactFacsimile;
    }

    @Override
    public String getContactOrganization() {
        return InternationalStringUtils.getOrDefault(
                contactOrganization, internationalContactOrganization);
    }

    @Override
    public void setContactOrganization(String contactOrganization) {
        this.contactOrganization = contactOrganization;
    }

    @Override
    public String getContactPerson() {
        return InternationalStringUtils.getOrDefault(contactPerson, internationalContactPerson);
    }

    @Override
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    @Override
    public String getContactPosition() {
        return InternationalStringUtils.getOrDefault(contactPosition, internationalContactPosition);
    }

    @Override
    public void setContactPosition(String contactPosition) {
        this.contactPosition = contactPosition;
    }

    @Override
    public String getContactVoice() {
        return InternationalStringUtils.getOrDefault(contactVoice, internationalContactVoice);
    }

    @Override
    public void setContactVoice(String contactVoice) {
        this.contactVoice = contactVoice;
    }

    @Override
    public String getOnlineResource() {
        return InternationalStringUtils.getOrDefault(onlineResource, internationalOnlineResource);
    }

    @Override
    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
    }

    @Override
    public String getWelcome() {
        return InternationalStringUtils.getOrDefault(welcome, internationalWelcome);
    }

    @Override
    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    @Override
    public InternationalString getInternationalAddress() {
        return internationalAddress;
    }

    @Override
    public void setInternationalAddress(InternationalString internationalAddress) {
        this.internationalAddress = InternationalStringUtils.growable(internationalAddress);
    }

    @Override
    public InternationalString getInternationalContactFacsimile() {
        return internationalContactFacsimile;
    }

    @Override
    public void setInternationalContactFacsimile(
            InternationalString internationalContactFacsimile) {
        this.internationalContactFacsimile =
                InternationalStringUtils.growable(internationalContactFacsimile);
    }

    @Override
    public InternationalString getInternationalContactOrganization() {
        return internationalContactOrganization;
    }

    @Override
    public void setInternationalContactOrganization(
            InternationalString internationalContactOrganization) {
        this.internationalContactOrganization =
                InternationalStringUtils.growable(internationalContactOrganization);
    }

    @Override
    public InternationalString getInternationalContactPerson() {
        return internationalContactPerson;
    }

    @Override
    public void setInternationalContactPerson(InternationalString internationalContactPerson) {
        this.internationalContactPerson =
                InternationalStringUtils.growable(internationalContactPerson);
    }

    @Override
    public InternationalString getInternationalContactPosition() {
        return internationalContactPosition;
    }

    @Override
    public void setInternationalContactPosition(InternationalString internationalContactPosition) {
        this.internationalContactPosition =
                InternationalStringUtils.growable(internationalContactPosition);
    }

    @Override
    public InternationalString getInternationalContactVoice() {
        return internationalContactVoice;
    }

    @Override
    public void setInternationalContactVoice(InternationalString internationalContactVoice) {
        this.internationalContactVoice =
                InternationalStringUtils.growable(internationalContactVoice);
    }

    @Override
    public InternationalString getInternationalOnlineResource() {
        return internationalOnlineResource;
    }

    @Override
    public void setInternationalOnlineResource(InternationalString internationalOnlineResource) {
        this.internationalOnlineResource =
                InternationalStringUtils.growable(internationalOnlineResource);
    }

    @Override
    public InternationalString getInternationalWelcome() {
        return internationalWelcome;
    }

    @Override
    public void setInternationalWelcome(InternationalString internationalWelcome) {
        this.internationalWelcome = InternationalStringUtils.growable(internationalWelcome);
    }

    @Override
    public InternationalString getInternationalAddressCity() {
        return internationalAddressCity;
    }

    @Override
    public void setInternationalAddressCity(InternationalString internationalAddressCity) {
        this.internationalAddressCity = InternationalStringUtils.growable(internationalAddressCity);
    }

    @Override
    public InternationalString getInternationalAddressCountry() {
        return internationalAddressCountry;
    }

    @Override
    public void setInternationalAddressCountry(InternationalString internationalAddressCountry) {
        this.internationalAddressCountry =
                InternationalStringUtils.growable(internationalAddressCountry);
    }

    @Override
    public InternationalString getInternationalAddressDeliveryPoint() {
        return internationalAddressDeliveryPoint;
    }

    @Override
    public void setInternationalAddressDeliveryPoint(
            InternationalString internationalAddressDeliveryPoint) {
        this.internationalAddressDeliveryPoint =
                InternationalStringUtils.growable(internationalAddressDeliveryPoint);
    }

    @Override
    public InternationalString getInternationalAddressPostalCode() {
        return internationalAddressPostalCode;
    }

    @Override
    public void setInternationalAddressPostalCode(
            InternationalString internationalAddressPostalCode) {
        this.internationalAddressPostalCode =
                InternationalStringUtils.growable(internationalAddressPostalCode);
    }

    @Override
    public InternationalString getInternationalAddressState() {
        return internationalAddressState;
    }

    @Override
    public void setInternationalAddressState(InternationalString internationalAddressState) {
        this.internationalAddressState =
                InternationalStringUtils.growable(internationalAddressState);
    }

    @Override
    public InternationalString getInternationalAddressType() {
        return internationalAddressType;
    }

    @Override
    public void setInternationalAddressType(InternationalString internationalAddressType) {
        this.internationalAddressType = InternationalStringUtils.growable(internationalAddressType);
    }

    @Override
    public InternationalString getInternationalContactEmail() {
        return internationalContactEmail;
    }

    @Override
    public void setInternationalContactEmail(InternationalString internationalContactEmail) {
        this.internationalContactEmail =
                InternationalStringUtils.growable(internationalContactEmail);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((address == null) ? 0 : address.hashCode());
        result = PRIME * result + ((addressCity == null) ? 0 : addressCity.hashCode());
        result = PRIME * result + ((addressCountry == null) ? 0 : addressCountry.hashCode());
        result = PRIME * result + ((addressPostalCode == null) ? 0 : addressPostalCode.hashCode());
        result = PRIME * result + ((addressState == null) ? 0 : addressState.hashCode());
        result = PRIME * result + ((addressType == null) ? 0 : addressType.hashCode());
        result = PRIME * result + ((contactEmail == null) ? 0 : contactEmail.hashCode());
        result = PRIME * result + ((contactFacsimile == null) ? 0 : contactFacsimile.hashCode());
        result =
                PRIME * result
                        + ((contactOrganization == null) ? 0 : contactOrganization.hashCode());
        result = PRIME * result + ((contactPerson == null) ? 0 : contactPerson.hashCode());
        result = PRIME * result + ((contactPosition == null) ? 0 : contactPosition.hashCode());
        result = PRIME * result + ((contactVoice == null) ? 0 : contactVoice.hashCode());
        result = PRIME * result + ((onlineResource == null) ? 0 : onlineResource.hashCode());
        result =
                PRIME * result
                        + ((addressDeliveryPoint == null) ? 0 : addressDeliveryPoint.hashCode());

        result =
                PRIME * result
                        + ((internationalAddress == null) ? 0 : internationalAddress.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressCity == null)
                                ? 0
                                : internationalAddressCity.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressCountry == null)
                                ? 0
                                : internationalAddressCountry.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressPostalCode == null)
                                ? 0
                                : internationalAddressPostalCode.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressState == null)
                                ? 0
                                : internationalAddressState.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressType == null)
                                ? 0
                                : internationalAddressType.hashCode());
        result =
                PRIME * result
                        + ((internationalContactEmail == null)
                                ? 0
                                : internationalContactEmail.hashCode());
        result =
                PRIME * result
                        + ((internationalContactFacsimile == null)
                                ? 0
                                : internationalContactFacsimile.hashCode());
        result =
                PRIME * result
                        + ((internationalContactOrganization == null)
                                ? 0
                                : internationalContactOrganization.hashCode());
        result =
                PRIME * result
                        + ((internationalContactPerson == null)
                                ? 0
                                : internationalContactPerson.hashCode());
        result =
                PRIME * result
                        + ((internationalContactPosition == null)
                                ? 0
                                : internationalContactPosition.hashCode());
        result =
                PRIME * result
                        + ((internationalContactVoice == null)
                                ? 0
                                : internationalContactVoice.hashCode());
        result =
                PRIME * result
                        + ((internationalOnlineResource == null)
                                ? 0
                                : internationalOnlineResource.hashCode());
        result =
                PRIME * result
                        + ((internationalAddressDeliveryPoint == null)
                                ? 0
                                : internationalAddressDeliveryPoint.hashCode());

        result =
                PRIME * result
                        + ((internationalWelcome == null) ? 0 : internationalWelcome.hashCode());

        return result;
    }

    @Override
    @SuppressWarnings("PMD.CognitiveComplexity") // IDE generated code, ignoring
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ContactInfo)) return false;
        final ContactInfo other = (ContactInfo) obj;
        if (address == null) {
            if (other.getAddress() != null) return false;
        } else if (!address.equals(other.getAddress())) return false;
        if (addressCity == null) {
            if (other.getAddressCity() != null) return false;
        } else if (!addressCity.equals(other.getAddressCity())) return false;
        if (addressCountry == null) {
            if (other.getAddressCountry() != null) return false;
        } else if (!addressCountry.equals(other.getAddressCountry())) return false;
        if (addressPostalCode == null) {
            if (other.getAddressPostalCode() != null) return false;
        } else if (!addressPostalCode.equals(other.getAddressPostalCode())) return false;
        if (addressState == null) {
            if (other.getAddressState() != null) return false;
        } else if (!addressState.equals(other.getAddressState())) return false;
        if (addressType == null) {
            if (other.getAddressType() != null) return false;
        } else if (!addressType.equals(other.getAddressType())) return false;
        if (contactEmail == null) {
            if (other.getContactEmail() != null) return false;
        } else if (!contactEmail.equals(other.getContactEmail())) return false;
        if (contactFacsimile == null) {
            if (other.getContactFacsimile() != null) return false;
        } else if (!contactFacsimile.equals(other.getContactFacsimile())) return false;
        if (contactOrganization == null) {
            if (other.getContactOrganization() != null) return false;
        } else if (!contactOrganization.equals(other.getContactOrganization())) return false;
        if (contactPerson == null) {
            if (other.getContactPerson() != null) return false;
        } else if (!contactPerson.equals(other.getContactPerson())) return false;
        if (contactPosition == null) {
            if (other.getContactPosition() != null) return false;
        } else if (!contactPosition.equals(other.getContactPosition())) return false;
        if (contactVoice == null) {
            if (other.getContactVoice() != null) return false;
        } else if (!contactVoice.equals(other.getContactVoice())) return false;
        if (onlineResource == null) {
            if (other.getOnlineResource() != null) return false;
        } else if (!onlineResource.equals(other.getOnlineResource())) return false;
        if (addressDeliveryPoint == null) {
            if (other.getAddressDeliveryPoint() != null) return false;
        } else if (!addressDeliveryPoint.equals(other.getAddressDeliveryPoint())) return false;

        if (internationalAddress == null) {
            if (other.getInternationalAddress() != null) return false;
        } else if (!internationalAddress.equals(other.getInternationalAddress())) return false;
        if (internationalAddressCity == null) {
            if (other.getInternationalAddressCity() != null) return false;
        } else if (!internationalAddressCity.equals(other.getInternationalAddressCity()))
            return false;
        if (internationalAddressCountry == null) {
            if (other.getInternationalAddressCountry() != null) return false;
        } else if (!internationalAddressCountry.equals(other.getInternationalAddressCountry()))
            return false;
        if (internationalAddressPostalCode == null) {
            if (other.getInternationalAddressPostalCode() != null) return false;
        } else if (!internationalAddressPostalCode.equals(
                other.getInternationalAddressPostalCode())) return false;
        if (internationalAddressState == null) {
            if (other.getInternationalAddressState() != null) return false;
        } else if (!internationalAddressState.equals(other.getInternationalAddressState()))
            return false;
        if (internationalAddressType == null) {
            if (other.getInternationalAddressType() != null) return false;
        } else if (!internationalAddressType.equals(other.getInternationalAddressType()))
            return false;
        if (internationalContactEmail == null) {
            if (other.getInternationalContactEmail() != null) return false;
        } else if (!internationalContactEmail.equals(other.getInternationalContactEmail()))
            return false;
        if (internationalContactFacsimile == null) {
            if (other.getInternationalContactFacsimile() != null) return false;
        } else if (!internationalContactFacsimile.equals(other.getInternationalContactFacsimile()))
            return false;
        if (internationalContactOrganization == null) {
            if (other.getInternationalContactOrganization() != null) return false;
        } else if (!internationalContactOrganization.equals(
                other.getInternationalContactOrganization())) return false;
        if (internationalContactPerson == null) {
            if (other.getInternationalContactPerson() != null) return false;
        } else if (!internationalContactPerson.equals(other.getInternationalContactPerson()))
            return false;
        if (internationalContactPosition == null) {
            if (other.getInternationalContactPosition() != null) return false;
        } else if (!internationalContactPosition.equals(other.getInternationalContactPosition()))
            return false;
        if (internationalContactVoice == null) {
            if (other.getInternationalContactVoice() != null) return false;
        } else if (!internationalContactVoice.equals(other.getInternationalContactVoice()))
            return false;
        if (internationalOnlineResource == null) {
            if (other.getInternationalOnlineResource() != null) return false;
        } else if (!internationalOnlineResource.equals(other.getInternationalOnlineResource()))
            return false;
        if (internationalAddressDeliveryPoint == null) {
            if (other.getInternationalAddressDeliveryPoint() != null) return false;
        } else if (!internationalAddressDeliveryPoint.equals(
                other.getInternationalAddressDeliveryPoint())) return false;
        return true;
    }
}
