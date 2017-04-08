/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.identity.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON writer capable of handling complex features.
 */
public final class ComplexGeoJsonWriter {

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

    /**
     * Encode all available features by iterating over the iterator.
     */
    private void encodeFeatureCollection(FeatureIterator iterator) {
        while (iterator.hasNext()) {
            // encode the next feature
            encodeFeature(iterator.next());
            featuresCount++;
        }
    }

    /**
     * Encode a feature in GeoJSON.
     */
    private void encodeFeature(Feature feature) {
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
        // encode object properties, we pass the geometry attribute to avoid duplicate encodings
        encodeProperties(geometryAttribute, feature.getType(), feature.getProperties());
        // close the feature JSON object
        jsonWriter.endObject();
        // close the properties JSON object
        jsonWriter.endObject();
    }

    /**
     * Encode feature geometry attribute which may not exist or be NULL.
     * Returns the geometry attribute name for the provided feature, NULL
     * will be returned if the provided feature has no geometry attribute.
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
            geometry = (Geometry) geometryAttribute.getValue();
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

    /**
     * Encode a feature properties. Geometry attribute will be ignored.
     */
    private void encodeProperties(Property geometryAttribute, PropertyType parentType, Collection<Property> properties) {
        // index all the feature available properties by their type
        Map<PropertyType, List<Property>> index = indexPropertiesByType(geometryAttribute, properties);
        for (Map.Entry<PropertyType, List<Property>> entry : index.entrySet()) {
            // encode properties per type
            encodePropertiesByType(parentType, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Index the provided properties by their type, geometry property
     * will be ignored.
     */
    private Map<PropertyType, List<Property>> indexPropertiesByType(Property geometryAttribute, Collection<Property> properties) {
        Map<PropertyType, List<Property>> index = new HashMap<>();
        for (Property property : properties) {
            if (geometryAttribute != null && property.equals(geometryAttribute)) {
                // ignore the geometry attribute that should have been encoded already
                continue;
            }
            // update the index with the current property
            List<Property> propertiesWithSameType = index.get(property.getType());
            if (propertiesWithSameType == null) {
                // first time we see a property fo this type
                propertiesWithSameType = new ArrayList<>();
                index.put(property.getType(), propertiesWithSameType);
            }
            propertiesWithSameType.add(property);
        }
        return index;
    }

    /**
     * Encode feature properties by type, we do this way so we can handle the case were
     * these properties should be encoded as a list or as elements that appear multiple
     * times.
     */
    private void encodePropertiesByType(PropertyType parentType, PropertyType type, List<Property> properties) {
        PropertyDescriptor multipleType = isMultipleType(parentType, type);
        if (multipleType == null) {
            // simple JSON objects
            properties.forEach(this::encodeProperty);
        } else {
            // possible chained features that need to be encoded as a list
            List<Feature> chainedFeatures = getChainedFeatures(properties);
            if (chainedFeatures == null || chainedFeatures.isEmpty()) {
                // no chained features just encode each property
                properties.forEach(this::encodeProperty);
            } else {
                // chained features so we need to encode the chained features as an array
                encodeChainedFeatures(multipleType.getName().getLocalPart(), chainedFeatures);
            }
        }
    }

    /**
     * Encodes a list of features (chained features) as a JSON array.
     */
    private void encodeChainedFeatures(String attributeName, List<Feature> chainedFeatures) {
        // start the JSON object
        jsonWriter.key(attributeName);
        jsonWriter.array();
        for (Feature feature : chainedFeatures) {
            // encode each chained feature
            jsonWriter.object();
            encodeProperties(null, feature.getType(), feature.getProperties());
            jsonWriter.endObject();
        }
        // end the JSON chained features array
        jsonWriter.endArray();
    }

    /**
     * Check if a property type should appear multiple times or be encoded as a list.
     */
    private PropertyDescriptor isMultipleType(PropertyType parentType, PropertyType type) {
        if (!(parentType instanceof ComplexType)) {
            // only properties that belong to a complex type can be chained features
            return null;
        }
        // search the current type on the parent properties
        ComplexType complexType = (ComplexType) parentType;
        PropertyDescriptor foundType = null;
        for (PropertyDescriptor descriptor : complexType.getDescriptors()) {
            if (descriptor.getType().equals(type)) {
                // found our type
                foundType = descriptor;
            }
        }
        // if the found type can appear multiples time is not a chained feature
        if (foundType == null) {
            return null;
        }
        if (foundType.getMaxOccurs() > 1) {
            // this type can appear more than once so it should not be encoded as a list
            return foundType;
        }
        return null;
    }

    /**
     * Get a list of chained features, NULL will be returned if this properties
     * are not chained features.
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

    /**
     * Helper method that just gets an element from a collection at a certain index.
     */
    private <T> T getElementAt(Collection<T> collection, int index) {
        Iterator<T> iterator = collection.iterator();
        T element = null;
        for (int i = 0; i <= index && iterator.hasNext(); i++) {
            element = iterator.next();
        }
        return element;
    }

    /**
     * Encode a feature property, we only support complex attributes and
     * simple attributes, if another tye of attribute is used an exception
     * will be throw.
     */
    private void encodeProperty(Property property) {
        if (property instanceof ComplexAttribute) {
            // check if we have a simple content
            ComplexAttribute complexAttribute = (ComplexAttribute) property;
            Object simpleValue = getSimpleContent(complexAttribute);
            if (simpleValue != null) {
                encodeSimpleAttribute(complexAttribute.getName().getLocalPart(), simpleValue);
            } else {
                // we need to encode a complex attribute
                encodeComplexAttribute((ComplexAttribute) property);
            }
        } else if (property instanceof Attribute) {
            // check if we have a feature or list of features (chained features)
            List<Feature> features = getFeatures((Attribute) property);
            if (features != null) {
                encodeChainedFeatures(property.getName().getLocalPart(), features);
            } else {
                // we need to encode a simple attribute
                encodeSimpleAttribute((Attribute) property);
            }
        } else {
            // unsupported attribute type provided, this will unlikely happen
            throw new RuntimeException(String.format(
                    "Invalid property '%s' of type '%s', only 'Attribute' and 'ComplexAttribute' properties types are supported.",
                    property.getName(), property.getClass().getCanonicalName()));
        }
    }

    /**
     * Helper method that try to extract a list of features from
     * an attribute. If no features can be found NULL is returned.
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
                throw new RuntimeException(String.format(
                        "Unable to handle attribute '%s'.", attribute));
            }
            features.add((Feature) object);
        }
        return features;
    }

    /**
     * Helper method that try to extract a simple content from a complex
     * attribute, NULL is returned if no simple content is present.
     */
    private Object getSimpleContent(ComplexAttribute property) {
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
        Object value = simpleContent.getValue();
        if (value instanceof Number
                || value instanceof String
                || value instanceof Character) {
            // the extract value is a simple Java type
            return value;
        }
        // not a valid simple content type
        return null;
    }

    /**
     * Encode a complex attribute as a JSON object.
     */
    private void encodeComplexAttribute(ComplexAttribute attribute) {
        // get the attribute name and start a JSON object
        String name = attribute.getName().getLocalPart();
        jsonWriter.key(name);
        jsonWriter.object();
        // encode the object properties, since this is not a top feature or a
        // chained feature we don't need to explicitly handle the geometry attribute
        encodeProperties(null, attribute.getType(), attribute.getProperties());
        // end the attribute JSON object
        jsonWriter.endObject();
    }

    /**
     * Encode a simple attribute, this means that this property
     * will be encoded as a simple JSON attribute.
     */
    private void encodeSimpleAttribute(Attribute attribute) {
        String name = attribute.getName().getLocalPart();
        Object value = attribute.getValue();
        encodeSimpleAttribute(name, value);
    }

    /**
     * Encode a simple attribute, this means that this property
     * will be encoded as a simple JSON attribute.
     */
    private void encodeSimpleAttribute(String name, Object value) {
        // add a simple JSON attribute to the current object
        jsonWriter.key(name).value(value);
    }

    /**
     * Return TRUE if a geometry was found during the features collections encoding.
     */
    public boolean geometryFound() {
        return geometryFound;
    }

    /**
     * Return a CRS if one was found during the features collections encoding.
     */
    public CoordinateReferenceSystem foundCrs() {
        return crs;
    }

    /**
     * Return the number of top encoded features.
     */
    public long getFeaturesCount() {
        return featuresCount;
    }
}