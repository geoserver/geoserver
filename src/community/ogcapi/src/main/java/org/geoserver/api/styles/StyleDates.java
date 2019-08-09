/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class StyleDates implements Serializable {

    Date creation;
    boolean creationSet;
    Date publication;
    boolean publicationSet;
    Date revision;
    boolean revisionSet;
    Date validTill;
    boolean validTillSet;
    Date receivedOn;
    boolean receivedOnSet;

    public Date getCreation() {
        return creation;
    }

    public void setCreation(Date creation) {
        this.creationSet = true;
        this.creation = creation;
    }

    public Date getPublication() {
        return publication;
    }

    public void setPublication(Date publication) {
        this.publicationSet = true;
        this.publication = publication;
    }

    public Date getRevision() {
        return revision;
    }

    public void setRevision(Date revision) {
        this.revisionSet = true;
        this.revision = revision;
    }

    public Date getValidTill() {
        return validTill;
    }

    public void setValidTill(Date validTill) {
        this.validTillSet = true;
        this.validTill = validTill;
    }

    public Date getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(Date receivedOn) {
        this.receivedOnSet = true;
        this.receivedOn = receivedOn;
    }

    @JsonIgnore
    public boolean isCreationSet() {
        return creationSet;
    }

    @JsonIgnore
    public boolean isPublicationSet() {
        return publicationSet;
    }

    @JsonIgnore
    public boolean isRevisionSet() {
        return revisionSet;
    }

    @JsonIgnore
    public boolean isValidTillSet() {
        return validTillSet;
    }

    @JsonIgnore
    public boolean isReceivedOnSet() {
        return receivedOnSet;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
