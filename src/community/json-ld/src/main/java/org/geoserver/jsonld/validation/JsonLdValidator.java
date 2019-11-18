package org.geoserver.jsonld.validation;

import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

/**
 * This class perform a validation of the json-ld template by evaluating dynamic and source fields
 * using {@link ValidateExpressionVisitor}
 */
public class JsonLdValidator {

    private ValidateExpressionVisitor visitor;

    private FeatureType type;

    public JsonLdValidator(FeatureType type) {
        visitor = new ValidateExpressionVisitor(type);
        this.type = type;
    }

    public JsonLdValidator() {
        visitor = new ValidateExpressionVisitor();
    }

    public boolean validateTemplate(RootBuilder root) {
        return validateExpressions(root, type);
    }

    private boolean validateExpressions(JsonBuilder builder, AttributeType type) {
        for (JsonBuilder jb : builder.getChildren()) {
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                if (djb.getCql() != null) {
                    try {
                        if (!(boolean) djb.getCql().accept(visitor, null)) return false;
                    } catch (Exception e) {
                        return false;
                    }
                } else if (djb.getXpath() != null) {
                    try {
                        if (!(djb.getXpath().accept(visitor, null) != null)) return false;
                    } catch (Exception e) {
                        return false;
                    }
                }
            } else if (jb instanceof SourceBuilder) {
                Object newType = null;
                SourceBuilder sb = ((SourceBuilder) jb);
                if (sb.getSource() != null) {
                    String typeName =
                            sb.getStrSource().substring(sb.getStrSource().indexOf(":") + 1);
                    if (!type.getName().getLocalPart().contains(typeName)) {
                        newType = sb.getSource().accept(visitor, null);
                        if (newType == null) {
                            return false;
                        }
                    }
                }
                validateExpressions(
                        jb, newType != null ? ((AttributeDescriptor) newType).getType() : type);
            }
        }
        return true;
    }
}
