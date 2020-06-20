/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class that mainly allows the extraction of CQL and Xpath expressions out of a plain text
 * string using special separators. Moreover, since JsonLd templates can declare an xpath('some
 * xpath') function that has no real FunctionExpression implementation, this class provides methods
 * to extracts the xpath from the function as a literal, to substitute it after cql encoding
 * happened with an AttributeExpression.
 */
public class JsonLdCQLManager {

    private String strCql;

    private int contextPos = 0;

    private NamespaceSupport namespaces;

    public JsonLdCQLManager(String strCql, NamespaceSupport namespaces) {
        this.strCql = strCql;
        this.namespaces = namespaces;
    }

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public static final String XPATH_FUN_START = "xpath(";

    /**
     * Create a PropertyName from a String
     *
     * @return
     */
    public AttributeExpressionImpl getAttributeExpressionFromString() {
        String strXpath = extractXpath(this.strCql);
        this.contextPos = determineContextPos(strXpath);
        strXpath = removeBackDots(strXpath);
        return new AttributeExpressionImpl(strXpath, namespaces);
    }

    /**
     * Create an expression from a string taking care of correctly handling the xpath() expression
     *
     * @return
     */
    public Expression getExpressionFromString() {
        // takes xpath fun from cql
        String strXpathFun = extractXpathFromCQL(this.strCql);
        if (strXpathFun.indexOf(XPATH_FUN_START) != -1)
            this.contextPos = determineContextPos(strXpathFun);
        // takes the literal argument of xpathFun
        String literalXpath = removeBackDots(toLiteralXpath(strXpathFun));

        // clean the function to obtain a cql expression without xpath() syntax
        Expression expression =
                extractCqlExpressions(cleanCQL(this.strCql, strXpathFun, literalXpath));
        // replace the xpath literal inside the expression with a PropertyName
        return (Expression) setPropertyNameToCQL(expression, literalXpath.replaceAll("'", ""));
    }

    /**
     * Create a filter from a string taking care of correctly handling the xpath() expression
     *
     * @return
     * @throws CQLException
     */
    public Filter getFilterFromString() throws CQLException {
        String xpathFunction = extractXpathFromCQL(this.strCql);
        if (xpathFunction.indexOf(XPATH_FUN_START) != -1)
            contextPos = determineContextPos(xpathFunction);
        String literalXpath = removeBackDots(toLiteralXpath(xpathFunction));
        String cleanedCql = cleanCQL(this.strCql, xpathFunction, literalXpath);
        return (Filter)
                setPropertyNameToCQL(ECQL.toFilter(cleanedCql), literalXpath.replaceAll("'", ""));
    }

    /**
     * @param cql the cql filter or expression to which set the clean xpath as a PropertyName
     * @param xpath
     * @return
     */
    private Object setPropertyNameToCQL(Object cql, String xpath) {
        DuplicatingFilterVisitor filterVisitor =
                new DuplicatingFilterVisitor() {
                    @Override
                    public Object visit(Literal expression, Object extraData) {
                        if (expression.getValue() instanceof String) {
                            String strVal = (String) expression.getValue();
                            if (strVal.endsWith(xpath))
                                return new AttributeExpressionImpl(xpath, namespaces);
                        }
                        return super.visit(expression, extraData);
                    }
                };
        Object result;
        if (cql instanceof Expression) {
            result = ((Expression) cql).accept(filterVisitor, null);
        } else {
            result = ((Filter) cql).accept(filterVisitor, null);
        }
        return result;
    }

    private String extractXpath(String xpath) {
        boolean inXpath = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < xpath.length(); i++) {
            final char curr = xpath.charAt(i);
            final boolean isLast = (i == xpath.length() - 1);
            final char next = isLast ? 0 : xpath.charAt(i + 1);

            if (curr == '\\') {
                if (isLast)
                    throw new IllegalArgumentException("Unescaped \\ at position " + (i + 1));

                if (next == '\\') sb.append('\\');
                else if (next == '$') sb.append('$');
                else if (next == '}') sb.append('}');
                else throw new IllegalArgumentException("Unescaped \\ at position " + (i + 1));

                // skip the next character
                i++;
            } else if (curr == '$') {
                if (isLast || next != '{')
                    throw new IllegalArgumentException("Unescaped $ at position " + (i + 1));
                if (inXpath)
                    throw new IllegalArgumentException(
                            "Already found a ${ sequence before the one at " + (i + 1));

                inXpath = true;
                i++;
            } else if (curr == '}') {
                if (!inXpath)
                    throw new IllegalArgumentException(
                            "Already found a ${ sequence before the one at " + (i + 1));

                if (sb.length() == 0)
                    throw new IllegalArgumentException(
                            "Invalid empty cql expression ${} at " + (i - 1));

            } else {
                sb.append(curr);
            }
        }
        return sb.toString();
    }

    /**
     * Parses the original string and returns an array or parsed expressions, in particular, the
     * result of parsing each embedded cql expression and string literals in between the cql
     * expressions, in the order they appear in the original string
     */
    private List<Expression> splitCqlExpressions(String expression) {
        boolean inCqlExpression = false;
        List<Expression> result = new ArrayList<Expression>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            final char curr = expression.charAt(i);
            final boolean isLast = (i == expression.length() - 1);
            final char next = isLast ? 0 : expression.charAt(i + 1);
            final char prev = i > 0 ? expression.charAt(i - 1) : ' ';

            if (curr == '\\') {
                if (isLast)
                    throw new IllegalArgumentException("Unescaped \\ at position " + (i + 1));

                if (next == '\\') sb.append('\\');
                else if (next == '$') sb.append('$');
                else if (next == '}') sb.append('}');
                else throw new IllegalArgumentException("Unescaped \\ at position " + (i + 1));

                // skip the next character
                i++;
            } else if (curr == '$' && prev != '$') {
                if (isLast || next != '$')
                    throw new IllegalArgumentException("Unescaped $ at position " + (i + 1));
                if (inCqlExpression)
                    throw new IllegalArgumentException(
                            "Already found a ${ sequence before the one at " + (i + 1));

                // if we extracted a literal in between two expressions, add it to the result
                if (sb.length() > 0) {
                    result.add(ff.literal(sb.toString()));
                    sb.setLength(0);
                }
                // mark the beginning and skip the next character
                i++;
            } else if (curr == '$' && prev == '$') {
                if (isLast || next != '{')
                    throw new IllegalArgumentException("Unescaped $ at position " + (i + 1));
                if (inCqlExpression)
                    throw new IllegalArgumentException(
                            "Already found a ${ sequence before the one at " + (i + 1));

                // if we extracted a literal in between two expressions, add it to the result
                if (sb.length() > 0) {
                    result.add(ff.literal(sb.toString()));
                    sb.setLength(0);
                }

                // mark the beginning and skip the next character
                inCqlExpression = true;
                i++;
            } else if (curr == '}') {
                /*if (!inCqlExpression)
                throw new IllegalArgumentException(
                        "Already found a ${ sequence before the one at " + (i + 1));*/

                if (sb.length() == 0)
                    throw new IllegalArgumentException(
                            "Invalid empty cql expression ${} at " + (i - 1));

                try {
                    result.add(ECQL.toExpression(sb.toString()));
                    sb.setLength(0);
                } catch (CQLException e) {
                    throw new IllegalArgumentException("Invalid cql expression '" + sb + "'", e);
                }
                inCqlExpression = false;
            } else {
                if (curr != '{') sb.append(curr);
            }
        }

        // when done, if we are still in a CQL expression, it means it hasn't been closed
        if (inCqlExpression) {
            throw new IllegalArgumentException("Unclosed CQL expression '" + sb + "'");
        } else if (sb.length() > 0) {
            result.add(ff.literal(sb.toString()));
        }
        return result;
    }

    /** Given an expression list will create an expression concatenating them. */
    private Expression catenateExpressions(List<Expression> expressions) {
        if (expressions == null || expressions.size() == 0)
            throw new IllegalArgumentException(
                    "You should provide at least one expression in the list");

        if (expressions.size() == 1) {
            return expressions.get(0);
        } else {
            return ff.function("Concatenate", expressions.toArray(new Expression[] {}));
        }
    }

    /**
     * Builds a CQL expression equivalent to the specified string, see class javadocs for rules on
     * how to build the expression in string form
     */
    private Expression extractCqlExpressions(String expression) {
        return catenateExpressions(splitCqlExpressions(expression));
    }

    public static String removeQuotes(String cqlFilter) {
        cqlFilter = cqlFilter.replaceFirst("\"", "");
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < cqlFilter.length(); i++) {
            char curr = cqlFilter.charAt(i);
            if (curr != '\"') {
                strBuilder.append(curr);
            } else {
                if (i != cqlFilter.length() && cqlFilter.charAt(i + 1) != ' ')
                    strBuilder.append(curr);
            }
        }
        return strBuilder.toString();
    }

    /**
     * Clean a CQL from the xpath function syntax to make the xpath suitable to be encoded as a
     * PropertyName
     */
    private String cleanCQL(String cql, String toReplace, String replacement) {
        if (cql.indexOf(XPATH_FUN_START) != -1)
            return cql.replace(toReplace, replacement).replaceAll("\\.\\./", "");
        else return cql;
    }

    public static String removeBackDots(String xpath) {
        if (xpath.indexOf("../") != -1) return xpath.replaceAll("\\.\\./", "");
        return xpath;
    }

    /** Extract the xpath function from CQL Expression if present */
    private String extractXpathFromCQL(String expression) {
        int xpathI = expression.indexOf(XPATH_FUN_START);
        if (xpathI != -1) {
            int xpathI2 = expression.indexOf(")", xpathI);
            String strXpath = expression.substring(xpathI, xpathI2 + 1);
            return strXpath;
        }
        return expression;
    }

    /** Extract the literal argument from the xpath function */
    private String toLiteralXpath(String strXpath) {
        if (strXpath.indexOf(XPATH_FUN_START) != -1) {
            return strXpath.replace(XPATH_FUN_START, "").replace(")", "");
        }
        return strXpath;
    }

    /**
     * Determines how many times is needed to walk up {@link JsonBuilderContext} in order to execute
     * xpath, and cleans it from ../ notation.
     *
     * @param xpath
     * @return
     */
    public static int determineContextPos(String xpath) {
        int contextPos = 0;
        while (xpath.contains("../")) {
            contextPos++;
            xpath = xpath.replaceFirst("\\.\\./", "");
        }
        return contextPos;
    }

    public int getContextPos() {
        return contextPos;
    }

    /**
     * Wrap attribute selector syntax in a xpath with quotation marks to make it suitable by cql
     * compiler.
     */
    public static String quoteXpathAttribute(String xpath) {
        int atIndex = xpath.indexOf("@");
        if (atIndex != -1) {
            Pattern pattern = Pattern.compile("[a-zA-Z()<>.\\-1-9*]");
            String substring = xpath.substring(atIndex + 1);
            StringBuilder xpathAttribute = new StringBuilder("@");
            for (int i = 0; i < substring.length(); i++) {
                char current = substring.charAt(i);
                Matcher matcher = pattern.matcher(String.valueOf(current));
                if (matcher.matches()) {
                    xpathAttribute.append(current);
                } else {
                    break;
                }
            }
            return xpath.replaceAll(
                    xpathAttribute.toString(), "\"" + xpathAttribute.toString() + "\"");
        }
        return xpath;
    }
}
