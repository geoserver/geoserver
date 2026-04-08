package org.geoserver.featurestemplating.utils;

import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;

/**
 * This class is used to resolve static templates. It is used to resolve templates that are not dynamic and do not
 * require any processing or features collection evaluation. It will collect all the $${} expressions and replace them
 * with the ECQL resolved expression result from GeoTools ECQL.toExpression method expression creation and evaluation,
 * returning the final placeholders replaced String content.
 */
public final class StaticTemplateResolver {

    private StaticTemplateResolver() {
        // Prevent instantiation
    }

    /**
     * Resolves a static template by replacing all $${} expressions with their evaluated values.
     *
     * @param templateContent The template content containing $${} expressions.
     * @return The resolved template content with all placeholders replaced.
     * @throws IllegalArgumentException if the templateContent is null or invalid expressions are found.
     */
    public static String resolveTemplate(String templateContent) {
        if (templateContent == null) {
            throw new IllegalArgumentException("Template content cannot be null.");
        }

        StringBuilder resolvedContent = new StringBuilder();
        int startIndex = 0;

        while (startIndex < templateContent.length()) {
            int expressionStart = templateContent.indexOf("$${", startIndex);
            if (expressionStart == -1) {
                resolvedContent.append(templateContent.substring(startIndex));
                break;
            }

            resolvedContent.append(templateContent.substring(startIndex, expressionStart));
            startIndex = processExpression(templateContent, resolvedContent, expressionStart);
        }

        return resolvedContent.toString();
    }

    private static int processExpression(String templateContent, StringBuilder resolvedContent, int expressionStart) {
        int expressionEnd = templateContent.indexOf("}", expressionStart);
        if (expressionEnd == -1) {
            throw new IllegalArgumentException("Unclosed $${} expression in template.");
        }
        String expression = templateContent.substring(expressionStart + 3, expressionEnd);
        Expression ecqlExpression = null;
        try {
            ecqlExpression = ECQL.toExpression(expression);
        } catch (CQLException e) {
            throw new RuntimeException(e);
        }
        String evaluatedValue = ecqlExpression.evaluate(null, String.class);
        resolvedContent.append(evaluatedValue);
        return expressionEnd + 1;
    }
}
