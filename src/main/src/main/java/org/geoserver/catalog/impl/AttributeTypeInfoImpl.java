/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.Objects;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.util.InternationalString;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.GrowableInternationalString;

public class AttributeTypeInfoImpl implements AttributeTypeInfo {

    protected String id;
    protected String name;
    protected int minOccurs;
    protected int maxOccurs;
    protected Boolean nillable;
    protected transient AttributeDescriptor attribute;
    protected MetadataMap metadata = new MetadataMap();
    protected FeatureTypeInfo featureType;
    protected Class<?> binding;
    protected Integer length;
    protected String source;
    protected GrowableInternationalString description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    @Override
    public boolean isNillable() {
        // NPE safe, defaults to true if not set
        return nillable == null || nillable;
    }

    @Override
    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    @Override
    public FeatureTypeInfo getFeatureType() {
        return featureType;
    }

    @Override
    public void setFeatureType(FeatureTypeInfo featureType) {
        this.featureType = featureType;
    }

    @Override
    public AttributeDescriptor getAttribute() {
        return attribute;
    }

    @Override
    public void setAttribute(AttributeDescriptor attribute) {
        this.attribute = attribute;
    }

    @Override
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

    @Override
    public Class<?> getBinding() {
        return binding;
    }

    @Override
    public void setBinding(Class<?> binding) {
        this.binding = binding;
    }

    @Override
    public Integer getLength() {
        return length;
    }

    @Override
    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public String getSource() {
        if (source == null && name != null) {
            try {
                // is it usable as is?
                ECQL.toExpression(name);
                // even if parseable, dots should be escaped
                if (name.contains(".")) return "\"" + name + "\"";
                return name;
            } catch (CQLException e) {
                // quoting to avoid reserved keyword issues
                return "\"" + name + "\"";
            }
        }
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public GrowableInternationalString getDescription() {
        return description;
    }

    @Override
    public void setDescription(InternationalString description) {
        this.description = InternationalStringUtils.growable(description);
    }

    @Override
    public boolean equals(Object o) {
        return equalsIngnoreFeatureType(o)
                && Objects.equals(featureType, ((AttributeTypeInfoImpl) o).featureType);
    }

    @Override
    public int hashCode() {
        // feature type excluded, or it's gonna go in infinite recursion
        String source = this.source == null ? this.name : this.source;
        return Objects.hash(
                id,
                name,
                minOccurs,
                maxOccurs,
                isNillable(),
                attribute,
                metadata,
                binding,
                length,
                description,
                source);
    }

    @Override
    public boolean equalsIngnoreFeatureType(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeTypeInfoImpl that = (AttributeTypeInfoImpl) o;
        return minOccurs == that.minOccurs
                && maxOccurs == that.maxOccurs
                && Objects.equals(isNillable(), that.isNillable())
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(attribute, that.attribute)
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(binding, that.binding)
                && Objects.equals(length, that.length)
                && Objects.equals(description, that.description)
                // avoid false negatives, source is derived if unset
                && (Objects.equals(source, that.source)
                        || Objects.equals(getSource(), that.getSource()));
    }
}
