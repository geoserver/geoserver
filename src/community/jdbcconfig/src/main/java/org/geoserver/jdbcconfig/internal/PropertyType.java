/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.jdbcconfig.internal;

import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/** @author groldan */
public class PropertyType implements Comparable<PropertyType> {

    private final Integer oid;

    private final Integer targetPropertyOid;

    private final Integer objectTypeOid;

    private final String propertyName;

    private final boolean collectionProperty;

    private final boolean text;

    /**
     * @param oid the pk of this property type
     * @param targetPropertyOid the pk of the related property type, or {@code null} if this
     *     property type is a "self" property (i.e. does not relate to a property of another type)
     * @param objectTypeOid the type of object this property belongs to
     * @param propertyName the name of the property (e.g. {@code name}, {@code
     *     resource.store.workspace.id}, etc)
     * @param collectionProperty {@code true} if this is a multi-valued property
     * @param isText whether this property is subject for a full text search
     */
    public PropertyType(
            final Integer oid,
            @Nullable final Integer targetPropertyOid,
            final Integer objectTypeOid,
            final String propertyName,
            boolean collectionProperty,
            final boolean isText) {
        if (targetPropertyOid != null && targetPropertyOid == 0) {
            throw new IllegalArgumentException("oid cannot be zero");
        }
        this.oid = oid;
        this.targetPropertyOid = targetPropertyOid;
        this.objectTypeOid = objectTypeOid;
        this.propertyName = propertyName;
        this.collectionProperty = collectionProperty;
        this.text = isText;
    }

    public Integer getOid() {
        return oid;
    }

    public Integer getTargetPropertyOid() {
        return targetPropertyOid;
    }

    public Integer getObjectTypeOid() {
        return objectTypeOid;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public boolean isCollectionProperty() {
        return collectionProperty;
    }

    public boolean isText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int compareTo(PropertyType o) {
        int comparison = oid.compareTo(o.oid);
        if (comparison == 0) {
            comparison =
                    targetPropertyOid == null && o.targetPropertyOid != null
                            ? -1
                            : (o.targetPropertyOid == null
                                    ? 1
                                    : targetPropertyOid.compareTo(o.targetPropertyOid));
            if (comparison == 0) {
                comparison = objectTypeOid.compareTo(o.objectTypeOid);
                if (comparison == 0) {
                    comparison = propertyName.compareTo(o.propertyName);
                    if (comparison == 0) {
                        comparison =
                                (collectionProperty == o.collectionProperty
                                        ? 0
                                        : (collectionProperty ? 1 : -1));
                    }
                }
            }
        }
        return comparison;
    }
}
