/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * Catalog Store Mapping An instance from this class provides a mapping from the data in the
 * Internal Geoserver Catalog to a particular CSW Record Type
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
        protected String key;

        protected Expression content = null;

        protected boolean required = false;

        protected int splitIndex = -1;

        /**
         * Create new Mapping Element
         *
         * @param key The Key to be mapped
         */
        protected CatalogStoreMappingElement(String key) {
            this.key = key;
        }

        /**
         * Getter for mapped key
         *
         * @return Mapped Key
         */
        public String getKey() {
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
        public int getSplitIndex() {
            return splitIndex;
        }
    }

    protected static final Logger LOGGER = Logging.getLogger(CatalogStoreMapping.class);

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected Map<String, CatalogStoreMappingElement> mappingElements =
            new HashMap<String, CatalogStoreMappingElement>();

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
        return mappingElements.values();
    }

    /**
     * Return a mapping element from a mapped key
     *
     * @param key the mapped key
     * @return the element, null if key doesn't exist
     */
    public CatalogStoreMappingElement getElement(String key) {
        return mappingElements.get(key);
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
        Set<String> paths = new HashSet<String>();
        for (PropertyName prop : properties) {
            paths.add(
                    toDotPath(
                            XPathUtil.steps(
                                    rd.getFeatureDescriptor(),
                                    prop.toString(),
                                    rd.getNamespaceSupport())));
        }

        CatalogStoreMapping mapping = new CatalogStoreMapping();

        for (Entry<String, CatalogStoreMappingElement> element : mappingElements.entrySet()) {
            if (element.getValue().isRequired() || paths.contains(element.getKey())) {
                mapping.mappingElements.put(element.getKey(), element.getValue());
            }
        }

        mapping.identifier = identifier;

        mapping.includeEnvelope =
                includeEnvelope && paths.contains(rd.getBoundingBoxPropertyName());

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
     * <p>The textual representation is a set of key-value pairs, where the key represents the
     * mapped key and the value is an OGC expression representing the mapped content. Furthermore,
     * if the key starts with @ it also defines the ID element and if the key starts with $ it is a
     * required property.
     */
    public static CatalogStoreMapping parse(Map<String, String> mappingSource) {

        CatalogStoreMapping mapping = new CatalogStoreMapping();
        for (Map.Entry<String, String> mappingEntry : mappingSource.entrySet()) {

            String key = mappingEntry.getKey();
            boolean required = false;
            boolean id = false;
            int splitIndex = -1;
            if ("$".equals(key.substring(0, 1))) {
                key = key.substring(1);
                required = true;
            }
            if ("@".equals(key.substring(0, 1))) {
                key = key.substring(1);
                id = true;
            }
            if (key.contains("%.")) {
                splitIndex = StringUtils.countMatches(key.substring(0, key.indexOf("%.")), ".");
                key = key.replace("%.", ".");
            }

            CatalogStoreMappingElement element = mapping.mappingElements.get(key);
            if (element == null) {
                element = new CatalogStoreMappingElement(key);
                mapping.mappingElements.put(key, element);
            }

            element.content = parseOgcCqlExpression(mappingEntry.getValue());
            element.required = required;
            element.splitIndex = splitIndex;
            if (id) {
                mapping.identifier = element;
            }
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
                        "Error parsing CQL expression "
                                + sourceExpr
                                + ":\n"
                                + formattedErrorMessage);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = "parsing expression " + sourceExpr;
                LOGGER.log(Level.SEVERE, msg, e);
                throw new IllegalArgumentException(msg + ": " + e.getMessage(), e);
            }
        }
        return expression;
    }

    /**
     * Helper method to convert StepList path to Dot path (separated by dots and no namespace
     * prefixes, used for mapping)
     *
     * @param steps XPath steplist
     * @return String with dot path
     */
    public static String toDotPath(XPathUtil.StepList steps) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            sb.append(steps.get(i).getName().getLocalPart());
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
