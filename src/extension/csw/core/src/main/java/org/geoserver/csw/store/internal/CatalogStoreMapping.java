/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.util.PropertyPath;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;

/**
 * Catalog Store Mapping An instance from this class provides a mapping from the data in the Internal Geoserver Catalog
 * to a particular CSW Record Type
 *
 * @author Niels Charlier
 */
public class CatalogStoreMapping {

    /**
     * A Catalog Store Mapping Element, provides mapping of particular attribute.
     *
     * @author Niels Charlier
     */
    public static class CatalogStoreMappingElement {
        protected PropertyPath key;

        protected Expression content = null;

        protected boolean required = false;

        protected int[] splitIndex = {};

        /**
         * Create new Mapping Element
         *
         * @param key The Key to be mapped
         */
        protected CatalogStoreMappingElement(PropertyPath key) {
            this.key = key;
        }

        /**
         * Getter for mapped key
         *
         * @return Mapped Key
         */
        public PropertyPath getKey() {
            return key;
        }

        /**
         * Mapper for mapped content expression
         *
         * @return content
         */
        public Expression getContent() {
            return content;
        }

        /**
         * Getter for required property
         *
         * @return true if property is required
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Getter for splitIndex property
         *
         * @return splitIndex
         */
        public int[] getSplitIndex() {
            return splitIndex;
        }
    }

    protected String mappingName;

    protected static final Logger LOGGER = Logging.getLogger(CatalogStoreMapping.class);

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected List<CatalogStoreMappingElement> mappingElements = new ArrayList<>();

    protected CatalogStoreMappingElement identifier = null;

    protected boolean includeEnvelope = true;

    /** Create new Catalog Store Mapping */
    protected CatalogStoreMapping() {}

    /**
     * Return Collection of all Elements
     *
     * @return a Collection with all Mapping Elements
     */
    public final Collection<CatalogStoreMappingElement> elements() {
        return mappingElements;
    }

    /**
     * Return a mapping element from a mapped key
     *
     * @param pattern property path pattern
     * @return the element, null if key doesn't exist
     */
    public Collection<CatalogStoreMappingElement> elements(PropertyPath pattern) {
        return mappingElements.stream()
                .filter(el -> el.getKey().matches(pattern))
                .collect(Collectors.toList());
    }

    /**
     * Getter for the identifier element, provides identifier expression for features
     *
     * @return the identifier element
     */
    public CatalogStoreMappingElement getIdentifierElement() {
        return identifier;
    }

    /**
     * Create a submapping from a list of property names Required properties will also be included.
     *
     * @param properties list of property names to be included in submapping
     * @param rd Record Descriptor
     */
    public CatalogStoreMapping subMapping(List<PropertyName> properties, RecordDescriptor rd) {
        Set<PropertyPath> patterns = new HashSet<>();
        for (PropertyName prop : properties) {
            patterns.add(PropertyPath.fromXPath(
                    XPathUtil.steps(rd.getFeatureDescriptor(), prop.toString(), rd.getNamespaceSupport())));
        }

        CatalogStoreMapping mapping = new CatalogStoreMapping();

        for (CatalogStoreMappingElement element : mappingElements) {
            if (element.isRequired()
                    || patterns.stream().anyMatch(pattern -> element.getKey().matches(pattern))) {
                mapping.mappingElements.add(element);
            }
        }

        mapping.identifier = identifier;
        PropertyPath bboxPropName =
                PropertyPath.fromDotPath(rd.getQueryablesMapping(mappingName).getBoundingBoxPropertyName());
        mapping.includeEnvelope = includeEnvelope
                && bboxPropName != null
                && patterns.stream().anyMatch(pattern -> bboxPropName.matches(pattern));

        mapping.mappingName = mappingName;

        return mapping;
    }

    public void setIncludeEnvelope(boolean value) {
        this.includeEnvelope = value;
    }

    public boolean isIncludeEnvelope() {
        return includeEnvelope;
    }

    /**
     * Parse a Textual representation of the mapping to create a CatalogStoreMapping
     *
     * <p>The textual representation is a set of key-value pairs, where the key represents the mapped key and the value
     * is an OGC expression representing the mapped content. Furthermore, if the key starts with @ it also defines the
     * ID element and if the key starts with $ it is a required property.
     */
    public static CatalogStoreMapping parse(Map<String, String> mappingSource, String mappingName) {

        CatalogStoreMapping mapping = new CatalogStoreMapping();
        for (Map.Entry<String, String> mappingEntry : mappingSource.entrySet()) {

            String key = mappingEntry.getKey();
            boolean required = false;
            boolean id = false;
            if ("$".equals(key.substring(0, 1))) {
                key = key.substring(1);
                required = true;
            }
            if ("@".equals(key.substring(0, 1))) {
                key = key.substring(1);
                id = true;
            }
            if ("\\".equals(key.substring(0, 1))) {
                // escape character
                // can be used to avoid attribute @ being confused with id @
                key = key.substring(1);
            }
            List<Integer> splitIndexes = new ArrayList<>();
            while (key.contains("%.")) {
                splitIndexes.add(StringUtils.countMatches(key.substring(0, key.indexOf("%.")), "."));
                key = key.replaceFirst(Pattern.quote("%."), ".");
            }

            CatalogStoreMappingElement element = new CatalogStoreMappingElement(PropertyPath.fromDotPath(key));
            mapping.mappingElements.add(element);

            element.content = parseOgcCqlExpression(mappingEntry.getValue());
            element.required = required;
            element.splitIndex = new int[splitIndexes.size()];
            for (int i = 0; i < splitIndexes.size(); i++) {
                element.splitIndex[i] = splitIndexes.get(i);
            }
            if (id) {
                mapping.identifier = element;
            }
        }

        int index = mappingName.indexOf('-');
        if (index >= 0) {
            mapping.setMappingName(mappingName.substring(index + 1));
        } else {
            mapping.setMappingName("");
        }

        return mapping;
    }

    /**
     * Helper method to parce cql expression
     *
     * @param sourceExpr The cql expression string
     * @return the expression
     */
    protected static Expression parseOgcCqlExpression(String sourceExpr) {
        Expression expression = Expression.NIL;
        if (sourceExpr != null && sourceExpr.trim().length() > 0) {
            try {
                expression = CQL.toExpression(sourceExpr, ff);
            } catch (CQLException e) {
                String formattedErrorMessage = e.getMessage();
                LOGGER.log(Level.SEVERE, formattedErrorMessage, e);
                throw new IllegalArgumentException(
                        "Error parsing CQL expression " + sourceExpr + ":\n" + formattedErrorMessage);
            } catch (Exception e) {
                String msg = "parsing expression " + sourceExpr;
                LOGGER.log(Level.SEVERE, msg, e);
                throw new IllegalArgumentException(msg + ": " + e.getMessage(), e);
            }
        }
        return expression;
    }

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }
}
