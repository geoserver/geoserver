/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.checkerframework.checker.units.qual.K;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.identity.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;

/** GeoJSON writer capable of handling complex features. */
class ComplexGeoJsonWriter {

    static final Logger LOGGER = Logging.getLogger(ComplexGeoJsonWriter.class);

    private static Class NON_FEATURE_TYPE_PROXY;
    private static final String DATATYPE = "@dataType";
    /**
     * A string constant for representing a not needed key name because object is being added inside
     * an already named json array
     */
    private static final String INSIDE_ARRAY_ATTRIBUTE = "${inside-array}";

    static {
        try {
            NON_FEATURE_TYPE_PROXY =
                    Class.forName("org.geotools.data.complex.config.NonFeatureTypeProxy");
        } catch (ClassNotFoundException e) {
            // might be ok if the app-schema datastore is not around
            if (StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                    DataStoreFinder.getAllDataStores(), Spliterator.ORDERED),
                            false)
                    .anyMatch(
                            f ->
                                    f != null
                                            && f.getClass()
                                                    .getSimpleName()
                                                    .equals("AppSchemaDataAccessFactory"))) {
                LOGGER.log(
                        Level.FINE,
                        "Could not find NonFeatureTypeProxy yet App-schema is around, probably the class changed name, package or does not exist anymore",
                        e);
            }
            NON_FEATURE_TYPE_PROXY = null;
        }
    }

    private final GeoJSONBuilder jsonWriter;

    private boolean geometryFound = false;
    private CoordinateReferenceSystem crs;
    private long featuresCount = 0;

    public ComplexGeoJsonWriter(GeoJSONBuilder jsonWriter) {
        this.jsonWriter = jsonWriter;
    }

    public void write(List<FeatureCollection> collections) {
        for (FeatureCollection collection : collections) {
            // encode the feature collection making sure that the collection is closed
            try (FeatureIterator iterator = collection.features()) {
                encodeFeatureCollection(iterator);
            }
        }
    }

    /** Encode all available features by iterating over the iterator. */
    private void encodeFeatureCollection(FeatureIterator iterator) {
        while (iterator.hasNext()) {
            // encode the next feature
            encodeFeature(iterator.next(), true);
            featuresCount++;
        }
    }

    /** Encode a feature in GeoJSON. */
    protected void encodeFeature(Feature feature, boolean topLevelFeature) {
        // start the feature JSON object
        jsonWriter.object();
        jsonWriter.key("type").value("Feature");
        // encode the feature identifier if available
        Identifier identifier = feature.getIdentifier();
        if (identifier != null) {
            jsonWriter.key("id").value(identifier.getID());
        }
        // geometry attribute has some special handling
        Property geometryAttribute = encodeGeometry(feature);
        // start the JSON object that will contain all the others properties
        jsonWriter.key("properties");
        jsonWriter.object();
        jsonWriter.key("@featureType").value(getSimplifiedTypeName(feature.getType().getName()));
        // encode object properties, we pass the geometry attribute to avoid duplicate encodings
        encodeProperties(geometryAttribute, feature.getType(), feature.getProperties());
        // close the properties JSON object
        jsonWriter.endObject();
        writeExtraFeatureProperties(feature, topLevelFeature);
        // close the feature JSON object
        jsonWriter.endObject();
    }

    /**
     * Allows subclasses to write extra attributes after the "properties" section end. By default it
     * does nothing.
     *
     * @param feature The feature being encoded
     * @param topLevelfeature If the feature being encoded is top level in the GeoJSON output, or
     *     nested inside another feature instead
     */
    protected void writeExtraFeatureProperties(Feature feature, boolean topLevelfeature) {}

    /**
     * Returns the simplified type name, e.g., if the name is BoreCollarType the method will return
     * "BoreCollar" (to remove yet another GML convention)
     */
    private String getSimplifiedTypeName(Name name) {
        String localName = name.getLocalPart();
        if (localName.endsWith("_Type")) {
            return localName.substring(0, localName.length() - "_Type".length());
        }
        if (localName.endsWith("Type")) {
            return localName.substring(0, localName.length() - "Type".length());
        }
        return localName;
    }

    /**
     * Encode feature geometry attribute which may not exist or be NULL. Returns the geometry
     * attribute name for the provided feature, NULL will be returned if the provided feature has no
     * geometry attribute.
     */
    private Property encodeGeometry(Feature feature) {
        // get feature geometry attribute description
        GeometryDescriptor geometryType = feature.getType().getGeometryDescriptor();
        Property geometryAttribute = null;
        Geometry geometry = null;
        if (geometryType != null) {
            // extract CRS information from the geometry attribute description
            CoordinateReferenceSystem crs = geometryType.getCoordinateReferenceSystem();
            // we let the setAxisOrder method handle the NULL case
            jsonWriter.setAxisOrder(CRS.getAxisOrder(crs));
            if (crs != null) {
                // store the found CRS, this may be useful for the invoker
                this.crs = crs;
            }
            // store the attribute name and geometry value of the current feature
            geometryAttribute = feature.getProperty(geometryType.getName());
            geometry = geometryAttribute != null ? (Geometry) geometryAttribute.getValue() : null;
        } else {
            // this feature seems to not have a geometry, write the default axis order
            jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
        }
        // start the JSON geometry object
        jsonWriter.key("geometry");
        if (geometry != null) {
            // the feature has a geometry so encode it
            jsonWriter.writeGeom(geometry);
            // store that we found a geometry, this may be useful for the invoker
            geometryFound = true;
        } else {
            // no geometry just write a NULL value
            jsonWriter.value(null);
        }
        // return the found geometry attribute, may be NULL
        return geometryAttribute;
    }

    /** Encode a feature properties. Geometry attribute will be ignored. */
    private void encodeProperties(
            Property geometryAttribute, PropertyType parentType, Collection<Property> properties) {
        // index all the feature available properties by their type
        Map<PropertyDescriptor, List<Property>> index =
                indexPropertiesByDescriptor(geometryAttribute, properties);
        for (Map.Entry<PropertyDescriptor, List<Property>> entry : index.entrySet()) {
            // encode properties per type
            encodePropertiesByType(parentType, entry.getKey(), entry.getValue());
        }
    }

    /** Index the provided properties by their type, geometry property will be ignored. */
    private Map<PropertyDescriptor, List<Property>> indexPropertiesByDescriptor(
            Property geometryAttribute, Collection<Property> properties) {
        Map<PropertyDescriptor, List<Property>> index = new LinkedHashMap<>();
        for (Property property : properties) {
            if (geometryAttribute != null && geometryAttribute.equals(property)) {
                // ignore the geometry attribute that should have been encoded already
                continue;
            }
            // update the index with the current property
            List<Property> propertiesWithSameDescriptor = index.get(property.getDescriptor());
            if (propertiesWithSameDescriptor == null) {
                // first time we see a property fo this type
                propertiesWithSameDescriptor = new ArrayList<>();
                index.put(property.getDescriptor(), propertiesWithSameDescriptor);
            }
            propertiesWithSameDescriptor.add(property);
        }
        return index;
    }

    /**
     * Encode feature properties by type, we do this way so we can handle the case were these
     * properties should be encoded as a list or as elements that appear multiple times.
     */
    private void encodePropertiesByType(
            PropertyType parentType, PropertyDescriptor descriptor, List<Property> properties) {
        // possible chained features that need to be encoded as a list
        List<Feature> chainedFeatures = getChainedFeatures(properties);
        if (chainedFeatures == null
                || chainedFeatures.isEmpty()
                || descriptor.getMaxOccurs() == 1) {
            // let's check if we are in the presence of linked features
            List<Map<NameImpl, String>> linkedFeatures = getLinkedFeatures(properties);
            if (!linkedFeatures.isEmpty()) {
                // encode linked features
                encodeLinkedFeatures(descriptor, linkedFeatures);
            } else {
                // encode properties
                encodeProperties(descriptor, properties);
            }
        } else {
            // chained features so we need to encode the chained features as an array
            encodeChainedFeatures(descriptor.getName().getLocalPart(), chainedFeatures);
        }
    }

    /**
     * Encodes the properties and select if it is a single attribute or a json array is needed.
     *
     * @param descriptor the attribute descriptor
     * @param properties the properties to be encoded
     */
    private void encodeProperties(PropertyDescriptor descriptor, List<Property> properties) {
        // no chained or linked features just encode each property
        String attributeName = descriptor.getName().getLocalPart();
        if (properties.size() > 1
                && areAllPropertiesAttributeNameEquals(properties, attributeName)) {
            encodeArray(properties, attributeName);
        } else {
            properties.forEach(this::encodeProperty);
        }
    }

    /**
     * Encodes a JSON array with provided properties using the attribute name as key.
     *
     * @param properties the properties to be encoded inside the array
     * @param attributeName the attribute name to be used as key name for the array
     */
    private void encodeArray(List<Property> properties, String attributeName) {
        jsonWriter.key(attributeName).array();
        properties.forEach(
                prop -> encodeProperty(INSIDE_ARRAY_ATTRIBUTE, prop, getAttributes(prop)));
        jsonWriter.endArray();
    }

    /**
     * Checks if all properties names are the same as provided attribute name.
     *
     * @param properties properties to check
     * @param attributeName attribute name
     * @return true if all properties attribute name are equals to provided one
     */
    private boolean areAllPropertiesAttributeNameEquals(
            List<Property> properties, String attributeName) {
        return properties
                .stream()
                .allMatch(prop -> Objects.equals(attributeName, prop.getName().getLocalPart()));
    }

    /** Encodes linked features as a JSON array. */
    private void encodeLinkedFeatures(
            PropertyDescriptor descriptor, List<Map<NameImpl, String>> linkedFeatures) {
        // start the JSON object
        jsonWriter.key(descriptor.getName().getLocalPart());
        // is it multiple or single?
        if (descriptor.getMaxOccurs() > 1) {
            jsonWriter.array();
        }
        // encode each linked feature
        for (Map<NameImpl, String> feature : linkedFeatures) {
            encodeAttributesAsObject(feature);
        }
        if (descriptor.getMaxOccurs() > 1) {
            // end the linked features JSON array
            jsonWriter.endArray();
        }
    }

    /** Encodes a list of features (chained features) as a JSON array. */
    private void encodeChainedFeatures(String attributeName, List<Feature> chainedFeatures) {
        // start the JSON object
        // print the key name if it is not inside an array
        key(attributeName);
        if (!isInsideArrayAttributeName(attributeName)) {
            // start the json array only if it is not inside one already
            jsonWriter.array();
        }
        for (Feature feature : chainedFeatures) {
            // if it's GeoJSON compatible, encode as a full blown GeoJSON feature (must have a
            // default geometry)
            if (feature.getType().getGeometryDescriptor() != null) {
                encodeFeature(feature, false);
            } else {
                jsonWriter.object();
                encodeProperties(null, feature.getType(), feature.getProperties());
                jsonWriter.endObject();
            }
        }
        // end the JSON chained features array
        if (!isInsideArrayAttributeName(attributeName)) {
            // end the json array only if it is not inside one already
            jsonWriter.endArray();
        }
    }

    /**
     * Checks if the provided attribute name represents an already started key name and current
     * object is inside an array already.
     *
     * @param attributeName the attribute name to check
     * @return true if it is inside an array
     */
    private boolean isInsideArrayAttributeName(String attributeName) {
        return INSIDE_ARRAY_ATTRIBUTE.equals(attributeName);
    }

    /**
     * Get a list of chained features, NULL will be returned if this properties are not chained
     * features.
     */
    private List<Feature> getChainedFeatures(List<Property> properties) {
        List<Feature> features = new ArrayList<>();
        for (Property property : properties) {
            if (!(property instanceof ComplexAttribute)) {
                // only the chaining of complex features is supported
                return null;
            }
            ComplexAttribute complexProperty = (ComplexAttribute) property;
            Collection<Property> subProperties = complexProperty.getProperties();
            if (subProperties.size() > 1) {
                // more than one property means that this are not chained features
                return null;
            }
            Property subProperty = getElementAt(subProperties, 0);
            if (!(subProperty instanceof Feature)) {
                // if the only property is not a feature this are no chained features
                return null;
            }
            features.add((Feature) subProperty);
        }
        // this are chained features
        return features;
    }

    /** Extracts from the provided properties any chained features resolved as links. */
    @SuppressWarnings("unchecked")
    private List<Map<NameImpl, String>> getLinkedFeatures(List<Property> properties) {
        List<Map<NameImpl, String>> linkedFeatures = new ArrayList<>();
        for (Property property : properties) {
            // get the attributes (XML attributes) associated with the current property
            Map<NameImpl, String> attributes =
                    (Map<NameImpl, String>) property.getUserData().get(Attributes.class);
            if (checkIfFeatureIsLinked(property, attributes)) {
                // we have a linked features
                linkedFeatures.add(attributes);
            }
        }
        return linkedFeatures;
    }

    /**
     * Helper method that returns TRUE if the provided complex property corresponds to a chained
     * feature resolved as a link.
     */
    @SuppressWarnings("unchecked")
    static boolean checkIfFeatureIsLinked(Property property, Map<NameImpl, String> attributes) {
        if (!(property instanceof ComplexAttribute)) {
            // not a complex attribute, so we don't consider it a candidate to be a linked one
            return false;
        }
        ComplexAttribute complexProperty = (ComplexAttribute) property;
        if (complexProperty.getProperties() != null && !complexProperty.getProperties().isEmpty()) {
            // has properties, so not a chained feature resolved as a link
            return false;
        }
        if (attributes != null) {
            for (NameImpl key : attributes.keySet()) {
                if (key != null && "href".equalsIgnoreCase(key.getLocalPart())) {
                    // we found a link
                    return true;
                }
            }
        }
        // no link was found, so not a chained feature resolved as a link
        return false;
    }

    /** Helper method that just gets an element from a collection at a certain index. */
    private <T> T getElementAt(Collection<T> collection, int index) {
        Iterator<T> iterator = collection.iterator();
        T element = null;
        for (int i = 0; i <= index && iterator.hasNext(); i++) {
            element = iterator.next();
        }
        return element;
    }

    /**
     * Encode a feature property, we only support complex attributes and simple attributes, if
     * another type of attribute is used an exception will be throw.
     */
    private void encodeProperty(Property property) {
        // these extra attributes should be seen as XML attributes
        Map<NameImpl, Object> attributes = getAttributes(property);
        String attributeName = property.getName().getLocalPart();
        encodeProperty(attributeName, property, attributes);
    }

    /**
     * Returns a map of attributes inside the provided property.
     *
     * @param property property to check
     * @return a map of attributes
     */
    @SuppressWarnings("unchecked")
    private Map<NameImpl, Object> getAttributes(Property property) {
        Map<NameImpl, Object> attributes =
                (Map<NameImpl, Object>) property.getUserData().get(Attributes.class);
        return attributes != null ? attributes : Collections.emptyMap();
    }

    private void encodeProperty(
            String attributeName, Property property, Map<NameImpl, Object> attributes) {
        if (property instanceof ComplexAttribute) {
            // check if we have a simple content
            ComplexAttribute complexAttribute = (ComplexAttribute) property;

            if (isSimpleContent(complexAttribute.getType())) {
                Object value = getSimpleContentValue(complexAttribute);
                if (value != null || (attributes != null && !attributes.isEmpty())) {
                    encodeSimpleAttribute(attributeName, value, attributes);
                }
            } else {
                // skip the property/element nesting found in GML, if possible
                if (isGMLPropertyType(complexAttribute)) {
                    Collection<? extends Property> value = complexAttribute.getValue();
                    Property nested = value.iterator().next();
                    Map<NameImpl, Object> nestedAttributes =
                            (Map<NameImpl, Object>) nested.getUserData().get(Attributes.class);
                    Map<NameImpl, Object> mergedAttributes =
                            mergeMaps(attributes, nestedAttributes);
                    encodeProperty(attributeName, nested, mergedAttributes);
                } else {
                    // we need to encode a normal complex attribute
                    encodeComplexAttribute(attributeName, complexAttribute, attributes);
                }
            }
        } else if (property instanceof Attribute) {
            // check if we have a feature or list of features (chained features)
            List<Feature> features = getFeatures((Attribute) property);
            if (features != null) {
                encodeChainedFeatures(attributeName, features);
            } else {
                // we need to encode a simple attribute
                encodeSimpleAttribute(attributeName, property.getValue(), attributes);
            }
        } else {
            // unsupported attribute type provided, this will unlikely happen
            throw new RuntimeException(
                    String.format(
                            "Invalid property '%s' of type '%s', only 'Attribute' and 'ComplexAttribute' properties types are supported.",
                            property.getName(), property.getClass().getCanonicalName()));
        }
    }

    private <K, V> Map<K, V> mergeMaps(Map<K, V> mapA, Map<K, V> mapB) {
        if (mapA == null) {
            return mapB;
        } else if (mapB == null) {
            return mapA;
        }

        Map<K, V> merged = new HashMap<>(mapA);
        merged.putAll(mapB);
        return merged;
    }

    /**
     * This code tries to determine if the current complex attribute is an example of GML
     * property/type alternation. The GML gives us pretty much no firm indication to recognize them,
     * there is no substitution group or inheritance, there are attribute groups sometimes found in
     * these constructs, but not mandatory and not always present at the schema level.
     *
     * <p>This code works by recognizing the common alternation nomenclature, that is:
     *
     * <ul>
     *   <li>The attribute type is called ${name}PropertyType
     *   <li>It contains a single element inside, which is in turn another complex attribute itself
     *   <li>The contained element type is called ${name}Type or the property is called ${name}
     * </ul>
     *
     * Can I just say.... HACK HACK HACK!
     */
    private boolean isGMLPropertyType(ComplexAttribute complexAttribute) {
        String attributeName = complexAttribute.getType().getName().getLocalPart();
        if (!attributeName.endsWith("PropertyType")) {
            return false;
        }
        Collection<? extends Property> value = complexAttribute.getValue();
        if (value.size() != 1) {
            return false;
        }
        Property containedProperty = value.iterator().next();
        String containedPropertyTypeName = containedProperty.getType().getName().getLocalPart();
        String containedPropertyName = containedProperty.getName().getLocalPart();
        String propertyTypePrefix;
        if (attributeName.endsWith("_PropertyType")) {
            propertyTypePrefix =
                    attributeName.substring(0, attributeName.length() - "_PropertyType".length());
        } else {
            propertyTypePrefix =
                    attributeName.substring(0, attributeName.length() - "PropertyType".length());
        }
        return containedPropertyTypeName.equals(propertyTypePrefix + "Type")
                || containedPropertyName.equals(propertyTypePrefix);
    }

    /**
     * Helper method that try to extract a list of features from an attribute. If no features can be
     * found NULL is returned.
     */
    private List<Feature> getFeatures(Attribute attribute) {
        Object value = attribute.getValue();
        if (value instanceof Feature) {
            // feature found return it in a single ton list
            return Collections.singletonList((Feature) value);
        }
        if (!(value instanceof Collection)) {
            // not a feature or list of features
            return null;
        }
        Collection collection = (Collection) value;
        if (collection.isEmpty()) {
            // we cannot be sure that this is a list of features
            return Collections.emptyList();
        }
        if (!(getElementAt(collection, 0) instanceof Feature)) {
            // list doesn't contain features
            return null;
        }
        // make sure we have only features
        List<Feature> features = new ArrayList<>();
        for (Object object : collection) {
            if (!(object instanceof Feature)) {
                // not a feature this is a mixed collection
                throw new RuntimeException(
                        String.format("Unable to handle attribute '%s'.", attribute));
            }
            features.add((Feature) object);
        }
        return features;
    }

    /**
     * Helper method that try to extract a simple content from a complex attribute, NULL is returned
     * if no simple content is present.
     */
    private boolean isSimpleContent(AttributeType type) {
        if ("http://www.w3.org/2001/XMLSchema".equals(type.getName().getNamespaceURI())
                && type.getName().getLocalPart().equals("anySimpleType")) {
            return true;
        }

        AttributeType superType = type.getSuper();
        if (superType == null) {
            return false;
        }
        return isSimpleContent(superType);
    }

    /**
     * Helper method that try to extract a simple content from a complex attribute, NULL is returned
     * if no simple content is present.
     */
    private Object getSimpleContentValue(ComplexAttribute property) {
        Collection<Property> properties = property.getProperties();
        if (properties.isEmpty() || properties.size() > 1) {
            // no properties or more than property mean no simple content
            return null;
        }
        Property simpleContent = getElementAt(properties, 0);
        if (simpleContent == null) {
            // simple content is NULL end extraction here
            return null;
        }
        Name name = simpleContent.getName();
        if (name == null || !name.getLocalPart().equals("simpleContent")) {
            // not a simple content node
            return null;
        }
        return simpleContent.getValue();
    }

    /** Encode a complex attribute as a JSON object. */
    private void encodeComplexAttribute(
            String name, ComplexAttribute attribute, Map<NameImpl, Object> attributes) {
        if (isFullFeature(attribute)) {
            key(name);
            encodeFeature((Feature) attribute, false);
        } else {
            // get the attribute name and start a JSON object
            key(name);
            jsonWriter.object();
            // encode the datatype
            jsonWriter.key(DATATYPE);
            jsonWriter.value(getSimplifiedTypeName(attribute.getType().getName()));
            // let's see if we have actually some properties to encode
            if (attribute.getProperties() != null && !attribute.getProperties().isEmpty()) {
                // encode the object properties, since this is not a top feature or a
                // chained feature we don't need to explicitly handle the geometry attribute
                encodeProperties(null, attribute.getType(), attribute.getProperties());
            }
            if (attributes != null && !attributes.isEmpty()) {
                // encode the attributes list
                encodeAttributes(attributes);
            }
            jsonWriter.endObject();
        }
    }

    /**
     * Checks if an attribute is an actual feature, skipping the NonFeatureTypeProxy case app-schema
     * is using for technical reasons
     */
    private boolean isFullFeature(ComplexAttribute attribute) {
        return attribute instanceof Feature
                && (NON_FEATURE_TYPE_PROXY == null
                        || !NON_FEATURE_TYPE_PROXY.isInstance(attribute.getType()));
    }

    /**
     * Encode a simple attribute, this means that this property will be encoded as a simple JSON
     * attribute if no attributes are available, otherwise it will be encoded as an array containing
     * the value and attributes values.
     */
    private void encodeSimpleAttribute(
            String name, Object value, Map<NameImpl, Object> attributes) {
        // let's see if we need to encode attributes or simple value
        if (attributes == null || attributes.isEmpty()) {
            // add a simple JSON attribute to the current object
            key(name);
            jsonWriter.value(value);
            return;
        }
        // we need to encode a list of attributes, let's first encode the main value
        key(name);
        jsonWriter.object();
        if (value != null) {
            jsonWriter.key("value").value(value);
        }
        // encode the attributes list
        encodeAttributes(attributes);
        // close the values \ attributes object
        jsonWriter.endObject();
    }

    /**
     * Start a json attribute name only if provided name does not represent that current object is
     * already inside on a json array.
     *
     * @param name provided attribute name or the constant representing this is inside an array
     */
    private void key(String name) {
        if (!isInsideArrayAttributeName(name)) {
            jsonWriter.key(name);
        }
    }

    /**
     * Utility method that encode an attributes map as a set of properties in an object The
     * attribute name local part will be used as the property name. Attributes with a NULL value
     * will not be encoded. This method assumes that it is already in an object context.
     */
    private void encodeAttributes(Map<NameImpl, Object> attributes) {
        attributes.forEach(
                (name, value) -> {
                    if (value != null) {
                        // encode attribute, we don't take namespace into account
                        jsonWriter.key("@" + name.getLocalPart()).value(value);
                    }
                });
    }

    /**
     * Utility method that encode an attributes map as properties of an object, each one using the
     * attribute name local part and value . Attributes with a NULL value will not be encoded.
     */
    private void encodeAttributesAsObject(Map<NameImpl, String> attributes) {
        jsonWriter.object();
        attributes.forEach(
                (name, value) -> {
                    if (value != null) {
                        // encode attribute, we don't take namespace into account
                        jsonWriter.key("@" + name.getLocalPart()).value(value);
                    }
                });
        jsonWriter.endObject();
    }

    /** Return TRUE if a geometry was found during the features collections encoding. */
    public boolean geometryFound() {
        return geometryFound;
    }

    /** Return a CRS if one was found during the features collections encoding. */
    public CoordinateReferenceSystem foundCrs() {
        return crs;
    }

    /** Return the number of top encoded features. */
    public long getFeaturesCount() {
        return featuresCount;
    }
}
