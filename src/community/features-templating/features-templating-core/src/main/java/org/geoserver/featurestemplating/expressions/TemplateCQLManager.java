/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.expressions.aggregate.StringCQLFunction;
import org.geoserver.util.XCQL;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Helper class that mainly allows the extraction of CQL and Xpath expressions out of a plain text
 * string using special separators. Moreover, since JsonLd templates can declare an xpath('some
 * xpath') function that has no real FunctionExpression implementation, this class provides methods
 * to extracts the xpath from the function as a literal, to substitute it after cql encoding
 * happened with an AttributeExpression.
 */
public class TemplateCQLManager {

    private String strCql;

    private int contextPos = 0;

    private NamespaceSupport namespaces;

    public TemplateCQLManager(String strCql, NamespaceSupport namespaces) {
        this.strCql = strCql;
        this.namespaces = namespaces;
    }

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public static final String XPATH_FUN_START = "xpath(";

    public static final String PROPERTY_FUN_START = "propertyPath(";

    /**
     * Create a PropertyName from a String
     *
     * @return
     */
    public AttributeExpressionImpl getAttributeExpressionFromString() {
        String property = extractProperty(this.strCql);
        if (property.indexOf(".") != -1 && property.indexOf("/") == -1)
            property = replaceDotSeparatorWithSlash(property);
        this.contextPos = determineContextPos(property);
        property = removeBackDots(property);
        if (property.indexOf(".") != -1) property = property.replaceAll("\\.", "/");
        return new AttributeExpressionImpl(property, namespaces);
    }

    /**
     * Create an expression from a string taking care of correctly handling the xpath() expression
     *
     * @return
     */
    public Expression getExpressionFromString() {
        // TODO XPath and PropertyPath function are used here as pattern to recognize strings
        // in filter to be managed and converted to real PropertyName.
        // This is due the reference to previous context through ../ that is handled in the
        // DynamicValueBuilder
        // and due backwards mapping integration with OGCApi filter handling. This should be
        // changed the two function should be used as function and be able to evaluate the reference
        // to previous context avoiding this string manipulation that is done here.

        // takes xpath fun from cql
        String strPropertyNameFun = extractPropertyNameFunction(this.strCql);
        String propertyWithSlash = replaceDotSeparatorWithSlashInFunction(strPropertyNameFun);
        if (containsAPropertyNameFunction(propertyWithSlash))
            this.contextPos = determineContextPos(propertyWithSlash);
        // takes the literal argument of xpathFun
        String literalXpath = removeBackDots(propertyWithSlash);

        // clean the function to obtain a cql expression without xpath() syntax
        Expression cql =
                extractCqlExpressions(cleanCQL(this.strCql, strPropertyNameFun, literalXpath));
        TemplatingExpressionVisitor visitor = new TemplatingExpressionVisitor();
        cql.accept(visitor, null);
        return cql;
    }

    /**
     * Create a filter from a string taking care of correctly handling the xpath() expression
     *
     * @return
     * @throws CQLException
     */
    public Filter getFilterFromString() throws CQLException {
        String propertyNameFun = extractPropertyNameFunction(this.strCql);
        String propertyWithSlash = replaceDotSeparatorWithSlashInFunction(propertyNameFun);
        if (containsAPropertyNameFunction(propertyWithSlash))
            contextPos = determineContextPos(propertyWithSlash);
        String literalPn = removeBackDots(propertyWithSlash);
        String cleanedCql = cleanCQL(this.strCql, propertyNameFun, literalPn);
        Filter templateFilter = XCQL.toFilter(cleanedCql);
        TemplatingExpressionVisitor visitor = new TemplatingExpressionVisitor();
        return (Filter) templateFilter.accept(visitor, null);
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

    private String extractProperty(String property) {
        boolean inXpath = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < property.length(); i++) {
            final char curr = property.charAt(i);
            final boolean isLast = (i == property.length() - 1);
            final char next = isLast ? 0 : property.charAt(i + 1);

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
                    Expression parsed = ECQL.toExpression(sb.toString());
                    TemplatingExpressionVisitor visitor = new TemplatingExpressionVisitor();
                    Expression namespaced = (Expression) parsed.accept(visitor, null);
                    result.add(namespaced);
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

    /**
     * Clean a CQL from the xpath function syntax to make the xpath suitable to be encoded as a
     * PropertyName
     */
    private String cleanCQL(String cql, String toReplace, String replacement) {
        if (containsAPropertyNameFunction(cql))
            cql = cql.replace(toReplace, replacement).replaceAll("\\.\\./", "");
        return cql;
    }

    public static String removeBackDots(String xpath) {
        if (xpath.indexOf("../") != -1) return xpath.replaceAll("\\.\\./", "");
        return xpath;
    }

    /** Extract the xpath function from CQL Expression if present */
    private String extractPropertyNameFunction(String expression) {
        int propertyI = expression.indexOf(XPATH_FUN_START);
        if (propertyI == -1) propertyI = expression.indexOf(PROPERTY_FUN_START);
        if (propertyI != -1) {
            int propertyI2 = expression.indexOf(")", propertyI);
            String strProperty = expression.substring(propertyI, propertyI2 + 1);
            return strProperty;
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
     * Determines how many times is needed to walk up {@link TemplateBuilderContext} in order to
     * execute xpath, and cleans it from ../ notation.
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
        int quotedAtIndex = xpath.indexOf("\"@\"");
        if (atIndex != -1 && quotedAtIndex == -1) {
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

    public Expression getThis() {
        return ff.function("xpath", ff.literal("."));
    }

    /** Can be used to force namespace support into parsed CQL expressions */
    private final class TemplatingExpressionVisitor extends DuplicatingFilterVisitor {

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            if (expression instanceof XpathFunction) {
                XpathFunction f = (XpathFunction) visit(((Function) expression), extraData);
                f.setNamespaceContext(namespaces);
                return f;
            }
            return getFactory(extraData).property(expression.getPropertyName(), namespaces);
        }

        @Override
        public Object visit(Function expression, Object extraData) {
            if (expression instanceof StringCQLFunction) {
                StringCQLFunction function = (StringCQLFunction) expression;
                function.setNamespaceSupport(namespaces);
            }
            return super.visit(expression, extraData);
        }
    }

    private String replaceDotSeparatorWithSlashInFunction(String propertyName) {
        if (propertyName.indexOf(PROPERTY_FUN_START) != -1
                && propertyName.indexOf(".") != -1
                && propertyName.indexOf("/") == -1) {
            propertyName = propertyName.replaceAll(" ", "");
            int startPath =
                    propertyName.indexOf(PROPERTY_FUN_START) + (PROPERTY_FUN_START + "'").length();
            int endPath = propertyName.lastIndexOf("')");
            String content = propertyName.substring(startPath, endPath);
            String replaced = replaceDotSeparatorWithSlash(content);
            propertyName = propertyName.replace(content, replaced);
        }
        return propertyName;
    }

    private String replaceDotSeparatorWithSlash(String propertyName) {
        char[] chars = propertyName.toCharArray();
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            char prev = ' ';
            char next = ' ';
            if (i > 0) prev = chars[i - 1];
            if (i < chars.length - 1) next = chars[i + 1];
            if (current == '.') {
                if (((i + 1) % 3) == 0 || (prev != '.' && next != '.')) sb.append("/");
                else sb.append(current);
            } else sb.append(current);
        }
        return sb.toString();
    }

    private boolean containsAPropertyNameFunction(String cql) {
        return cql.indexOf(XPATH_FUN_START) != -1 || cql.indexOf(PROPERTY_FUN_START) != -1;
    }
}
