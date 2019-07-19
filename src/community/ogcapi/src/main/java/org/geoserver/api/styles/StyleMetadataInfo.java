/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.io.Serializable;
import java.util.List;

/**
 * These may eventually be added to StyleInfo itself, but for the moment we don't know if the OGC
 * Styles service will become a formal spec, and what final shape it will take, so setting them up
 * in the metadata map for the time being
 */
public class StyleMetadataInfo implements Serializable {

    public static String METADATA_KEY = "OgcStylesMetadataInfo";

    String title;
    String abstrct;
    List<String> keywords;
    String pointOfContact;
    String accessConstraints;
    StyleDates dates;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstract() {
        return abstrct;
    }

    public void setAbstract(String abstrct) {
        this.abstrct = abstrct;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getPointOfContact() {
        return pointOfContact;
    }

    public void setPointOfContact(String pointOfContact) {
        this.pointOfContact = pointOfContact;
    }

    public String getAccessConstraints() {
        return accessConstraints;
    }

    public void setAccessConstraints(String accessConstraints) {
        this.accessConstraints = accessConstraints;
    }

    public StyleDates getDates() {
        return dates;
    }

    public void setDates(StyleDates dates) {
        this.dates = dates;
    }
}
