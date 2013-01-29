/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Copying some of this implementation from gt-app-schema. Some of it can't be moved here because it has dependencies on other libraries which gt-main
 * cannot include.
 * 
 * @author bro879
 * 
 */
public class AttributeBuilder {
    /**
     * Factory used to create attributes.
     */
    protected FeatureFactory attributeFactory;

    /**
     * Type of complex attribute being built. This field is mutually exclusive with {@link #descriptor}.
     */
    protected AttributeType type;

    /**
     * Descriptor of complex attribute being built. This field is mutually exclusive with {@link #type}.
     */
    protected AttributeDescriptor descriptor;

    /**
     * Contained properties (associations + attributes)
     */
    protected List properties;

    /**
     * The crs of the attribute.
     */
    protected CoordinateReferenceSystem crs;

    /**
     * Namespace context.
     */
    protected String namespace;

    /**
     * Default geometry of the feature.
     */
    protected Object defaultGeometry;

    // getters & setters
    /**
     * @return The type of the attribute being built.
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Sets the type of the attribute being built.
     * <p>
     * When building a complex attribute, this type is used a reference to obtain the types of contained attributes.
     * </p>
     */
    public void setType(AttributeType type) {
        this.type = type;
        this.descriptor = null;
    }

    /**
     * @return The descriptor of the attribute being built or null there is no descriptor (this occurs if setType() was used).
     */
    public AttributeDescriptor getDescriptor() {
        return this.descriptor;
    }

    /**
     * Sets the descriptor of the attribute being built.
     * <p>
     * When building a complex attribute, this type is used a reference to obtain the types of contained attributes.
     * </p>
     */
    public void setDescriptor(AttributeDescriptor descriptor) {
        this.descriptor = descriptor;
        this.type = (AttributeType) descriptor.getType();
    }

    /**
     * @return The coordinate reference system of the feature, or null if not set.
     */
    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    /**
     * Sets the coordinate reference system of the built feature.
     */
    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * @return The default geometry of the feature.
     */
    public Object getDefaultGeometry() {
        return this.defaultGeometry;
    }

    /**
     * Sets the default geometry of the feature.
     */
    public void setDefaultGeometry(Object defaultGeometry) {
        this.defaultGeometry = defaultGeometry;
    }

    /**
     * Convenience accessor for properties list which does the null check.
     */
    protected List getProperties() {
        if (this.properties == null) {
            this.properties = new ArrayList();
        }

        return this.properties;
    }

    // constructor
    public AttributeBuilder(FeatureFactory attributeFactory) {
        this.attributeFactory = attributeFactory;
    }

    // public methods
    /**
     * Adds an attribute to the complex attribute being built. <br>
     * <p>
     * This method uses the type supplied in {@link #setType(AttributeType)} in order to determine the attribute type.
     * </p>
     * 
     * @param id The id of the attribute.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * 
     */
    public Attribute add(String id, Object value, Name name) {
        AttributeDescriptor descriptor = getAttributeDescriptorFor(name);
        Attribute attribute = create(value, null, descriptor, id);
        getProperties().add(attribute);
        return attribute;
    }

    /**
     * Create complex attribute
     * 
     * @param value
     * @param type
     * @param descriptor
     * @param id
     * @return
     */
    public ComplexAttribute createComplexAttribute(Object value, ComplexType type,
            AttributeDescriptor descriptor, String id) {
        return descriptor != null ? attributeFactory.createComplexAttribute((Collection) value,
                descriptor, id) : attributeFactory.createComplexAttribute((Collection) value, type,
                id);
    }

    /**
     * Builds the attribute.
     * <p>
     * The class of the attribute built is determined from its type set with {@link #setType(AttributeType)}.
     * </p>
     * 
     * @return The build attribute.
     */
    public Attribute build() {
        return build(null);
    }

    /**
     * Builds the attribute.
     * <p>
     * The class of the attribute built is determined from its type set with {@link #setType(AttributeType)}.
     * </p>
     * 
     * @param id The id of the attribute, or null.
     * 
     * @return The build attribute.
     */
    public Attribute build(String id) {
        Attribute built = create(getProperties(), type, descriptor, id);

        // FIXME
        // // if geometry, set the crs
        // if (built instanceof GeometryAttribute) {
        // ((GeometryAttribute) built).getDescriptor().setCRS(getCRS());
        // }

        // if feature, set crs and default geometry
        if (built instanceof Feature) {
            Feature feature = (Feature) built;
            // FIXME feature.setCRS(getCRS());
            if (defaultGeometry != null) {
                for (Iterator itr = feature.getProperties().iterator(); itr.hasNext();) {
                    Attribute att = (Attribute) itr.next();
                    if (att instanceof GeometryAttribute) {
                        if (defaultGeometry.equals(att.getValue())) {
                            feature.setDefaultGeometryProperty((GeometryAttribute) att);
                        }
                    }
                }
            }
        }

        getProperties().clear();
        return built;
    }
    
    public Attribute buildSimple(String id, Object value) {
        return create(value, type, descriptor, id);
    }

    /**
     * Resets the builder to its initial state, the same state it is in directly after being instantiated.
     */
    public void init() {
        descriptor = null;
        type = null;
        properties = null;
        crs = null;
        defaultGeometry = null;
    }

    // protected methods
    /**
     * Factors out attribute creation code, needs to be called with either one of type or descriptor.
     */
    protected Attribute create(Object value, AttributeType type, AttributeDescriptor descriptor,
            String id) {
        if (descriptor != null) {
            type = (AttributeType) descriptor.getType();
        }

        if (type instanceof FeatureType) {
            return descriptor != null ? attributeFactory.createFeature((Collection) value,
                    descriptor, id) : attributeFactory.createFeature((Collection) value,
                    (FeatureType) type, id);
        } else if (type instanceof ComplexType) {
        	
        	if (value instanceof AttributeImpl) {
        		return createComplexAttribute((Collection) ((AttributeImpl)value).value, (ComplexType) type, descriptor, id);	
        	}
        	
            return createComplexAttribute((Collection) value, (ComplexType) type, descriptor, id);
        } else if (type instanceof GeometryType) {
            return attributeFactory.createGeometryAttribute(value, (GeometryDescriptor) descriptor,
                    id, getCRS());
        } else {
            return attributeFactory.createAttribute(value, descriptor, id);
        }
    }

    protected AttributeDescriptor getAttributeDescriptorFor(Name name) {
        PropertyDescriptor descriptor = findDescriptor((ComplexType) type, name);

        if (descriptor == null) {
            String msg = "Could not locate attribute: " + name + " in type: " + type.getName();
            throw new IllegalArgumentException(msg);
        }

        if (!(descriptor instanceof AttributeDescriptor)) {
            String msg = name + " references a non attribute";
            throw new IllegalArgumentException(msg);
        }

        return (AttributeDescriptor) descriptor;
    }

    /**
     * Find a descriptor, taking in to account supertypes AND substitution groups
     * 
     * @param parentType type
     * @param name name of descriptor
     * @return descriptor, null if not found
     */
    public static PropertyDescriptor findDescriptor(ComplexType parentType, Name name) {
        // get list of descriptors from types and all supertypes
        List<PropertyDescriptor> descriptors = descriptors(parentType);

        // find matching descriptor
        for (Iterator<PropertyDescriptor> it = descriptors.iterator(); it.hasNext();) {
            PropertyDescriptor d = it.next();
            if (d.getName().equals(name)) {
                return d;
            }
        }

        // nothing found, perhaps polymorphism?? let's loop again
        for (Iterator<PropertyDescriptor> it = descriptors.iterator(); it.hasNext();) {
            List<AttributeDescriptor> substitutionGroup = (List<AttributeDescriptor>) it.next()
                    .getUserData().get("substitutionGroup");
            if (substitutionGroup != null) {
                for (Iterator<AttributeDescriptor> it2 = substitutionGroup.iterator(); it2
                        .hasNext();) {
                    AttributeDescriptor d = it2.next();
                    if (d.getName().equals(name)) {
                        return d;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns the set of all descriptors of a complex type, including from supertypes.
     * 
     * @param type The type, non null.
     * 
     * @return The list of all descriptors.
     */
    public static List<PropertyDescriptor> descriptors(ComplexType type) {
        // get list of descriptors from types and all supertypes
        List<PropertyDescriptor> children = new ArrayList<PropertyDescriptor>();
        ComplexType loopType = type;
        while (loopType != null) {
            children.addAll(loopType.getDescriptors());
            loopType = loopType.getSuper() instanceof ComplexType ? (ComplexType) loopType
                    .getSuper() : null;
        }
        return children;
    }
}
