/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.util.InternationalString;

/** Concrete implementation of a DynamicComplexType */
public class DynamicComplexTypeImpl extends AttributeTypeImpl implements DynamicComplexType {

    private final Collection<PropertyDescriptor> properties;

    private final Map<Name, PropertyDescriptor> propertyMap;

    public DynamicComplexTypeImpl(
            Name name,
            Collection<PropertyDescriptor> properties,
            boolean identified,
            boolean isAbstract,
            List<Filter> restrictions,
            AttributeType superType,
            InternationalString description) {
        super(name, Collection.class, identified, isAbstract, restrictions, superType, description);
        Map<Name, PropertyDescriptor> localPropertyMap;
        if (properties == null) {
            localPropertyMap = new HashMap<>();
        } else {
            localPropertyMap = new HashMap<>();
            for (PropertyDescriptor pd : properties) {
                if (pd == null) {
                    // descriptor entry may be null if a request was made for a property that does
                    // not exist
                    throw new NullPointerException(
                            "PropertyDescriptor is null - did you request a property that does not exist?");
                }
                localPropertyMap.put(pd.getName(), pd);
            }
        }
        this.properties = properties;
        this.propertyMap = localPropertyMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Collection<Property>> getBinding() {
        return (Class<Collection<Property>>) super.getBinding();
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {
        return properties;
    }

    @Override
    public PropertyDescriptor getDescriptor(Name name) {
        PropertyDescriptor propertyDescriptor = propertyMap.get(name);
        if (propertyDescriptor == null)
            propertyDescriptor = getDescriptorByLocalPart(name.getLocalPart());
        return propertyDescriptor;
    }

    @Override
    public PropertyDescriptor getDescriptor(String name) {
        PropertyDescriptor result = getDescriptor(new NameImpl(name));
        if (result == null) {
            // look in the same namespace as the complex type
            result = getDescriptor(new NameImpl(getName().getNamespaceURI(), name));
            if (result == null) {
                getDescriptorByLocalPart(name);
            }
        }
        return result;
    }

    private PropertyDescriptor getDescriptorByLocalPart(String localPart) {
        for (PropertyDescriptor pd : properties) {
            if (pd.getName().getLocalPart().equals(localPart)) {
                return pd;
            }
        }
        return null;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    @SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        DynamicComplexTypeImpl other = (DynamicComplexTypeImpl) o;
        if (!properties.equals(other.properties)) {
            return false;
        }
        return true;
    }

    @Override
    public void addPropertyDescriptor(PropertyDescriptor descriptor) {
        if (!properties.contains(descriptor)) {
            properties.add(descriptor);
            propertyMap.put(descriptor.getName(), descriptor);
        }
    }

    public void removePropertyDescriptor(PropertyDescriptor descriptor) {
        if (properties.contains(descriptor)) {
            properties.remove(descriptor);
            propertyMap.remove(descriptor.getName());
        }
    }
}
