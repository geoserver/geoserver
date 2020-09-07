/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.complex;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Converts a complex feature type to a simple one based on the provided transformation rules map.
 */
class FeatureTypeConverter {

    private static final Logger LOGGER = Logging.getLogger(FeatureTypeConverter.class);

    private final FeatureType featureType;
    private final Map<String, String> rules;
    private final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    private final NamespaceSupport namespaceSupport;

    private List<AttributeDescriptor> processedDescriptors = new ArrayList<>();

    public FeatureTypeConverter(
            FeatureType featureType, Map<String, String> rules, NamespaceSupport namespaceSupport) {
        this.featureType = requireNonNull(featureType);
        this.rules = requireNonNull(rules);
        this.namespaceSupport = requireNonNull(namespaceSupport);
    }

    /**
     * Builds the resulting simple feature type based on the rules map.
     *
     * @return the simple feature type
     */
    public SimpleFeatureType produceSimpleType() {
        // convert the type name
        builder.setNamespaceURI(featureType.getName().getNamespaceURI());
        builder.setName(convertTypeName(featureType));
        // process the attributes
        processAllAttributes();
        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        LOGGER.info(() -> "Converted simple feature type: " + simpleFeatureType);
        return simpleFeatureType;
    }

    /**
     * Process all the attributes to find simple attributes by convention and complex attributes by
     * rules.
     */
    private void processAllAttributes() {
        processRules();
        Collection<PropertyDescriptor> descriptors = featureType.getDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            processDescriptor(descriptor);
        }
    }

    /** Processes the transformation rules and builds the resulting simple attributes. */
    private void processRules() {
        for (Entry<String, String> rule : rules.entrySet()) {
            processRule(rule);
        }
    }

    /** Processes the transformation rule and builds the resulting simple attribute. */
    private void processRule(Entry<String, String> rule) {
        // get the descriptor
        Optional<AttributeDescriptor> descriptorOpt = getDescriptor(rule.getValue());
        if (!descriptorOpt.isPresent()) {
            LOGGER.warning(
                    () ->
                            "Descriptor for attribute: '"
                                    + rule.getValue()
                                    + "' not found on feature type: "
                                    + featureType);
            return;
        }
        AttributeDescriptor descriptor = descriptorOpt.get();
        LOGGER.finer(() -> "Descriptor found: " + descriptor);
        // add descriptor to new feature type
        builder.add(rule.getKey(), descriptor.getType().getBinding());
        // add descriptor to processed list
        processedDescriptors.add(descriptor);
    }

    /**
     * Checks if the provided descriptor belongs to a simple attribute. If it accomplish this rule,
     * the descriptor is added to the new feature type.
     *
     * @return true if a simple attribute is found and was added
     */
    private boolean processDescriptor(PropertyDescriptor descriptor) {
        // check if current descriptor was already processed
        if (processedDescriptors.contains(descriptor)) return false;
        // if it is a geometry descriptor, pass the original
        if (descriptor instanceof GeometryDescriptor) {
            processGeometryDescriptor((GeometryDescriptor) descriptor);
            return true;
        } else if (descriptor instanceof AttributeDescriptor) {
            return processAttributeDescriptor((AttributeDescriptor) descriptor);
        }
        return false;
    }

    private void processGeometryDescriptor(GeometryDescriptor descriptor) {
        // add the descriptor directly
        builder.add(descriptor);
    }

    private boolean processAttributeDescriptor(AttributeDescriptor descriptor) {
        if (!isSimpleType(descriptor)) return false;
        String name = descriptor.getName().getLocalPart();
        Class<?> binding = descriptor.getType().getSuper().getBinding();
        builder.add(name, binding);
        return true;
    }

    private boolean isSimpleType(AttributeDescriptor descriptor) {
        if (isBlackListed(descriptor)) return false;
        // if multiple value, return false
        if (descriptor.getMaxOccurs() > 1) return false;
        // check the inner type names
        AttributeType attributeType = descriptor.getType();
        if (isSimple(attributeType)) return true;
        while (attributeType.getSuper() != null) {
            attributeType = attributeType.getSuper();
            // only return null if is a simple attribute and not blacklisted
            if (isSimple(attributeType)) return true;
        }
        return false;
    }

    private boolean isSimple(AttributeType attributeType) {
        Name name = attributeType.getName();
        return "http://www.w3.org/2001/XMLSchema".equals(name.getNamespaceURI())
                && "anySimpleType".equals(name.getLocalPart());
    }

    /** Detects if the attribute is not allowed to be on the resulting feature type. */
    private boolean isBlackListed(AttributeDescriptor descriptor) {
        // if namespace is null return true
        if (descriptor.getName().getNamespaceURI() == null) return true;
        // if namespace is GML return true
        if (descriptor.getName().getNamespaceURI().startsWith("http://www.opengis.net/gml"))
            return true;
        return false;
    }

    /** Adds the suffix 'SimpleType' to the type name and returns it. */
    private String convertTypeName(FeatureType featureType) {
        String typeName = featureType.getName().getLocalPart();
        if (typeName.contains("Type")) {
            return typeName.replace("Type", "SimpleType");
        }
        return typeName + "SimpleType";
    }

    /**
     * Resolves the Complex feature type attribute descriptor for the provided xpath expression.
     *
     * @param xpath the attribute expression
     * @return an optional attribute descriptor, empty if not found
     */
    protected Optional<AttributeDescriptor> getDescriptor(String xpath) {
        AttributeExpressionImpl attr = new AttributeExpressionImpl(xpath, namespaceSupport);
        return (Optional.ofNullable(attr.evaluate(featureType, AttributeDescriptor.class)));
    }
}
