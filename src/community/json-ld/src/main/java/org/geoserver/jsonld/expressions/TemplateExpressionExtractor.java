/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import java.util.ArrayList;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;

/**
 * Helper class that allows the extraction of CQL and Xpath expressions out of a plain text string
 * using special separators. Parsing rules are:
 *
 * <ul>
 *   <li>whatever is between <code>${</code> and <code>}</code> is considered an Xpath expression
 *   <li>whatever is between <code>$${</code> and <code>}</code> is considered a CQL expression
 * </ul>
 */
public class TemplateExpressionExtractor {

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public static String extractXpath(String xpath) {
        boolean inXpath = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < xpath.length(); i++) {
            final char curr = xpath.charAt(i);
            final boolean isLast = (i == xpath.length() - 1);
            final char next = isLast ? 0 : xpath.charAt(i + 1);
            final char prev = xpath.charAt(i > 0 ? i - 1 : 0);

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
     *
     * @param expression
     * @return
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

    /**
     * Given an expression list will create an expression concatenating them.
     *
     * @param
     * @return
     */
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
     *
     * @param expression
     * @return
     */
    public static Expression extractCqlExpressions(String expression) {
        return catenateExpressions(splitCqlExpressions(expression));
    }
}
