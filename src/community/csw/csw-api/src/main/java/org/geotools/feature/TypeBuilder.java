/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2011, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.opengis.feature.type.AssociationDescriptor;
import org.opengis.feature.type.AssociationType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.feature.type.Schema;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * Makes building types easier by maintaining state.
 * <p>
 * For reference the builder captures the following state: <table border=1 cellpadding=0
 * cellspacing=0>
 * <tr>
 * <th colspan=2 valign=top>State</th>
 * <th colspan=4>Property</th>
 * <th>Complex</th>
 * <th colspan=2>Feature</th>
 * <th colspan=3>Builder</th>
 * </font> </tr>
 * <tr>
 * <th>scope</th>
 * <th>state</th>
 * <th>descriptor()</th>
 * <th>attribute()</th>
 * <th>geometry()</th>
 * <th>association()</th>
 * <th>complex()</th>
 * <th>feature()</th>
 * <th>collection()</th>
 * <th>init()</th>
 * <th>reset()</th>
 * <th>clear()</th>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <th rowspan=4 valign=top>naming</th>
 * <td>namespace</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <td>name</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <td>isIdentified</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <td>description</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <th rowspan=3 valign=top>type</th>
 * <td>isAbstract</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>superType</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>restriction*</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <th>association</th>
 * <td>referenceType</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <th rowspan=4 valign=top>descriptor</th>
 * <td>minOccurs</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>maxOccurs</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>isNillable</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td>propertyType</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <th>atomic</th>
 * <td>binding</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <th>complex</th>
 * <td>properties*</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <th rowspan=2 valign=top>spatial</th>
 * <td>crs</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr bgcolor=#BBCCAA>
 * <td>defaultGeom</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <th>collection</th>
 * <td>members*</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>x</td>
 * <td>x</td>
 * <td>x</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table> * collection implementation may be specified
 * </p>
 * <p>
 * There are five state control methods:
 * <ul>
 * <li>{@link init( PropertyType )} - completly load settings from type
 * <li>{@link init( PropertyDescriptor )} - completly load settings from descriptor
 * <li>{@link init()} - completly replace settings with builder defaults
 * <ul>
 * <li>{@link reset()} - called after a type creation methods to reset common type settings
 * <li>{@link clear()} - called after an descriptor create method (or add methods) to clear common
 * descriptor settings
 * </ul>
 * </ul>
 * <p>
 * For examples of using this class please see the following type creation methods:
 * <ul>
 * <li>{@link attribute()}
 * <li>{@link geometry()}
 * <li>{@link association()}
 * <li>{@link complex()}
 * <li>{@link feature()}
 * <li>{@link collection()}
 * </ul>
 * <p>
 * 
 * @author Justin Deoliveira
 * @author Jody Garnett
 *
 *
 *
 *
 * @source $URL$
 */
public class TypeBuilder {

    /**
     * factory for creating actual types
     */
    private FeatureTypeFactory factory;

    /**
     * Namespace of the type, and contained types.
     */
    private String namespace;

    /**
     * Local name of the type.
     */
    private String name;

    /**
     * Class the built type is bound to.
     */
    private Class<?> binding;

    /**
     * Description of type.
     */
    private InternationalString description;

    /**
     * Flag indicating wether type is identifiable.
     */
    private boolean isIdentified;

    /**
     * Flag indicating wether type is identifiable.
     */
    private boolean isAbstract;

    /**
     * Additional restrictions on the type.
     */
    private List<Filter> restrictions;

    /**
     * The parent type of the type.
     */
    private PropertyType superType;

    /**
     * Flag indiciating wether added properties are nillable.
     */
    private boolean isNillable;

    /**
     * Map of java class bound to properties types.
     * <p>
     * Please see
     */
    private Map<Class<?>, AttributeType> bindings;

    /**
     * The properties of the type.
     */
    private Collection<PropertyDescriptor> properties;

    /**
     * Minimum occurences of added properties.
     */
    private int minOccurs;

    /**
     * Maximum occurences of added properties.
     */
    private int maxOccurs;

    //
    // FeatureType state
    //
    private Name defaultGeom = null;

    private CoordinateReferenceSystem crs = null;

    /** Used as the target of association() */
    private AttributeType referenceType;

    /** Members of a collection() */
    protected Collection members;

    /**
     * Type used when creating descriptor.
     * <p>
     * May be either a AttributeType or AssocationType.
     */
    private PropertyType propertyType;

    //
    // FeatureCollection state
    //
    public TypeBuilder(FeatureTypeFactory factory) {
        this.factory = factory;
        init();
    }

    public FeatureTypeFactory getTypeFactory() {
        return factory;
    }

    public void setTypeFactory(FeatureTypeFactory factory) {
        this.factory = factory;
    }

    /**
     * Reset is called after creation a "new" type.
     * <p>
     * The following information is reset:
     * <ul>
     * <li>name
     * <li>properties: structural properties (attributes and associations)
     * <li>members: collection members
     * <li>default geometry
     * <li>minOccurs
     * <li>maxOccurs
     * </ul>
     * <p>
     * Note: type type of collection class is not reset, a new instanceof the existing collection
     * class is used.
     */
    public void reset() {
        this.name = null;
        this.description = null;
        this.properties = newCollection(properties);
        this.members = newCollection(members);
        this.defaultGeom = null;
    }

    public void clear() {
        this.minOccurs = 1;
        this.maxOccurs = 1;
        this.isNillable = false;
    }

    /**
     * Initialize this builder.
     * <p>
     * This method cleans the builder of all contained state.
     * </p>
     * 
     * @see reset
     */
    public void init() {
        this.namespace = null;
        this.name = null;
        this.description = null;
        this.isIdentified = false;

        this.binding = null;
        isAbstract = false;
        restrictions = null;
        superType = null;
        properties = null;

        isNillable = true;
        minOccurs = 1;
        maxOccurs = 1;
        propertyType = null;

        defaultGeom = null;
        crs = null;

        referenceType = null;
    }

    public void init(PropertyDescriptor descriptor) {
        init();
        namespace = descriptor.getName().getNamespaceURI();
        name = descriptor.getName().getLocalPart();
        minOccurs = descriptor.getMinOccurs();
        maxOccurs = descriptor.getMaxOccurs();
        if (descriptor instanceof AttributeDescriptor) {
            AttributeDescriptor attribute = (AttributeDescriptor) descriptor;
            isNillable = attribute.isNillable();
            propertyType = attribute.getType();
        }
        if (descriptor instanceof AssociationDescriptor) {
            AssociationDescriptor association = (AssociationDescriptor) descriptor;
            propertyType = association.getType();
        }
    }

    public void init(PropertyType type) {
        init();
        if (type == null)
            return;

        namespace = type.getName().getNamespaceURI();
        name = type.getName().getLocalPart();
        description = type.getDescription();
        isAbstract = type.isAbstract();
        restrictions = null;
        restrictions().addAll(type.getRestrictions());

        if (type instanceof AssociationType) {
            AssociationType assType = (AssociationType) type;

            referenceType = assType.getRelatedType();
            superType = assType.getSuper();
        }
        if (type instanceof AttributeType) {
            AttributeType aType = (AttributeType) type;

            binding = aType.getBinding();
            isIdentified = aType.isIdentified();
            superType = aType.getSuper();
        }
        if (type instanceof GeometryType) {
            GeometryType geometryType = (GeometryType) type;

            this.crs = geometryType.getCoordinateReferenceSystem();
        }
        if (type instanceof ComplexType) {
            ComplexType cType = (ComplexType) type;

            properties = null;
            properties.addAll(cType.getDescriptors());
        }
        if (type instanceof FeatureType) {
            FeatureType featureType = (FeatureType) type;
            defaultGeom = featureType.getGeometryDescriptor().getType().getName();
            crs = featureType.getCoordinateReferenceSystem();
        }
    }

    /**
     * Creation method for AttributeType.
     * <p>
     * Example:
     * 
     * <pre><code>
     * AttributeType TEXT = builder.name(&quot;Text&quot;).bind(String.class).attribute();
     * </code></pre>
     * 
     * </p>
     * <p>
     * Example:
     * 
     * <pre><code>
     * builder.setName(&quot;Interger&quot;);
     * builder.setBinding(Integer.class);
     * AttributeType INTEGER = builder.attribute();
     * </code></pre>
     * 
     * </p>
     * 
     * @return AttributeType created
     */
    public AttributeType attribute() {
        AttributeType type = factory.createAttributeType(typeName(), getBinding(), isIdentified(),
                isAbstract(), restrictions(), getSuper(), getDescription());
        reset();
        return type;
    }

    /** Create AssociationType */
    public AssociationType association() {
        return factory.createAssociationType(typeName(), getReferenceType(), true,
                this.restrictions, getAssociationSuper(), this.getDescription());
    }

    /**
     * Create GeometryType.
     * <p>
     * SFSQL Example JTS LineString.class:
     * 
     * <pre><code>
     * AttributeType ROUTE = builder.name(&quot;Route&quot;).bind(LineString.class).crs(&quot;EPSG:4326&quot;).geometry();
     * </code></pre>
     * 
     * Shape Example Java Rectangle2D:
     * 
     * <pre><code>
     * builder.setName(&quot;Quad&quot;);
     * builder.setBinding(Rectangle2D.class);
     * builder.setCRS(crs);
     * AttributeType QUAD = builder.geometry();
     * </code></pre>
     * 
     * Use of GeoAPI Geometry interfaces is encouraged as implementations are made avaiable.
     * 
     */
    public GeometryType geometry() {
        return getTypeFactory().createGeometryType(typeName(), getBinding(), getCRS(),
                isIdentified(), isAbstract(), restrictions(), getSuper(), getDescription());
    }

    /**
     * Create a complex attribute, made up of other attributes.
     * <p>
     * Example using Set:
     * 
     * <pre><code>
     * builder.setName(&quot;FullName&quot;);
     * builder.setProperties(new HasSet());
     * builder.addAttribute(&quot;first&quot;, TEXT);
     * builder.setMinOccurs(0);
     * builder.setMaxOccurs(Integer.MAX_VALUE);
     * builder.addAttribute(&quot;middle&quot;, TEXT);
     * builder.addAttribute(&quot;last&quot;, TEXT);
     * ComplexType FULLNAME = builder.complex();
     * </code></pre>
     * 
     * <p>
     * Example using chaining:
     * 
     * <pre><code>
     * ComplexType FULLNAME = builder.name(&quot;FullName&quot;).attribute(&quot;first&quot;, TEXT).cardinality(0,
     *         Integer.MAX_VALUE).attribute(&quot;middle&quot;, TEXT).attribute(&quot;last&quot;, TEXT).complex();
     * </code></pre>
     * 
     * @return ComplexType
     */
    public ComplexType complex() {
        ComplexType type = getTypeFactory().createComplexType(typeName(), properties(),
                isIdentified(), isAbstract(), restrictions(), getSuper(), getDescription());
        reset();
        return type;
    }

    /**
     * Create an AttributeDesctiptor, useful for fine grain control.
     * <p>
     * Example:
     * 
     * <pre><code>
     * AttributeDescriptor name = build.name(&quot;name&quot;).property(TEXT).cardinality(1, 5)
     *         .attributeDescriptor();
     * </code></pre>
     * 
     * @return AttributeDescriptor used to define sturcture of ComplexAttribtues
     */
    public AttributeDescriptor attributeDescriptor() {
        // TODO: handle default value
        AttributeDescriptor attribute = getTypeFactory().createAttributeDescriptor(
                (AttributeType) propertyType, typeName(), getMinOccurs(), getMaxOccurs(),
                isNillable(), null);
        reset();
        return attribute;
    }

    /**
     * Create an AssociationDesctiptor, define relationships between ComplexAttribtues (in
     * particular FeatureCollection to members).
     * <p>
     * Example:
     * 
     * <pre><code>
     * AttributeDescriptor contains = build.name(&quot;contains&quot;).property(ROAD).nillable(false).cardinality(0,
     *         Interger.MAX_VALUE).associationDescriptor();
     * </code></pre>
     * 
     * @return AttributeDescriptor used to define sturcture of ComplexAttribtues
     */
    public AssociationDescriptor associationDescriptor() {
        AssociationDescriptor association = getTypeFactory().createAssociationDescriptor(
                (AssociationType) propertyType, typeName(), getMinOccurs(), getMaxOccurs(),
                isNillable());
        reset();
        return association;
    }

    /**
     * Create feature.
     * 
     * @return FeatureType
     */
    public FeatureType feature() {
        // FeatureTypeFactory typeFactory = getTypeFactory();
        FeatureType type = factory.createFeatureType(typeName(), properties(), defaultGeometry(),
                isAbstract(), restrictions(), getSuper(), getDescription());
        reset();
        return type;
    }

//    /**
//     * Creates a FeatureCollectionType.
//     * 
//     * @return FeatureCollectionType based on builder state
//     */
//    public FeatureCollectionType collection() {
//        FeatureCollectionType type = getTypeFactory().createFeatureCollectionType(typeName(),
//                properties(), members(), defaultGeometry(), getCRS(), isAbstract(), restrictions(),
//                getSuper(), getDescription());
//        reset();
//        return type;
//    }

    public void setNamespaceURI(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespaceURI() {
        return namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeBuilder name(String name) {
        setName(name);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setBinding(Class<?> binding) {
        this.binding = binding;
    }

    public TypeBuilder bind(Class<?> binding) {
        setBinding(binding);
        return this;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType type) {
        this.propertyType = type;
    }

    /**
     * Used as a the target for attributeDescriptor or associatioDescriptor().
     * 
     * @param type
     * @return TypeBuilder (for chaining).
     */
    public TypeBuilder property(PropertyType type) {
        setPropertyType(type);
        return this;
    }

    public Class<?> getBinding() {
        return binding;
    }

    public InternationalString getDescription() {
        return description;
    }

    public void setDescription(InternationalString description) {
        this.description = description;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public TypeBuilder nillable(boolean isNillable) {
        this.isNillable = isNillable;
        return this;
    }

    public void setNillable(boolean isNillable) {
        this.isNillable = isNillable;
    }

    public boolean isNillable() {
        return isNillable;
    }

    public void setIdentified(boolean isIdentified) {
        this.isIdentified = isIdentified;
    }

    public boolean isIdentified() {
        return isIdentified;
    }

    public void setSuper(PropertyType superType) {
        this.superType = superType;
    }

    public AttributeType getSuper() {
        return (AttributeType) superType;
    }

    public AssociationType getAssociationSuper() {
        return (AssociationType) superType;
    }

    public void addRestriction(Filter restriction) {
        restrictions.add(restriction);
    }

    public List<Filter> getRestrictions() {
        return Collections.unmodifiableList(restrictions);
    }

    public void setRestrictions(List<Filter> restrictions) {
        this.restrictions = restrictions;
    }

    /**
     * Accessor for restructions which does the null check and creation if necessary.
     */
    protected List<Filter> restrictions() {
        if (restrictions == null) {
            restrictions = createRestrictionSet();
        }
        return restrictions;
    }

    /**
     * Template method to enable subclasses to customize the set implementation used for
     * restrictions.
     * 
     * @return A HashSet.
     */
    protected List<Filter> createRestrictionSet() {
        return new ArrayList<Filter>();
    }

    /**
     * Accessor which returns type banme as follows:
     * <ol>
     * <li>If <code>typeName</code> has been set, its value is returned.
     * <li>If <code>name</code> has been set, it + <code>namespaceURI</code> are returned.
     * </ol>
     * 
     */
    protected Name typeName() {

        // see if local name was set
        if (name != null) {
            // qualify the name with the namespace
            return createTypeName(namespace, name);
        }

        // else the type is anonymous
        return null;
    }

    /**
     * Template method for creating a type name.
     * 
     * @return {@link org.geotools.feature.iso.Types#typeName(String, String)}
     */
    protected Name createTypeName(String ns, String local) {
        return Types.typeName(ns, local);
    }

    /**
     * Used to lookup AttributeType for provided binding.
     * 
     * @param binding
     * @return AttributeType
     * @throws IllegalArgumentExcception
     *                 if class is not bound to a prototype
     */
    public AttributeType getBinding(Class binding) {
        AttributeType type = (AttributeType) bindings().get(binding);
        if (type == null) {
            throw new IllegalArgumentException("No type bound to: " + binding);
        }
        return type;
    }

    /**
     * Used to provide a specific type for provided binding.
     * <p>
     * You can use this method to map the AttributeType used when addAttribute( String name, Class
     * binding ) is called.
     * 
     * @param binding
     * @param type
     */
    public void addBinding(Class binding, AttributeType type) {
        bindings().put(binding, type);
    }

    /**
     * Load the indicated schema to map Java class to your Type System. (please us a profile to
     * prevent binding conflicts).
     * 
     * @param schema
     */
    public void load(Schema schema) {
        for (Iterator itr = schema.values().iterator(); itr.hasNext();) {
            AttributeType type = (AttributeType) itr.next();
            addBinding(type.getBinding(), type);
        }
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public TypeBuilder cardinality(int min, int max) {
        this.minOccurs = min;
        this.maxOccurs = max;
        return this;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    //
    // Structural Properties (Attirbute and Association)
    //
    /**
     * Add a descriptor with a provided name, with the binding
     * 
     * @param name
     *                Name of descriptor (combined with uri for a Name)
     * @param binding
     *                Used to look up a bound AttributeType
     * @return this builder for additional chaining
     */
    public TypeBuilder attribute(String name, Class binding) {
        return attribute(namespace, name, binding);
    }

    /**
     * Add a descriptor with a provided name, with the binding
     * 
     * @param namespaceURI
     * @param name
     *                Name of descriptor (combined with uri for a Name)
     * @param binding
     *                Used to look up a bound AttributeType
     * @return this builder for additional chaining
     */
    public TypeBuilder attribute(String namespaceURI, String name, Class binding) {
        return attribute(createName(namespaceURI, name), binding);
    }

    public TypeBuilder attribute(Name name, Class binding) {
        return attribute(name, getBinding(binding));
    }

    public TypeBuilder attribute(String name, String namespaceURI, AttributeType type) {
        attribute(createName(namespaceURI, name), type);
        return this;
    }

    public TypeBuilder attribute(String name, AttributeType type) {
        attribute(name, getNamespaceURI(), type);
        return this;
    }

    public TypeBuilder attribute(Name name, AttributeType type) {
        // TODO: handle default value
        AttributeDescriptor descriptor = getTypeFactory().createAttributeDescriptor(type, name,
                getMinOccurs(), getMaxOccurs(), isNillable(), null);
        add(descriptor);
        return this;
    }

    public void addAttribute(String name, Class binding) {
        addAttribute(namespace, name, binding);
    }

    public void addAttribute(String uri, String name, Class binding) {
        addAttribute(createName(uri, name), binding);
    }

    public void addAttribute(Name name, Class binding) {
        addAttribute(name, getBinding(binding));
    }

    public void addAttribute(String name, AttributeType type) {
        addAttribute(name, getNamespaceURI(), type);
    }

    public void addAttribute(String name, String namespaceURI, AttributeType type) {
        addAttribute(createName(namespaceURI, name), type);
    }

    public void addAttribute(Name name, AttributeType type) {
        // TODO: handle default value
        AttributeDescriptor descriptor = getTypeFactory().createAttributeDescriptor(type, name,
                getMinOccurs(), getMaxOccurs(), isNillable(), null);
        add(descriptor);
    }

    /**
     * Allows you to indicate the reference type to be used with Association to be created.
     */
    public void setReferenceType(AttributeType reference) {
        this.referenceType = reference;
    }

    public TypeBuilder referenceType(AttributeType reference) {
        setReferenceType(reference);
        return this;
    }

    public AttributeType getReferenceType() {
        return referenceType;
    }

    public TypeBuilder association(String name, AssociationType type) {
        return association(getNamespaceURI(), name, type);
    }

    public TypeBuilder association(String namespaceURI, String name, AssociationType type) {
        return association(createName(namespaceURI, name), type);
    }

    public TypeBuilder association(Name name, AssociationType type) {
        AssociationDescriptor descriptor = getTypeFactory().createAssociationDescriptor(type, name,
                getMinOccurs(), getMaxOccurs(), isNillable());

        add(descriptor);
        return this;
    }

    /**
     * Add provided descriptor to the type to be created.
     * <p>
     * Please note that you may not have two types with the same name, depending on the factory
     * being used the order of the structural content may be signficant - this builder will preserve
     * order although the factory may or may not make use of this fact.
     * </p>
     */
    public TypeBuilder add(PropertyDescriptor descriptor) {
        if (!contains(properties(), descriptor)) {
            properties.add(descriptor);
        }
        clear();
        return this;
    }

    public static boolean contains(Collection collection, PropertyDescriptor descriptor) {
        // check for a descriptor with the same name
        for (Iterator itr = collection.iterator(); itr.hasNext();) {
            PropertyDescriptor d = (PropertyDescriptor) itr.next();
            if (d.getName().equals(descriptor.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Access to properties used by builder.
     * <p>
     * You can use this method to perform collection opperations before construction. This is most
     * useful when initializing the builder with a known type, performing modifications, and then
     * creating a derrived type.
     * </p>
     */
    public Collection<PropertyDescriptor> getProperties() {
        if (properties == null) {
            properties = newCollection();
        }
        return properties;
    }

    /**
     * Allow for user supplied collection implementaion used for properties.
     * <p>
     * Examples of useful property collections:
     * <ul>
     * <li>List - structured properties maintained in order
     * <li>Set - order of structured properties insignificant
     * <li>etc...
     * </ul>
     * The collection class used here should be matched by content described by this type.
     * Explicitly a FeatureType with <code>getProperties() instanceof Set</code> indicates that
     * Features of that FeatureType should maintain a Set of properties where order is not
     * significant.
     * </p>
     * 
     * @param properties
     *                Collection implementation used to organize properties
     */
    public void setProperties(Collection<PropertyDescriptor> properties) {
        this.properties = properties;
    }

    //
    // Creation
    //
    /**
     * Template method to enable subclasses to customize the collection implementation used by
     * "default".
     * <p>
     * Considered moving this to the type interface but it would be in appropriate as the user may
     * need to specifiy different collections for seperate types in the same schema.
     * </p>
     * 
     * @return Collection (subclass may override)
     */
    protected Collection<PropertyDescriptor> newCollection() {
        return new HashSet<PropertyDescriptor>();
    }

    /**
     * Provides an empty copy of the provided origional collection.
     * <p>
     * This method is used by reset for the following goals:
     * <ul>
     * <li>use the user supplied collection directly by the TypeFactory,
     * <li>remember the user supplied collection type for subsequent builder use
     * </ul>
     * This allows a user to indicate that properties are stored in a "LinkedList" once.
     * 
     * @param origional
     *                Origional collection
     * @return New instance of the originoal Collection
     */
    protected Collection newCollection(Collection origional) {
        if (origional == null) {
            return newCollection();
        }
        try {
            return (Collection) origional.getClass().newInstance();
        } catch (InstantiationException e) {
            return newCollection();
        } catch (IllegalAccessException e) {
            return newCollection();
        }
    }

    //
    // Factory method argument preparation
    //
    /**
     * Grab property collection as an argument to factory method.
     * <p>
     * This may return a copy as needed, since most calls to a factory method end up with a reset
     * this seems not be needed at present.
     * </p>
     */
    protected Collection<PropertyDescriptor> properties() {
        if (properties == null) {
            properties = newCollection();
        }
        return properties;
    }

    /**
     * Accessor for bindings.
     */
    protected Map bindings() {
        if (bindings == null) {
            bindings = new HashMap();
        }
        return bindings;
    }

    /**
     * Template method for creating an attribute name.
     * 
     * @return {@link org.geotools.feature.Types#typeName(String, String)}
     */
    protected Name createName(String ns, String local) {
        return Types.typeName(ns, local);
    }

    public void setDefaultGeometry(String name) {
        setDefaultGeometry(name, getNamespaceURI());
    }

    public void setDefaultGeometry(String name, String namespaceURI) {
        setDefaultGeometry(createName(namespaceURI, name));
    }

    public void setDefaultGeometry(Name name) {
        defaultGeom = name;
    }

    public TypeBuilder defaultGeometry(String name) {
        setDefaultGeometry(name);
        return this;
    }

    public Name getDefaultGeometry() {
        return defaultGeom;
    }

    /**
     * Convenience method for getting the descriptor of the default geometry type. This method will
     * first try to look up the supplied <code>defaultGeom</code> property, if it cant find, it
     * will try to locate any added geometry.
     * 
     */
    protected GeometryDescriptor defaultGeometry() {
        if (defaultGeom != null) {
            for (PropertyDescriptor pd : properties) {
                if (pd.getName().equals(defaultGeom)) {
                    return (GeometryDescriptor) pd;
                }
            }
        }

        // not found or not set, return first geometry
        for (PropertyDescriptor pd : properties) {
            if (pd instanceof GeometryDescriptor) {
                return (GeometryDescriptor) pd;
            }
        }
        return null;
    }

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public TypeBuilder crs(CoordinateReferenceSystem crs) {
        setCRS(crs);
        return this;
    }

    /**
     * Uses CRS utility class with buildres TypeFactory.getCRSFactory to look up a
     * CoordinateReferenceSystem based on the provied srs.
     * <p>
     * A SpatialReferenceSystem can be one of the following:
     * <ul>
     * <li>"AUTHORITY:CODE"
     * <li>Well Known Text
     * </ul>
     * 
     * @param srs
     * @return TypeBuilder ready for chaining
     * @throws IllegalArgumentException
     *                 When SRS not understood
     */
    public TypeBuilder crs(String SRS) {
        try {
            setCRS(CRS.decode(SRS));
        } catch (Exception e) {
            IllegalArgumentException iae = new IllegalArgumentException("SRS '" + SRS
                    + "' unknown:" + e);
            iae.initCause(e);
            throw iae;
        }
        return this;
    }

    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    //
    // Feature Collection
    //
    /**
     * Access to members used by builder.
     * <p>
     * You can use this method to perform collection opperations before construction. This is most
     * useful when initializing the builder with a known type, performing modifications, and then
     * creating a derrived type.
     * </p>
     */
    public Collection getMembers() {
        if (members == null) {
            members = newCollection();
        }
        return members;
    }

    /** Provide collection class used organize collection members */
    public void setMembers(Collection members) {
        this.members = members;
    }

    /**
     * Grab member collection as an argument to factory method.
     * <p>
     * This may return a copy as needed, since most calls to a factory method end up with a reset
     * this seems not be needed at present.
     * </p>
     */
    protected Collection members() {
        if (members == null) {
            members = newCollection();
        }
        return members;
    }

    /**
     * Creates a association descriptor and adds to collection members.
     * <p>
     * Calls clear to reset cardinality after use.
     * </p>
     * 
     * @param name
     * @param type
     */
    public void addMemberType(String name, AssociationType memberType) {
        addMemberType(getNamespaceURI(), name, memberType);
    }

    /**
     * Creates a association descriptor and adds to collection members.
     * <p>
     * Calls clear to reset cardinality after use.
     * </p>
     * 
     * @param name
     * @param type
     */
    public void addMemberType(String namespaceURI, String name, AssociationType memberType) {
        addMemberType(createName(namespaceURI, name), memberType);
    }

    /**
     * Creates a association descriptor and adds to collection members.
     * <p>
     * Calls clear to reset cardinality after use.
     * </p>
     * 
     * @param name
     * @param type
     */
    public void addMemberType(Name name, AssociationType/* <FeatureType> */memberType) {
        member(name, memberType);
    }

    /**
     * Creates a association descriptor and adds to collection members.
     * <p>
     * Calls clear to reset cardinality after use.
     * </p>
     * 
     * @param name
     * @param type
     * @return TypeBuilder for operation chaining
     */
    public TypeBuilder member(String name, AssociationType type) {
        return member(createName(getNamespaceURI(), name), type);
    }

    /**
     * Creates a association descriptor and adds to collection members.
     * <p>
     * Calls clear to reset cardinality after use.
     * </p>
     * 
     * @param name
     * @param type
     * @return TypeBuilder for operation chaining
     */
    public TypeBuilder member(Name name, AssociationType type) {
        AssociationDescriptor descriptor = getTypeFactory().createAssociationDescriptor(type, name,
                getMinOccurs(), getMaxOccurs(), isNillable());
        clear();
        return member(descriptor);
    }

    public TypeBuilder member(AssociationDescriptor memberOf) {
        if (!contains(members(), memberOf)) {
            members.add(memberOf);
        }
        return this;
    }
}
