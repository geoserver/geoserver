/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Property {

    private final PropertyType propertyType;

    public final Object value;

    public Property(PropertyType propertyType, Object value) {
        this.propertyType = propertyType;
        this.value = value;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public Object getValue() {
        return value;
    }

    public boolean isRelationship() {
        return propertyType.getTargetPropertyOid() != null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /** */
    public String getPropertyName() {
        return propertyType.getPropertyName();
    }

    public boolean isCollectionProperty() {
        return propertyType.isCollectionProperty();
    }
}
