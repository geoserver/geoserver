package org.geoserver.geogit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;

/**
 * Represents the difference between two states of the same feature.
 * 
 * @author aaime
 * @author groldan
 */
public class SimpleFeatureDiff implements FeatureDiff {
    private String ID;

    private State state;

    private List<Name> changedAttributes;

    private SimpleFeature feature;

    private SimpleFeature oldFeature;

    /**
     * Creates a new feature difference for a modified feature
     * 
     * @param ID
     * @param oldFeature
     *            old version of the feature, or {@code null} if it didn't exist at initial revision
     * @param newFeature
     *            new version of the feature, or {@code null} if it doesn't exist at final revision
     */
    public SimpleFeatureDiff(SimpleFeature oldFeature, SimpleFeature newFeature) {
        if (oldFeature == null && newFeature == null) {
            throw new IllegalArgumentException("Both features are null, that's not a diff!");
        }

        this.ID = oldFeature != null ? oldFeature.getID() : newFeature.getID();
        this.feature = newFeature;
        this.oldFeature = oldFeature;
        this.changedAttributes = Collections.emptyList();
        if (oldFeature == null) {
            this.state = State.INSERTED;
        } else if (newFeature == null) {
            this.state = State.DELETED;
        } else {
            this.state = State.UPDATED;

            List<Name> changedAttributes = new ArrayList<Name>();
            Name attName;
            Object toAttribute;
            Object fromAttribute;
            for (int i = 0; i < oldFeature.getAttributeCount(); i++) {
                attName = oldFeature.getFeatureType().getDescriptor(i).getName();
                toAttribute = newFeature.getAttribute(attName);
                fromAttribute = oldFeature.getAttribute(attName);
                if (!DataUtilities.attributesEqual(fromAttribute, toAttribute)) {
                    changedAttributes.add(attName);
                }
            }
            this.changedAttributes = Collections.unmodifiableList(changedAttributes);
        }
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getChangedAttributes()
     */
    public List<Name> getChangedAttributes() {
        return changedAttributes;
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getID()
     */
    public String getID() {
        return ID;
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getState()
     */
    public State getState() {
        return state;
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getFeature()
     */
    public SimpleFeature getFeature() {
        return feature;
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getOldFeature()
     */
    public SimpleFeature getOldFeature() {
        return oldFeature;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ID == null) ? 0 : ID.hashCode());
        result = prime * result + ((changedAttributes == null) ? 0 : changedAttributes.hashCode());
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + ((oldFeature == null) ? 0 : oldFeature.hashCode());
        result = prime * result + state.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleFeatureDiff other = (SimpleFeatureDiff) obj;
        if (ID == null) {
            if (other.ID != null)
                return false;
        } else if (!ID.equals(other.ID))
            return false;
        if (changedAttributes == null) {
            if (other.changedAttributes != null)
                return false;
        } else if (!changedAttributes.equals(other.changedAttributes))
            return false;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;
        if (oldFeature == null) {
            if (other.oldFeature != null)
                return false;
        } else if (!oldFeature.equals(other.oldFeature))
            return false;
        if (state != other.state)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FeatureDiff [ID=" + ID + ", changedAttributes=" + changedAttributes + ", state="
                + state + "]";
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getNewValues()
     */
    public List<Property> getNewValues() {
        List<Property> newValues = new ArrayList<Property>();
        switch (state) {
        case INSERTED: {
            newValues.addAll(feature.getProperties());
            break;
        }
        case UPDATED: {
            final List<Name> changedAttributes = getChangedAttributes();
            for (Name name : changedAttributes) {
                newValues.add(feature.getProperty(name));
            }
            break;
        }
        case DELETED: {
            // return empty list
            break;
        }
        default:
            throw new IllegalStateException("Unknown diff state: " + state);
        }
        return newValues;
    }

    /**
     * @see org.geoserver.geogit.FeatureDiff#getOldValues()
     */
    public List<Property> getOldValues() {
        List<Property> oldValues = new ArrayList<Property>();
        switch (state) {
        case INSERTED: {
            // return empty list
            break;
        }
        case UPDATED: {
            final List<Name> changedAttributes = getChangedAttributes();
            for (Name name : changedAttributes) {
                oldValues.add(oldFeature.getProperty(name));
            }
            break;
        }
        case DELETED: {
            oldValues.addAll(oldFeature.getProperties());
            break;
        }
        default:
            throw new IllegalStateException("Unknown diff state: " + state);
        }
        return oldValues;
    }

}
