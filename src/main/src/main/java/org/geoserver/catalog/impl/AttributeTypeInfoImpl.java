/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.opengis.feature.type.AttributeDescriptor;

public class AttributeTypeInfoImpl implements AttributeTypeInfo {

    protected String id;
    protected String name;
    protected int minOccurs;
    protected int maxOccurs;
    protected boolean nillable;
    protected transient AttributeDescriptor attribute;
    protected MetadataMap metadata = new MetadataMap();
    protected FeatureTypeInfo featureType;
    protected Class binding;
    protected Integer length;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public FeatureTypeInfo getFeatureType() {
        return featureType;
    }

    public void setFeatureType(FeatureTypeInfo featureType) {
        this.featureType = featureType;
    }

    public AttributeDescriptor getAttribute() {
        return attribute;
    }

    public void setAttribute(AttributeDescriptor attribute) {
        this.attribute = attribute;
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return name;
    }

    public Class getBinding() {
        return binding;
    }

    public void setBinding(Class binding) {
        this.binding = binding;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((binding == null) ? 0 : binding.hashCode());
        result = prime * result + ((featureType == null) ? 0 : featureType.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((length == null) ? 0 : length.hashCode());
        result = prime * result + maxOccurs;
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + minOccurs;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AttributeTypeInfoImpl other = (AttributeTypeInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (binding == null) {
            if (other.binding != null) return false;
        } else if (!binding.equals(other.binding)) return false;
        if (featureType == null) {
            if (other.featureType != null) return false;
        } else if (!featureType.equals(other.featureType)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (length == null) {
            if (other.length != null) return false;
        } else if (!length.equals(other.length)) return false;
        if (maxOccurs != other.maxOccurs) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (minOccurs != other.minOccurs) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (nillable != other.nillable) return false;
        return true;
    }

    @Override
    public boolean equalsIngnoreFeatureType(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AttributeTypeInfoImpl other = (AttributeTypeInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (binding == null) {
            if (other.binding != null) return false;
        } else if (!binding.equals(other.binding)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (length == null) {
            if (other.length != null) return false;
        } else if (!length.equals(other.length)) return false;
        if (maxOccurs != other.maxOccurs) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (minOccurs != other.minOccurs) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (nillable != other.nillable) return false;
        return true;
    }
}
