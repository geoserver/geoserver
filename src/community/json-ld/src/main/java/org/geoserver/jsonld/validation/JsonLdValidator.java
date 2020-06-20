/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.validation;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.jsonld.builders.AbstractJsonBuilder;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.IteratingBuilder;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * This class perform a validation of the json-ld template by evaluating dynamic and source fields
 * using {@link ValidateExpressionVisitor}
 */
public class JsonLdValidator {

    private FeatureTypeInfo type;

    private String failingAttribute;

    public JsonLdValidator(FeatureTypeInfo type) {
        this.type = type;
    }

    public boolean validateTemplate(RootBuilder root) {
        try {
            ValidateExpressionVisitor validateVisitor =
                    new ValidateExpressionVisitor(new JsonBuilderContext(type.getFeatureType()));
            String source = null;
            return validateExpressions(root, validateVisitor, source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean validateExpressions(
            JsonBuilder builder, ValidateExpressionVisitor visitor, String source) {
        for (JsonBuilder jb : builder.getChildren()) {
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                Expression toValidate = getExpressionToValidate(djb, source, djb.getContextPos());
                if (validate(toValidate, visitor) == null)
                    if (djb.getCql() != null) {
                        this.failingAttribute =
                                "Key: " + djb.getKey() + " Value: " + CQL.toCQL(djb.getCql());
                        return false;
                    } else if (djb.getXpath() != null) {
                        this.failingAttribute =
                                "Key: " + djb.getKey() + " Value: " + CQL.toCQL(djb.getXpath());
                        return false;
                    }
            } else if (jb instanceof SourceBuilder) {
                SourceBuilder sb = ((SourceBuilder) jb);
                String siblingSource = null;
                if (sb.getSource() != null) {
                    String typeName =
                            sb.getStrSource().substring(sb.getStrSource().indexOf(":") + 1);
                    if (!type.getName().contains(typeName)) {
                        PropertyName pn = getSourceToValidate(sb, source);
                        if (validate(pn, visitor) == null) {
                            failingAttribute = "Source: " + sb.getStrSource();
                            return false;
                        } else {
                            siblingSource = pn.getPropertyName();
                        }
                    }
                } else {
                    if (sb instanceof IteratingBuilder) return false;
                }
                if (!validateExpressions(
                        jb, visitor, siblingSource != null ? siblingSource : source)) return false;
            } else {
                Filter filter = getFilterToValidate((AbstractJsonBuilder) builder, source);
                if (filter != null && validate(filter, visitor) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getFailingAttribute() {
        return failingAttribute;
    }

    public Object validate(Object toValidate, ValidateExpressionVisitor visitor) {
        Object result = null;
        if (toValidate instanceof Expression)
            result = ((Expression) toValidate).accept(visitor, null);
        else {
            result = ((Filter) toValidate).accept(visitor, null);
        }
        return result;
    }

    /**
     * Produce an AttributeExpressionImpl from the source attribute, suitable to be validated, eg.
     * taking cares of handling properly changes of context
     */
    private AttributeExpressionImpl getSourceToValidate(SourceBuilder sb, String strSource) {
        AttributeExpressionImpl source = (AttributeExpressionImpl) sb.getSource();
        if (strSource == null) strSource = sb.getStrSource();
        else strSource += "/" + sb.getStrSource();
        return new AttributeExpressionImpl(strSource, source.getNamespaceContext());
    }

    /**
     * Produce a Filter from the filter attribute, suitable to be validated eg. taking cares of
     * handling properly ../ and changes of context
     */
    private Filter getFilterToValidate(AbstractJsonBuilder ab, String source) {
        if (ab.getFilter() != null)
            return (Filter)
                    completeXpathWithVisitor(ab.getFilter(), source, ab.getFilterContextPos());
        return null;
    }

    /**
     * Produce an expression from the xpath or the cql expression hold by the DynamicBuilder,
     * suitable to be validated, eg. taking care of handling properly ../ and changes of context
     */
    private Expression getExpressionToValidate(
            DynamicValueBuilder db, String source, int contextPos) {
        if (db.getXpath() != null) {
            return completeXpathForValidation(db.getXpath(), source, contextPos);
        } else {
            return (Expression) completeXpathWithVisitor(db.getCql(), source, contextPos);
        }
    }

    private PropertyName completeXpathForValidation(
            PropertyName pn, String source, int contextPos) {
        if (pn instanceof AttributeExpressionImpl) {
            AttributeExpressionImpl old = (AttributeExpressionImpl) pn;
            String strXpath = old.getPropertyName();
            int i = 0;
            String newSource = source;
            if (newSource != null) {
                while (i < contextPos) {
                    strXpath = strXpath.replaceFirst("\\.\\./", "");
                    newSource = source.substring(0, source.lastIndexOf('/'));
                    i++;
                }
                return new AttributeExpressionImpl(
                        newSource + "/" + old.getPropertyName(), old.getNamespaceContext());
            } else {
                return pn;
            }
        } else {
            return null;
        }
    }

    private Object completeXpathWithVisitor(Object cql, String source, int contextPos) {
        DuplicatingFilterVisitor visitor =
                new DuplicatingFilterVisitor() {
                    @Override
                    public Object visit(PropertyName filter, Object extraData) {
                        Object result = completeXpathForValidation(filter, source, contextPos);
                        if (result != null) return result;
                        return super.visit(filter, extraData);
                    }
                };
        if (cql instanceof Expression) return ((Expression) cql).accept(visitor, null);
        else return ((Filter) cql).accept(visitor, null);
    }
}
