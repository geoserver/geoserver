/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.validation;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.expressions.XpathFunction;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * This class perform a validation of a template by evaluating dynamic and source fields using
 * {@link ValidateExpressionVisitor}
 */
public class TemplateValidator {

    static final Logger LOGGER = Logging.getLogger(TemplateValidator.class);

    private FeatureTypeInfo type;

    private String failingAttribute;

    public TemplateValidator(FeatureTypeInfo type) {
        this.type = type;
    }

    public boolean validateTemplate(RootBuilder root) {
        try {
            String source = null;

            ValidateExpressionVisitor validateVisitor =
                    new ValidateExpressionVisitor(
                            new TemplateBuilderContext(type.getFeatureType()));
            return validateExpressions(root, validateVisitor, source);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
        return false;
    }

    private boolean validateExpressions(
            TemplateBuilder builder, ValidateExpressionVisitor visitor, String source) {
        for (TemplateBuilder jb : builder.getChildren()) {
            String siblingSource = null;
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                Expression toValidate = getExpressionToValidate(djb, source, djb.getContextPos());
                if (!validate(toValidate, visitor))
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
                if (sb.getSource() != null) {
                    String typeName =
                            sb.getStrSource().substring(sb.getStrSource().indexOf(":") + 1);
                    if (!type.getName().contains(typeName)) {
                        PropertyName pn = getSourceToValidate(sb, source);
                        if (!validate(pn, visitor)) {
                            failingAttribute = "Source: " + sb.getStrSource();
                            return false;
                        } else {
                            siblingSource = pn.getPropertyName();
                        }
                    }
                }
                if (!validateExpressions(
                        jb, visitor, siblingSource != null ? siblingSource : source)) return false;
            }
            if (jb instanceof AbstractTemplateBuilder) {
                Filter filter =
                        getFilterToValidate(
                                (AbstractTemplateBuilder) jb,
                                siblingSource != null ? siblingSource : source);
                if (filter != null && !validate(filter, visitor)) {
                    failingAttribute = "Filter: " + CQL.toCQL(filter);
                    return false;
                }
            }
        }
        return true;
    }

    public String getFailingAttribute() {
        return failingAttribute;
    }

    public boolean validate(Object toValidate, ValidateExpressionVisitor visitor) {

        if (toValidate instanceof Expression) ((Expression) toValidate).accept(visitor, null);
        else {
            ((Filter) toValidate).accept(visitor, null);
        }
        return visitor.isValid();
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
    private Filter getFilterToValidate(AbstractTemplateBuilder ab, String source) {
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
        String strXpath = pn.getPropertyName();
        int i = 0;
        String newSource = source;
        if (newSource != null) {
            while (i < contextPos) {
                strXpath = strXpath.replaceFirst("\\.\\./", "");
                if (newSource.lastIndexOf('/') != -1)
                    newSource = source.substring(0, source.lastIndexOf('/'));
                else newSource = "";
                i++;
            }
            String newXpath;
            if (!newSource.equals("") && !strXpath.startsWith(newSource))
                newXpath = newSource + "/" + pn.getPropertyName();
            else newXpath = pn.getPropertyName();
            if (pn instanceof AttributeExpressionImpl)
                pn = new AttributeExpressionImpl(newXpath, pn.getNamespaceContext());
            else if (pn instanceof XpathFunction) {
                ((XpathFunction) pn).setPropertyName(newXpath);
            }
        }
        return pn;
    }

    private Object completeXpathWithVisitor(Object cql, String source, int contextPos) {
        DuplicatingFilterVisitor visitor =
                new DuplicatingFilterVisitor() {
                    @Override
                    public Object visit(PropertyName filter, Object extraData) {
                        filter = (PropertyName) super.visit(filter, extraData);
                        Object result = completeXpathForValidation(filter, source, contextPos);
                        if (result != null) return result;
                        return filter;
                    }
                };
        if (cql instanceof Expression) return ((Expression) cql).accept(visitor, null);
        else return ((Filter) cql).accept(visitor, null);
    }
}
