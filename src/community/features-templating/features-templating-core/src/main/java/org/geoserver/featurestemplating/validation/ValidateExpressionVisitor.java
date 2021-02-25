/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.validation;

import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.PropertyName;

/**
 * Visitor to perform a validation of a template based on evaluation of xpath against the {@link
 * FeatureType} and on the checking of param count for CQL expression
 */
public class ValidateExpressionVisitor extends DuplicatingFilterVisitor {

    private TemplateBuilderContext context;

    private boolean valid = true;

    public ValidateExpressionVisitor(TemplateBuilderContext context) {
        super();
        this.context = context;
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        Object result = null;
        // attribute selector @ will not evaluate against featureType
        if (!expression.getPropertyName().contains("@")) {
            String xpathPath = expression.getPropertyName();
            PropertyName pn =
                    new AttributeExpressionImpl(xpathPath, expression.getNamespaceContext());
            result = pn.evaluate(context.getCurrentObj());
            if (result == null) valid = false;
        }
        return super.visit(expression, data);
    }

    public TemplateBuilderContext getContext() {
        return context;
    }

    public void setContext(TemplateBuilderContext context) {
        this.context = context;
    }

    public boolean isValid() {
        return valid;
    }
}
