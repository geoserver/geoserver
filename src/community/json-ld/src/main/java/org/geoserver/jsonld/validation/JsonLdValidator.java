/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.validation;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.IteratingBuilder;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geotools.filter.FunctionExpression;
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * This class perform a validation of the json-ld template by evaluating dynamic and source fields
 * using {@link ValidateExpressionVisitor}
 */
public class JsonLdValidator {

    private ValidateExpressionVisitor visitor;

    private FeatureTypeInfo type;

    private String failingAttribute;

    public JsonLdValidator(FeatureTypeInfo type) {
        visitor = new ValidateExpressionVisitor();
        this.type = type;
    }

    public boolean validateTemplate(RootBuilder root) {
        try {
            return validateExpressions(root, new JsonBuilderContext(type.getFeatureType()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean validateExpressions(JsonBuilder builder, JsonBuilderContext context) {
        for (JsonBuilder jb : builder.getChildren()) {
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                if (djb.getCql() != null) {
                    if (!validateCQL(djb.getCql(), context, djb.getKey())) return false;
                } else if (djb.getXpath() != null) {
                    if (!validatePropertyName((PropertyName) djb.getXpath(), context, djb.getKey()))
                        return false;
                }
            } else if (jb instanceof SourceBuilder) {
                Object newType = null;
                SourceBuilder sb = ((SourceBuilder) jb);
                if (sb.getSource() != null) {
                    String typeName =
                            sb.getStrSource().substring(sb.getStrSource().indexOf(":") + 1);
                    if (!type.getName().contains(typeName)) {
                        newType = sb.getSource().accept(visitor, context);
                        if (newType == null) {
                            failingAttribute = "Source: " + sb.getStrSource();
                            return false;
                        }
                    }
                } else {
                    if (sb instanceof IteratingBuilder) return false;
                }
                if (newType != null) {
                    JsonBuilderContext newContext = new JsonBuilderContext(newType);
                    newContext.setParent(context);
                    context = newContext;
                }
                return validateExpressions(jb, context);
            }
        }
        return true;
    }

    public String getFailingAttribute() {
        return failingAttribute;
    }

    private boolean validateCQL(Expression expression, JsonBuilderContext context, String key) {
        PropertyName pn;
        if (expression instanceof PropertyName) {
            pn = (PropertyName) expression;
            if (!validatePropertyName(pn, context, key)) return false;
        } else if (expression instanceof FunctionExpression) {
            FunctionExpression function = (FunctionExpression) expression;
            if (function.getParameters().size() > 0) {
                for (Expression ex : function.getParameters()) {
                    if (!validateCQL(ex, context, key)) return false;
                }
            }
        } else if (expression instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expression;
            if (!validateCQL(binary.getExpression1(), context, key)) return false;
            if (!validateCQL(binary.getExpression2(), context, key)) return false;
        }
        return true;
    }

    private boolean validatePropertyName(PropertyName pn, JsonBuilderContext context, String key) {
        try {
            if (pn != null && pn.accept(visitor, context) == null) {
                failingAttribute = "Key: " + key + " Value: " + pn.getPropertyName();
                return false;
            }
        } catch (Exception e) {
            failingAttribute = "Exception: " + e.getMessage();
            return false;
        }
        return true;
    }
}
