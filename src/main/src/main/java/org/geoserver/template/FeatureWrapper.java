/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.template;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataUtilities;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.MapEntry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.vividsolutions.jts.geom.Geometry;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Wraps a {@link Feature} in the freemarker {@link BeansWrapper} interface
 * allowing a template to be directly applied to a {@link Feature} or
 * {@link FeatureCollection}.
 * <p>
 * When a {@link FeatureCollection} is being processed by the template, it is
 * available via the <code>$features</code> variable, which can be broken down
 * into single features and attributes following this hierarchy:
 * <ul>
 * <li>features -> feature</li>
 * <ul>
 * <li>fid (String)</li>
 * <li>typeName (String)</li>
 * <li>attributes -> attribute</li>
 * <ul>
 * <li>value (String), a default String representation of the attribute value</li>
 * <li>rawValue (Object), the actual attribute value if it's non null, the
 * empty string otherwise</li>
 * <li>name (String)</li>
 * <li>type (String)</li>
 * <li>isGeometry (Boolean)</li>
 * </ul>
 * </ul>
 * </ul>
 * Example of a template processing a feature collection which will print out
 * the features id of every feature in the collection.
 * 
 * <pre><code>
 *  &lt;#list features as feature&gt;
 *  FeatureId: ${feature.fid}
 *  &lt;/#list&gt;
 * </code></pre>
 * 
 * </p>
 * <p>
 * To use this wrapper,use the
 * {@link Configuration#setObjectWrapper(freemarker.template.ObjectWrapper)}
 * method:
 * 
 * <pre>
 *         <code>
 *  //features we want to apply template to
 *  FeatureCollection features = ...;
 *  //create the configuration and set the wrapper
 *  Configuration cfg = new Configuration();
 *  cfg.setObjectWrapper( new FeatureWrapper() );
 *  //get the template and go
 *  Template template = cfg.getTemplate( &quot;foo.ftl&quot; );
 *  template.process( features, System.out );
 * </code>
 * </pre>
 * 
 * </p>
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime, TOPP
 * @author Gabriel Roldan, TOPP
 */
public class FeatureWrapper extends BeansWrapper {
    static Catalog gsCatalog;

    public FeatureWrapper() {
        setSimpleMapWrapper(true);
    }

    private Catalog getCatalog() {
        if (gsCatalog != null) 
            return gsCatalog;

        try {
            return (gsCatalog = (Catalog)GeoServerExtensions.bean("catalog2"));
        } catch (NoSuchBeanDefinitionException e){
            return null;
        }
    }

    /**
     * Returns a sensible String value for attributes so they are easily used by
     * templates.
     * <p>
     * Special cases:
     * <ul>
     * <li>for Date values returns a default {@link DateFormat} representation</li>
     * <li>for Boolean values returns "true" or "false"</li>
     * <li>for null values returns an empty string</li>
     * <li>for any other value returns its toString()</li>
     * </ul>
     * </p>
     * 
     * @param o
     *            could be an instance of Date (a special case)
     * @return the formated date as a String, or the object
     */
    protected String wrapValue(Object o) {
        return valueToString(o);
    }

    /**
     * Returns a sensible String value for attributes so they are easily used by
     * templates.
     * <p>
     * Special cases:
     * <ul>
     * <li>for Date values returns a default {@link DateFormat} representation</li>
     * <li>for Boolean values returns "true" or "false"</li>
     * <li>for null values returns an empty string</li>
     * <li>for any other value returns its toString()</li>
     * </ul>
     * </p>
     * 
     * @param o
     *            the object for which to return a String representation
     *            suitable to be used as template content
     * @return the formated date as a String, or the object
     */
    private static String valueToString(Object o) {
        if (o == null) {
            // nulls throw tempaltes off, use empty string
            return "";
        }
        if (o instanceof Date) {
            if ( o instanceof Timestamp ) {
                return DateFormat.getDateTimeInstance().format((Date)o);
            }
            if ( o instanceof Time ) {
                return DateFormat.getTimeInstance().format((Date)o);
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

    public TemplateModel wrap(Object object) throws TemplateModelException {
        // check for feature collection
        if (object instanceof FeatureCollection) {
            // create a model with just one variable called 'features'
            SimpleHash map = new SimpleHash();
            map.put("features", new CollectionModel(DataUtilities.list((FeatureCollection) object), this));
            map.put("type", wrap(((FeatureCollection) object).getSchema()));

            return map;
        } else if (object instanceof SimpleFeatureType) {
            SimpleFeatureType ft = (SimpleFeatureType) object;

            // create a variable "attributes" which his a list of all the
            // attributes, but at the same time, is a map keyed by name
            Map attributeMap = new LinkedHashMap();
            for (int i = 0; i < ft.getAttributeCount(); i++) {
                AttributeDescriptor type = ft.getDescriptor(i);

                Map attribute = new HashMap();
                attribute.put("name", type.getLocalName());
                attribute.put("type", type.getType().getBinding().getName());
                attribute.put("isGeometry", Boolean.valueOf(Geometry.class.isAssignableFrom(type.getType().getBinding())));

                attributeMap.put(type.getLocalName(), attribute);
            }

            // build up the result, feature type is represented by its name an
            // attributes
            SimpleHash map = new SimpleHash();
            map.put("attributes", new SequenceMapModel(attributeMap, this));
            map.put("name", ft.getTypeName());
            return map;
        } else if (object instanceof SimpleFeature) {

            SimpleFeature feature = (SimpleFeature) object;

            // create the model
            SimpleHash map = new SimpleHash();

            // next create the Map representing the per attribute useful
            // properties for a template
            Map attributeMap = new FeatureAttributesMap(feature);
            map.putAll(attributeMap);

            Catalog cat = getCatalog();

            if (cat != null){
                NamespaceInfo ns = cat.getNamespaceByURI(
                        feature.getFeatureType().getName().getNamespaceURI()
                        );

                if (ns != null){
                    FeatureTypeInfo info = cat.getResourceByName(
                            ns.getPrefix(),
                            feature.getFeatureType().getName().getLocalPart(),
                            FeatureTypeInfo.class
                            );

                    if (info != null){
                        map.put("type", info);
                    }
                }
            }

            if (map.get("type") == null){
                map.put("type", buildDummyFeatureTypeInfo(feature));
            }

            // Add the metadata after setting the attributes so they aren't masked by feature attributes
            map.put("fid", feature.getID());
            map.put("typeName", feature.getFeatureType().getTypeName());

            // create a variable "attributes" which his a list of all the
            // attributes, but at the same time, is a map keyed by name
            map.put("attributes", new SequenceMapModel(attributeMap, this));

            return map;
        }

        return super.wrap(object);
    }

    private Map<String, Object> buildDummyFeatureTypeInfo(SimpleFeature f){
        Map<String, Object> dummy = new HashMap<String, Object>();
        dummy.put("name", f.getFeatureType().getTypeName());
        dummy.put("title", "Layer: " + f.getFeatureType().getTypeName());
        dummy.put("abstract", "[No Abstract Provided]");
        dummy.put("description", "[No Description Provided]");
        dummy.put("keywords", new ArrayList<String>());
        dummy.put("metadataLinks", new ArrayList<String>());
        dummy.put("SRS", "[SRS]");
        final GeometryDescriptor gd = f.getFeatureType().getGeometryDescriptor();
        if(gd != null)
            dummy.put("nativeCRS", gd.getCoordinateReferenceSystem());
        return dummy;
    }

    /**
     * Adapts a Feature to a java.util.Map, where the map keys are the feature
     * attribute names and the values other Map representing the Feature
     * name/value attributes.
     * <p>
     * A special purpose Map implementation is used in order to lazily return
     * the attribute properties, most notably the toString representation of
     * attribute values.
     * </p>
     * 
     * @author Gabriel Roldan
     * @see AttributeMap
     */
    private static class FeatureAttributesMap extends AbstractMap {
        private Set entrySet;

        private SimpleFeature feature;

        public FeatureAttributesMap(SimpleFeature feature) {
            this.feature = feature;
        }

        public Set entrySet() {
            if (entrySet == null) {
                entrySet = new LinkedHashSet();
                final List<AttributeDescriptor> types = feature.getFeatureType().getAttributeDescriptors();
                final int attributeCount = types.size();
                String attName;
                Map attributesMap;
                for (int i = 0; i < attributeCount; i++) {
                    attName = types.get(i).getLocalName();
                    attributesMap = new AttributeMap(attName, feature);
                    entrySet.add(new MapEntry(attName, attributesMap));
                }
            }
            return entrySet;
        }
    }

    /**
     * Wraps a Feature as a
     * <code>Map&lt;String, Map&lt;String, Object&gt;&gt;</code>.
     * <p>
     * The Map keys are the wrapped feature's property names and the Map values
     * are Maps with appropriate key/value pairs for each feature attribute.
     * </p>
     * <p>
     * For instance, the value attribute Maps hold the following properties:
     * <ul>
     * <li>name: String holding the attribute name</li>
     * <li>type: String with the java class name bound to the attribute type</li>
     * <li>value: String representation of the attribute value suitable to be
     * used directly in a template expression. <code>null</code> values are
     * returned as the empty string, non String values as per
     * {@link FeatureWrapper#valueToString(Object)}</li>
     * <li>rawValue: the actual attribute value as it is in the Feature</li>
     * <li>isGeometry: Boolean indicating whether the attribute is of a
     * geometric type</li>
     * </ul>
     * </p>
     * 
     */
    private static class AttributeMap extends AbstractMap {

        private final String attributeName;

        private final SimpleFeature feature;

        private Set entrySet;

        /**
         * Builds an "attribute map" as used in templates for the given
         * attribute of the given feature.
         * 
         * @param attributeName
         *            the name of the feature attribute this attribute map is
         *            built for
         * @param feature
         *            the feature where to lazily grab the attribute named
         *            <code>attributeName</code> from
         */
        public AttributeMap(final String attributeName, final SimpleFeature feature) {
            this.attributeName = attributeName;
            this.feature = feature;
        }

        /**
         * Override so asking for the hashCode does not implies traversing the
         * whole map and thus calling entrySet() prematurely
         */
        public int hashCode() {
            return attributeName.hashCode();
        }

        /**
         * Returns this map's entry set. An entry for each of the properties
         * mentioned in this class's javadoc is returned. Of special interest is
         * the entry for the <code>"value"</code> property, which is lazily
         * evaluated through the use of a {@link DeferredValueEntry}
         */
        public Set entrySet() {
            if (entrySet == null) {
                entrySet = new LinkedHashSet();
                final SimpleFeatureType featureType = feature.getFeatureType();
                final AttributeDescriptor attributeType = featureType.getDescriptor(attributeName);
                final Object value = feature.getAttribute(attributeName);

                entrySet.add(new DeferredValueEntry("value", value));
                entrySet.add(new MapEntry("name", attributeName));
                entrySet.add(new MapEntry("type", attributeType.getType().getBinding().getName()));

                Object rawValue = value == null ? "" : value;
                boolean isGeometry = Geometry.class.isAssignableFrom(attributeType.getType().getBinding());
                entrySet.add(new MapEntry("isGeometry", Boolean.valueOf(isGeometry)));
                entrySet.add(new MapEntry("rawValue", rawValue));
            }
            return entrySet;
        }

        /**
         * A special purpose Map.Entry whose value is transformed to String on
         * demand, thus avoiding to hold both the actual value object and its
         * string value.
         * 
         * @see FeatureWrapper#valueToString(Object)
         */
        private static class DeferredValueEntry extends MapEntry {
            private static final long serialVersionUID = -3919798947862996744L;

            public DeferredValueEntry(String key, Object attribute) {
                super(key, attribute);
            }

            /**
             * Returns the value corresponding to this entry, as a String.
             */
            public Object getValue() {
                Object actualValue = super.getValue();
                String stringValue = FeatureWrapper.valueToString(actualValue);
                return stringValue;
            }
        }
    }
}
