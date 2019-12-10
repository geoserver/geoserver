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
import org.geoserver.jsonld.expressions.TemplateExpressionExtractor;
import org.geoserver.jsonld.expressions.XPathFunction;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.xml.sax.helpers.NamespaceSupport;

/** Evaluates xpath and cql functions, writing their results to the output. */
public class DynamicValueBuilder extends AbstractJsonBuilder {

    private Expression cql;

    private AttributeExpressionImpl xpath;

    private int contextPos = 0;

    private NamespaceSupport namespaces;

    private static final Logger LOGGER = Logging.getLogger(DynamicValueBuilder.class);

    public DynamicValueBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key);
        this.namespaces = namespaces;
        if (expression.startsWith("$${")) {
            this.cql =
                    TemplateExpressionExtractor.extractCqlExpressions(
                            workXpathFunction(expression));
        } else if (expression.startsWith("${")) {
            String strXpath = TemplateExpressionExtractor.extractXpath(expression);
            strXpath = determineContextPos(strXpath);
            this.xpath = new AttributeExpressionImpl(strXpath, namespaces);
        }
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        Object o = null;
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

    public Expression getCql() {
        return cql;
    }

    public Expression getXpath() {
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
            if (namespaces != null) prepareXpathFilter(cql);
            result = cql.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to evaluate expression. Exception: {0}", e.getMessage());
        }
        return result;
    }

    private void prepareXpathFilter(Expression expr) {
        List<Expression> params = ((Function) expr).getParameters();
        if (params.size() > 0) {
            for (Expression e : params) {
                if (e instanceof XPathFunction) ((XPathFunction) e).setNamespaces(namespaces);
                else if (e instanceof Function) {
                    prepareXpathFilter(e);
                }
            }
        }
    }

    /**
     * Determines how many times is needed to walk up {@link JsonBuilderContext} in order to execute
     * xpath, and cleans it from ../ notation.
     *
     * @param xpath
     * @return
     */
    private String determineContextPos(String xpath) {
        while (xpath.contains("../")) {
            contextPos++;
            xpath = xpath.replaceFirst("\\.\\./", "");
        }
        return xpath;
    }

    private String workXpathFunction(String expression) {
        // extract xpath from cql expression if present
        int xpathI = expression.indexOf("xpath(");
        if (xpathI != -1) {
            int xpathI2 = expression.indexOf(")", xpathI);
            String xpath = expression.substring(xpathI, xpathI2 + 1);
            determineContextPos(xpath);
            expression = expression.replaceAll("\\.\\./", "");
        }
        return expression;
    }

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
}
