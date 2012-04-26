package org.geoserver.gss.impl;

import java.util.List;

import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.Name;

public interface FeatureDiff {

    public static enum State {
        /**
         * Feature does not exists in fromVersion, has been created in the meantime
         */
        INSERTED,

        /**
         * Feature exists in both versions, but has been modified
         */
        UPDATED,

        /**
         * Feature existed in fromVersion, but has been deleted (change map is empty)
         */
        DELETED;
    }

    public List<Name> getChangedAttributes();

    public String getID();

    public State getState();

    public Feature getFeature();

    public Feature getOldFeature();

    public List<Property> getNewValues();

    public List<Property> getOldValues();

}