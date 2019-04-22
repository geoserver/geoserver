/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.config.ContactInfo;

public class ContactInfoImpl implements ContactInfo {

    String id = "contact";

    String address;

    String addressCity;

    String addressCountry;

    String addressDeliveryPoint;

    String addressPostalCode;

    String addressState;

    String addressType;

    String contactEmail;

    String contactFacsimile;

    String contactOrganization;

    String contactPerson;

    String contactPosition;

    String contactVoice;

    String onlineResource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    @Override
    public String getAddressDeliveryPoint() {
        return addressDeliveryPoint;
    }

    @Override
    public void setAddressDeliveryPoint(String addressDeliveryPoint) {
        this.addressDeliveryPoint = addressDeliveryPoint;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactFacsimile() {
        return contactFacsimile;
    }

    public void setContactFacsimile(String contactFacsimile) {
        this.contactFacsimile = contactFacsimile;
    }

    public String getContactOrganization() {
        return contactOrganization;
    }

    public void setContactOrganization(String contactOrganization) {
        this.contactOrganization = contactOrganization;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPosition() {
        return contactPosition;
    }

    public void setContactPosition(String contactPosition) {
        this.contactPosition = contactPosition;
    }

    public String getContactVoice() {
        return contactVoice;
    }

    public void setContactVoice(String contactVoice) {
        this.contactVoice = contactVoice;
    }

    public String getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
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
        return result;
    }

    @Override
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
        return true;
    }
}
