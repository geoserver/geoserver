/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.geoserver.catalog.FeatureTypeInfo.JDBC_VIRTUAL_TABLE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ValidationException;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.ExpressionTypeVisitor;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.Converters;

/** Validates a feature type attributes */
class FeatureTypeValidator {

    public void validate(FeatureTypeInfo fti) {
        List<AttributeTypeInfo> attributes = fti.getAttributes();
        if (attributes == null || attributes.isEmpty()) return;

        // only checking simple features
        // the metadata map is not available if the feature type has just been created from REST
        Optional<MetadataMap> metadata = Optional.ofNullable(fti.getMetadata());
        VirtualTable vt =
                metadata.map(m -> m.get(JDBC_VIRTUAL_TABLE, VirtualTable.class)).orElse(null);
        String typeName = fti.getNativeName();
        boolean temporaryVirtualTable = false;
        JDBCDataStore jds = null;
        try {
            DataAccess access = fti.getStore().getDataStore(null);
            if (!(access instanceof DataStore)) return;
            DataStore ds = (DataStore) access;

            if (vt != null && ds instanceof JDBCDataStore) {
                jds = (JDBCDataStore) ds;
                typeName = setupTempVirtualTable(vt, jds);
                temporaryVirtualTable = true;
            }

            SimpleFeatureType ft = ds.getSchema(typeName);
            Map<String, AttributeDescriptor> nativeAttributes =
                    ft.getAttributeDescriptors().stream()
                            .collect(Collectors.toMap(ad -> ad.getLocalName(), ad -> ad));

            // validate each attribute
            Set<String> names = new HashSet<>();
            for (AttributeTypeInfo attribute : attributes) {
                validate(attribute, ft, nativeAttributes);
                // check for duplicate attribute names
                String name = attribute.getName();
                if (names.contains(name)) {
                    throw new ValidationException(
                            "multiAttributeSameName",
                            "Found multiple definitions for output attribute {0}",
                            name);
                }
                names.add(name);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to access data source to check attribute customization", e);
        } finally {
            if (temporaryVirtualTable && jds != null) jds.dropVirtualTable(typeName);
        }
    }

    /**
     * Creates a temporary virtual table in the data store, returns the name of the virtual table
     */
    private String setupTempVirtualTable(VirtualTable vt, JDBCDataStore ds) throws IOException {
        String temporaryName = null;
        do {
            temporaryName = UUID.randomUUID().toString();
        } while (Arrays.asList(ds.getTypeNames()).contains(temporaryName));
        VirtualTable temporary = new VirtualTable(temporaryName, vt);
        ds.createVirtualTable(temporary);
        return temporaryName;
    }

    private void validate(
            AttributeTypeInfo attribute,
            SimpleFeatureType schema,
            Map<String, AttributeDescriptor> nativeAttributes) {
        try {
            if (attribute.getName() == null || attribute.getName().isEmpty()) {
                throw new ValidationException(
                        "attributeNullName", "Attribute name must not be null or empty");
            }

            if (attribute.getSource() == null || attribute.getSource().isEmpty()) {
                throw new ValidationException(
                        "attributeNullSource", "Attribute source must not be null or empty");
            }

            // parses the CQL expression, will throw exception if invalid
            Expression expression = ECQL.toExpression(attribute.getSource());

            // look for source attributes that might be missing
            FilterAttributeExtractor extractor = new FilterAttributeExtractor(schema);
            expression.accept(extractor, null);
            Set<String> usedSourceAttributes = new HashSet<>(extractor.getAttributeNameSet());
            usedSourceAttributes.removeAll(nativeAttributes.keySet());
            if (!usedSourceAttributes.isEmpty()) {
                throw new ValidationException(
                        "cqlUsesInvalidAttribute",
                        "The CQL source expression for attribute {0} refers to attributes "
                                + "unavailable in the data source: {1}",
                        attribute.getName(),
                        usedSourceAttributes);
            }

            // can we perform the eventual type conversion?
            Class<?> binding = attribute.getBinding();
            if (binding != null) {
                ExpressionTypeVisitor typeVisitor = new ExpressionTypeVisitor(schema);
                Class expressionType = (Class) expression.accept(typeVisitor, null);
                if (!Object.class.equals(expressionType)
                        && !expressionType.equals(binding)
                        && !binding.equals(String.class) // can do even without a Converter
                        && Converters.getConverterFactories(expressionType, binding).isEmpty()) {
                    throw new ValidationException(
                            "attributeInvalidConversion",
                            "Issue found in attribute {0}, unable to convert from native type, "
                                    + "{1}, to target type, {2}",
                            attribute.getName(),
                            expressionType.getName(),
                            binding.getName());
                }
            }
        } catch (CQLException e) {
            throw new ValidationException(
                    "attributeInvalidCQL",
                    "Invalid CQL for {0} source. {1}",
                    attribute.getName(),
                    e.getMessage());
        }
    }
}
