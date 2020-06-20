/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.AbstractJsonBuilder;
import org.geoserver.jsonld.expressions.JsonLdCQLManager;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.filter.expression.*;
import org.xml.sax.helpers.NamespaceSupport;

/** Evaluates xpath and cql functions, writing their results to the output. */
public class DynamicValueBuilder extends AbstractJsonBuilder {

    private Expression cql;

    private AttributeExpressionImpl xpath;

    private int contextPos = 0;

    private NamespaceSupport namespaces;

    private static final Logger LOGGER = Logging.getLogger(DynamicValueBuilder.class);

    public DynamicValueBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.namespaces = namespaces;
        JsonLdCQLManager cqlManager = new JsonLdCQLManager(expression, namespaces);
        if (expression.startsWith("$${")) {
            this.cql = cqlManager.getExpressionFromString();
        } else if (expression.startsWith("${")) {
            this.xpath = cqlManager.getAttributeExpressionFromString();
        }
        this.contextPos = cqlManager.getContextPos();
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        Object o = null;
        if (evaluateFilter(context)) {
            if (xpath != null) {

                o = evaluateXPath(context);

            } else if (cql != null) {
                o = evaluateExpressions(context);
            }
            if (canWriteValue(o)) {
                writeKey(writer);
                writer.writeResult(o);
            }
        }
    }

    public Expression getCql() {
        return cql;
    }

    public AttributeExpressionImpl getXpath() {
        return xpath;
    }

    private Object evaluateXPath(JsonBuilderContext context) {
        int i = 0;
        while (i < contextPos) {
            context = context.getParent();
            i++;
        }
        Object result = null;
        try {
            result = xpath.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Unable to evaluate xpath " + xpath + ". Exception: {0}",
                    e.getMessage());
        }
        return result;
    }

    private Object evaluateExpressions(JsonBuilderContext context) {
        Object result = null;
        try {
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            result = cql.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to evaluate expression. Exception: {0}", e.getMessage());
        }
        return result;
    }

    /**
     * A value can only be wrote if it is non NULL and not an empty list. This method supports *
     * complex features attributes, this method will be invoked recursively on the attribute value.
     */
    private boolean canWriteValue(Object result) {
        if (result instanceof ComplexAttributeImpl) {
            return canWriteValue(((ComplexAttribute) result).getValue());
        } else if (result instanceof Attribute) {
            return canWriteValue(((Attribute) result).getValue());
        } else if (result instanceof List && ((List) result).size() == 0) {
            if (((List) result).size() == 0) return false;
            else return true;
        } else if (result == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean checkNotNullValue(JsonBuilderContext context) {
        Object o = null;
        if (xpath != null) {

            o = evaluateXPath(context);

        } else if (cql != null) {
            o = evaluateExpressions(context);
        }
        if (o == null) return false;
        return true;
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public int getContextPos() {
        return contextPos;
    }
}
