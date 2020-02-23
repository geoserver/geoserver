/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.type.Types;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class that mainly allows the extraction of CQL and Xpath expressions out of a plain text
 * string using special separators. It also provides some Utility methods to handle namespaces and
 * xpath syntax
 */
public class ExpressionsUtils {

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public static final String XPATH_FUN_START = "xpath(";

    public static String extractXpath(String xpath) {
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
    static List<Expression> splitCqlExpressions(String expression) {
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
    static Expression catenateExpressions(List<Expression> expressions) {
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
    public static Expression extractCqlExpressions(String expression) {
        return catenateExpressions(splitCqlExpressions(expression));
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

    /**
     * Extract Namespaces from given FeatureType
     *
     * @return Namespaces if found for the given FeatureType
     */
    public static NamespaceSupport declareNamespaces(FeatureType type) {
        NamespaceSupport namespaceSupport = null;
        if (type instanceof ComplexFeatureTypeImpl) {
            Map namespaces = (Map) type.getUserData().get(Types.DECLARED_NAMESPACES_MAP);
            if (namespaces != null) {
                namespaceSupport = new NamespaceSupport();
                for (Iterator it = namespaces.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String prefix = (String) entry.getKey();
                    String namespace = (String) entry.getValue();
                    namespaceSupport.declarePrefix(prefix, namespace);
                }
            }
        }
        return namespaceSupport;
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
     * Clean a CQL expression from the xpath function syntax to make the xpath suitable to be
     * encoded as a PropertyName
     */
    public static String cleanCQLExpression(
            String expression, String toReplace, String replacement) {
        if (expression.indexOf(XPATH_FUN_START) != -1)
            return expression.replace(toReplace, replacement).replaceAll("\\.\\./", "");
        else return expression;
    }

    public static String removeBackDots(String xpath) {
        if (xpath.indexOf("../") != -1) return xpath.replaceAll("\\.\\./", "");
        return xpath;
    }

    /** Extract the xpath function from CQL Expression if present */
    public static String extractXpathFromCQL(String expression) {
        int xpathI = expression.indexOf(XPATH_FUN_START);
        if (xpathI != -1) {
            int xpathI2 = expression.indexOf(")", xpathI);
            String strXpath = expression.substring(xpathI, xpathI2 + 1);
            return strXpath;
        }
        return expression;
    }

    /** Extract the literal argument from the xpath function */
    public static String getLiteralXpath(String strXpath) {
        if (strXpath.indexOf(XPATH_FUN_START) != -1) {
            return strXpath.replace(XPATH_FUN_START, "").replace(")", "");
        }
        return strXpath;
    }

    /**
     * Determines how many times is needed to walk up {@link JsonBuilderContext} in order to execute
     * xpath, and cleans it from ../ notation.
     */
    public static int determineContextPos(String xpath) {
        int contextPos = 0;
        while (xpath.contains("../")) {
            contextPos++;
            xpath = xpath.replaceFirst("\\.\\./", "");
        }
        return contextPos;
    }
}
