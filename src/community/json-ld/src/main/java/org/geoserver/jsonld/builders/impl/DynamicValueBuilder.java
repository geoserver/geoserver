/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import static org.geoserver.jsonld.expressions.ExpressionsUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.AbstractJsonBuilder;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpression;
import org.geotools.filter.MathExpressionImpl;
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
        super(key);
        this.namespaces = namespaces;
        if (expression.startsWith("$${")) {
            strCqlToExpression(expression);
        } else if (expression.startsWith("${")) {
            strXpathToPropertyName(expression);
        }
    }

    /**
     * Takes a str cql as $${cql} and makes all the necessary operation to convert it to a valid
     * Expression
     */
    private void strCqlToExpression(String cql) {
        // takes xpath fun from cql
        String strXpathFun = extractXpathFromCQL(cql);
        if (strXpathFun.indexOf(XPATH_FUN_START) != -1)
            this.contextPos = determineContextPos(strXpathFun);
        // takes the literal argument of xpathFun
        String literalXpath = getLiteralXpath(strXpathFun);

        // clean the function to obtain a cql expression without xpath() syntax
        this.cql = extractCqlExpressions(cleanCQLExpression(cql, strXpathFun, literalXpath));
        // replace the xpath literal inside the expression with a PropertyName
        literalXpathToPropertyName(this.cql, removeBackDots(literalXpath));
    }

    /**
     * Takes a str xpath as ${xpath} and makes all the necessary operation to produce a valid
     * PropertyName
     */
    private void strXpathToPropertyName(String xpath) {
        String strXpath = extractXpath(xpath);
        this.contextPos = determineContextPos(strXpath);
        strXpath = removeBackDots(strXpath);
        this.xpath = new AttributeExpressionImpl(strXpath, namespaces);
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
            result = cql.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to evaluate expression. Exception: {0}", e.getMessage());
        }
        return result;
    }

    /**
     * Searches for one or more literal xpath inside the expression. If found, substitutes it/them
     * with a ${@link PropertyName}
     */
    private void literalXpathToPropertyName(Expression expr, String literalXpath) {
        List<Expression> params = null;
        if (expr instanceof Function) {
            params = ((Function) expr).getParameters();
        } else if (expr instanceof BinaryExpression) {
            params =
                    Arrays.asList(
                            ((BinaryExpression) expr).getExpression1(),
                            ((BinaryExpression) expr).getExpression2());
        }
        if (params != null) {
            int size = params.size();
            List<Expression> newParams = new ArrayList<>(size);
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    Expression e = params.get(i);
                    if (e instanceof Literal) {
                        e = xpathLiteralToPropertyName((Literal) e, literalXpath);
                        newParams.add(i, e);
                    } else if (e instanceof Function) {
                        newParams.add(e);
                        literalXpathToPropertyName(e, literalXpath);
                    }
                }
            }
            setParamsToExpression(expr, newParams);
        }
    }

    private Expression xpathLiteralToPropertyName(Literal literal, String literalXpath) {
        String unquoted = literalXpath.replaceAll("'", "");
        if (String.valueOf(literal.getValue()).equals(unquoted)) {
            return new AttributeExpressionImpl(unquoted, namespaces);
        } else {
            return literal;
        }
    }

    private void setParamsToExpression(Expression expr, List<Expression> params) {
        if (expr instanceof FunctionExpression) {
            ((FunctionExpression) expr).setParameters(params);
        } else if (expr instanceof MathExpressionImpl) {
            ((MathExpressionImpl) expr).setExpression1(params.get(0));
            ((MathExpressionImpl) expr).setExpression2(params.get(1));
        }
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
}
