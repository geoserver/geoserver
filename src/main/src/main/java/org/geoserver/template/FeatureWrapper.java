/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataUtilities;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.MapEntry;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * Wraps a {@link Feature} in the freemarker {@link BeansWrapper} interface allowing a template to
 * be directly applied to a {@link Feature} or {@link FeatureCollection}.
 *
 * <p>When a {@link FeatureCollection} is being processed by the template, it is available via the
 * <code>$features</code> variable, which can be broken down into single features and attributes
 * following this hierarchy:
 *
 * <ul>
 *   <li>features -> feature
 *       <ul>
 *         <li>fid (String)
 *         <li>typeName (String)
 *         <li>attributes -> attribute
 *             <ul>
 *               <li>value (String), a default String representation of the attribute value
 *               <li>rawValue (Object), the actual attribute value if it's non null, the empty
 *                   string otherwise
 *               <li>name (String)
 *               <li>type (String)
 *               <li>isGeometry (Boolean)
 *             </ul>
 *       </ul>
 * </ul>
 *
 * Example of a template processing a feature collection which will print out the features id of
 * every feature in the collection.
 *
 * <pre><code>
 *  &lt;#list features as feature&gt;
 *  FeatureId: ${feature.fid}
 *  &lt;/#list&gt;
 * </code></pre>
 *
 * <p>To use this wrapper,use the {@link
 * Configuration#setObjectWrapper(freemarker.template.ObjectWrapper)} method:
 *
 * <pre>
 *         <code>
 *  //features we want to apply template to
 *  FeatureCollection features = ...;
 *  //create the configuration and set the wrapper
 *  Configuration cfg = TemplateUtils.getSafeConfiguration();
 *  cfg.setObjectWrapper( new FeatureWrapper() );
 *  //get the template and go
 *  Template template = cfg.getTemplate( &quot;foo.ftl&quot; );
 *  template.process( features, System.out );
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime, TOPP
 * @author Gabriel Roldan, TOPP
 */
public class FeatureWrapper extends BeansWrapper {
    static Catalog gsCatalog;

    /** factory to create CollectionTemplateModel from FeatureCollection */
    protected TemplateFeatureCollectionFactory templateFeatureCollectionFactory;

    public FeatureWrapper() {
        setSimpleMapWrapper(true);
        this.templateFeatureCollectionFactory = copyTemplateFeatureCollectionFactory;
    }

    public FeatureWrapper(TemplateFeatureCollectionFactory templateFeatureCollectionFactory) {
        setSimpleMapWrapper(true);
        this.templateFeatureCollectionFactory = templateFeatureCollectionFactory;
    }

    private Catalog getCatalog() {
        try {
            return (gsCatalog = (Catalog) GeoServerExtensions.bean("catalog"));
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    /**
     * Returns a sensible String value for attributes so they are easily used by templates.
     *
     * <p>Special cases:
     *
     * <ul>
     *   <li>for Date values returns a default {@link DateFormat} representation
     *   <li>for Boolean values returns "true" or "false"
     *   <li>for null values returns an empty string
     *   <li>for any other value returns its toString()
     * </ul>
     *
     * @param o could be an instance of Date (a special case)
     * @return the formated date as a String, or the object
     */
    protected String wrapValue(Object o) {
        return valueToString(o);
    }

    /**
     * Returns a sensible String value for attributes so they are easily used by templates.
     *
     * <p>Special cases:
     *
     * <ul>
     *   <li>for Date values returns a default {@link DateFormat} representation
     *   <li>for Boolean values returns "true" or "false"
     *   <li>for null values returns an empty string
     *   <li>for any other value returns its toString()
     * </ul>
     *
     * @param o the object for which to return a String representation suitable to be used as
     *     template content
     * @return the formated date as a String, or the object
     */
    private static String valueToString(Object o) {
        if (o == null) {
            // nulls throw tempaltes off, use empty string
            return "";
        }
        if (o instanceof Date) {
            if (o instanceof Timestamp) {
                return DateFormat.getDateTimeInstance().format((Date) o);
            }
            if (o instanceof Time) {
                return DateFormat.getTimeInstance().format((Date) o);
            }
            return DateFormat.getInstance().format((Date) o);
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? "true" : "false";
        }
        if (o instanceof Geometry) {
            return String.valueOf(o);
        }
        return String.valueOf(o);
    }

    public String getPrefix(Name name) {
        Catalog cat = getCatalog();
        if (cat == null) {
            return "";
        }
        if (name.getNamespaceURI() == null) {
            return "";
        }
        NamespaceInfo ni = cat.getNamespaceByURI(name.getNamespaceURI());
        return ni == null ? "" : ni.getPrefix();
    }

    public String getNamespace(Name name) {
        return name.getNamespaceURI() == null ? "" : name.getNamespaceURI();
    }

    public TemplateModel wrap(Object object) throws TemplateModelException {
        // check for feature collection
        if (object instanceof FeatureCollection) {
            // create a model with just one variable called 'features'
            SimpleHash map = new SimpleHash();
            map.put(
                    "features",
                    templateFeatureCollectionFactory.createTemplateFeatureCollection(
                            (FeatureCollection) object, this));
            map.put("type", wrap(((FeatureCollection) object).getSchema()));

            return map;
        } else if (object instanceof ComplexType) {

            return buildType((ComplexType) object);

        } else if (object instanceof Feature) {

            return buildComplex((Feature) object);
        }

        return super.wrap(object);
    }

    private SimpleHash buildType(ComplexType ft) {
        // create a variable "attributes" which his a list of all the
        // attributes, but at the same time, is a map keyed by name
        Map<String, Object> attributeMap = new LinkedHashMap<String, Object>();
        Collection<PropertyDescriptor> descriptors = ft.getDescriptors();
        for (Iterator<PropertyDescriptor> it = descriptors.iterator(); it.hasNext(); ) {
            PropertyDescriptor descr = it.next();

            Map<String, Object> attribute = new HashMap<String, Object>();
            attribute.put("name", descr.getName().getLocalPart());
            attribute.put("namespace", getNamespace(descr.getName()));
            attribute.put("prefix", getPrefix(descr.getName()));
            attribute.put("type", descr.getType().getBinding().getName());
            attribute.put(
                    "isGeometry",
                    Boolean.valueOf(Geometry.class.isAssignableFrom(descr.getType().getBinding())));

            attributeMap.put(descr.getName().toString(), attribute);
        }

        // build up the result, feature type is represented by its name an
        // attributes
        SimpleHash map = new SimpleHash();
        map.put("attributes", new SequenceMapModel(attributeMap, this));
        map.put("name", ft.getName().getLocalPart());
        map.put("namespace", getNamespace(ft.getName()));
        map.put("prefix", getPrefix(ft.getName()));

        return map;
    }

    private SimpleHash buildComplex(ComplexAttribute att) {
        // create the model
        SimpleHash map = new SimpleHash();

        // next create the Map representing the per attribute useful
        // properties for a template
        Map attributeMap = new FeatureAttributesMap(att);
        map.putAll(attributeMap);

        Catalog cat = getCatalog();

        ResourceInfo info = null;
        if (cat != null) {
            info =
                    cat.getResourceByName(
                            att.getType().getName().getNamespaceURI(),
                            att.getType().getName().getLocalPart(),
                            ResourceInfo.class);

            if (info != null) {
                map.put("type", info);
            }
        }

        if (info == null) {
            map.put("type", buildDummyFeatureTypeInfo(att));
        }

        // Add the metadata after setting the attributes so they aren't masked by feature attributes
        if (att.getIdentifier() != null) {
            map.put("fid", att.getIdentifier().getID());
        } else {
            map.put("fid", "");
        }
        map.put("typeName", att.getType().getName().getLocalPart());

        // create a variable "attributes" which his a list of all the
        // attributes, but at the same time, is a map keyed by name
        map.put("attributes", new SequenceMapModel(attributeMap, this));

        return map;
    }

    private Map<String, Object> buildDummyFeatureTypeInfo(ComplexAttribute f) {
        Map<String, Object> dummy = new HashMap<String, Object>();
        dummy.put("name", f.getType().getName().getLocalPart());
        dummy.put("namespace", getNamespace(f.getType().getName()));
        dummy.put("prefix", getPrefix(f.getType().getName()));
        dummy.put("title", "Layer: " + f.getType().getName().getLocalPart());
        dummy.put("abstract", "[No Abstract Provided]");
        dummy.put("description", "[No Description Provided]");
        dummy.put("keywords", new ArrayList<String>());
        dummy.put("metadataLinks", new ArrayList<String>());
        dummy.put("SRS", "[SRS]");
        if (f instanceof Feature) {
            final GeometryDescriptor gd = ((Feature) f).getType().getGeometryDescriptor();
            if (gd != null) {
                dummy.put("nativeCRS", gd.getCoordinateReferenceSystem());
            }
        }
        return dummy;
    }

    /**
     * Adapts a Feature to a java.util.Map, where the map keys are the feature attribute names and
     * the values other Map representing the Feature name/value attributes.
     *
     * <p>A special purpose Map implementation is used in order to lazily return the attribute
     * properties, most notably the toString representation of attribute values.
     *
     * @author Gabriel Roldan
     * @see AttributeMap
     */
    private class FeatureAttributesMap extends AbstractMap {
        private Set<MapEntry> entrySet;

        private ComplexAttribute feature;

        public FeatureAttributesMap(ComplexAttribute feature) {
            this.feature = feature;
        }

        public Set entrySet() {
            if (entrySet == null) {
                entrySet = new LinkedHashSet<MapEntry>();
                final Collection<PropertyDescriptor> types = feature.getType().getDescriptors();
                Name attName;
                Map attributesMap;
                for (Iterator<PropertyDescriptor> iterator = types.iterator();
                        iterator.hasNext(); ) {
                    attName = iterator.next().getName();
                    attributesMap = new AttributeMap(attName, feature);
                    entrySet.add(
                            new MapEntry<Object, Object>(attName.getLocalPart(), attributesMap));
                }
            }
            return entrySet;
        }
    }

    /**
     * Wraps a Feature as a <code>Map&lt;String, Map&lt;String, Object&gt;&gt;</code>.
     *
     * <p>The Map keys are the wrapped feature's property names and the Map values are Maps with
     * appropriate key/value pairs for each feature attribute.
     *
     * <p>For instance, the value attribute Maps hold the following properties:
     *
     * <ul>
     *   <li>name: String holding the attribute name
     *   <li>type: String with the java class name bound to the attribute type
     *   <li>value: String representation of the attribute value suitable to be used directly in a
     *       template expression. <code>null</code> values are returned as the empty string, non
     *       String values as per {@link FeatureWrapper#valueToString(Object)}
     *   <li>rawValue: the actual attribute value as it is in the Feature
     *   <li>isGeometry: Boolean indicating whether the attribute is of a geometric type
     * </ul>
     */
    private class AttributeMap extends AbstractMap {

        private final Name attributeName;

        private final ComplexAttribute feature;

        private Set<MapEntry> entrySet;

        /**
         * Builds an "attribute map" as used in templates for the given attribute of the given
         * feature.
         *
         * @param attributeName the name of the feature attribute this attribute map is built for
         * @param feature the feature where to lazily grab the attribute named <code>attributeName
         *     </code> from
         */
        public AttributeMap(final Name attributeName, final ComplexAttribute feature) {
            this.attributeName = attributeName;
            this.feature = feature;
        }

        /**
         * Override so asking for the hashCode does not implies traversing the whole map and thus
         * calling entrySet() prematurely
         */
        @SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
        public int hashCode() {
            return attributeName.hashCode();
        }

        /**
         * Returns this map's entry set. An entry for each of the properties mentioned in this
         * class's javadoc is returned. Of special interest is the entry for the <code>"value"
         * </code> property, which is lazily evaluated through the use of a {@link
         * DeferredValueEntry}
         */
        public Set entrySet() {
            if (entrySet == null) {
                entrySet = new LinkedHashSet<MapEntry>();
                final ComplexType featureType = feature.getType();
                PropertyDescriptor attributeDescr = featureType.getDescriptor(attributeName);
                Property property = feature.getProperty(attributeName);

                if (property == null) {
                    // maybe polymorphism? let's try
                    @SuppressWarnings("unchecked")
                    List<AttributeDescriptor> substitutionGroup =
                            (List<AttributeDescriptor>)
                                    attributeDescr.getUserData().get("substitutionGroup");
                    if (substitutionGroup != null) {
                        Iterator<AttributeDescriptor> it = substitutionGroup.iterator();
                        while (property == null && it.hasNext()) {
                            property = feature.getProperty(it.next().getName());
                        }
                        if (property != null) {
                            attributeDescr = property.getDescriptor();
                        }
                    }
                }

                entrySet.add(
                        new MapEntry<Object, Object>(
                                "isComplex", property instanceof ComplexAttribute));

                Object value = null;
                if (property instanceof ComplexAttribute) {
                    value = buildComplex((ComplexAttribute) property);
                } else if (property != null) {
                    value = property.getValue();
                }

                entrySet.add(new DeferredValueEntry("value", value));
                entrySet.add(new MapEntry<Object, Object>("name", attributeName.getLocalPart()));
                entrySet.add(
                        new MapEntry<Object, Object>("namespace", getNamespace(attributeName)));
                entrySet.add(new MapEntry<Object, Object>("prefix", getPrefix(attributeName)));

                if (attributeDescr.getType() instanceof ComplexType) {
                    entrySet.add(
                            new MapEntry<Object, Object>(
                                    "type", buildType((ComplexType) attributeDescr.getType())));
                } else {
                    entrySet.add(
                            new MapEntry<Object, Object>(
                                    "type", attributeDescr.getType().getBinding().getName()));
                }

                Object rawValue = value == null ? "" : value;
                boolean isGeometry =
                        Geometry.class.isAssignableFrom(attributeDescr.getType().getBinding());
                entrySet.add(
                        new MapEntry<Object, Object>("isGeometry", Boolean.valueOf(isGeometry)));
                entrySet.add(new MapEntry<Object, Object>("rawValue", rawValue));
            }
            return entrySet;
        }

        /**
         * A special purpose Map.Entry whose value is transformed to String on demand, thus avoiding
         * to hold both the actual value object and its string value.
         *
         * @see FeatureWrapper#valueToString(Object)
         */
        private class DeferredValueEntry extends MapEntry<Object, Object> {
            private static final long serialVersionUID = -3919798947862996744L;

            public DeferredValueEntry(String key, Object attribute) {
                super(key, attribute);
            }

            /** Returns the value corresponding to this entry, as a String. */
            public Object getValue() {
                Object actualValue = super.getValue();
                String stringValue = FeatureWrapper.valueToString(actualValue);
                return stringValue;
            }
        }
    }

    /**
     * Factory to Create TemplateCollectionModel from FeatureCollection
     *
     * @author Niels Charlier, Curtin University of Technology
     */
    public static interface TemplateFeatureCollectionFactory<
            T extends TemplateCollectionModel & TemplateSequenceModel> {
        public TemplateCollectionModel createTemplateFeatureCollection(
                FeatureCollection collection, BeansWrapper wrapper);
    }

    /**
     * Default Factory to Create TemplateCollectionModel from FeatureCollection by making a copy of
     * features in list
     *
     * @author Niels Charlier, Curtin University of Technology
     */
    protected static class CopyTemplateFeatureCollectionFactory
            implements TemplateFeatureCollectionFactory<CollectionModel> {

        @SuppressWarnings("unchecked")
        public CollectionModel createTemplateFeatureCollection(
                FeatureCollection collection, BeansWrapper wrapper) {
            return new CollectionModel(DataUtilities.list(collection), wrapper);
        }
    }

    /** Default Template FeatureCollection Factory */
    protected static CopyTemplateFeatureCollectionFactory copyTemplateFeatureCollectionFactory =
            new CopyTemplateFeatureCollectionFactory();
}
