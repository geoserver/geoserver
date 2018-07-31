package com.boundlessgeo.gsr.translate.feature;

import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.identity.FeatureId;

import java.util.HashSet;

/**
 * Remaps {@link PropertyIsEqualTo} filters of {@link AttributeExpressionImpl} that represent an ESRI OBJECTID to
 * {@link Id} filters where applicable.
 *
 * Also converts OBJECTID=OBJECTID into Filter.INCLUDE
 */
public class ObjectIdRemappingFilterVisitor extends DuplicatingFilterVisitor {

    final String objectId;

    final String featureIdPrefix;

    public ObjectIdRemappingFilterVisitor() {
        this(null, "");
    }

    /**
     * @param objectIdField The property name to be remapped to the id. Defaults to {@link FeatureEncoder#OBJECTID_FIELD_NAME}
     * @param featureIdPrefix The prefix of the GeoTools FeatureId, including any delimiters. Defaults to and empty string
     */
    public ObjectIdRemappingFilterVisitor(String objectIdField, String featureIdPrefix) {
        this.featureIdPrefix = featureIdPrefix;
        if (null == objectIdField || objectIdField.isEmpty()) {
            objectId = FeatureEncoder.OBJECTID_FIELD_NAME;
        } else {
            objectId = objectIdField;
        }
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        boolean matchCase = filter.isMatchingCase();

        if (isIdAttribute(expr1, matchCase) && isValidIdValue(expr2)) {
            return toIdExpression((Literal) expr2, extraData);
        }
        if (isIdAttribute(expr2, matchCase) && isValidIdValue(expr1)) {
            return toIdExpression((Literal) expr2, extraData);
        }
        if (isIdAttribute(expr1, matchCase) && isIdAttribute(expr2, matchCase)) {
            return Filter.INCLUDE;
        }

        return getFactory(extraData).equal(expr1, expr2, matchCase, filter.getMatchAction());
    }

    private boolean isIdAttribute(Expression expr, boolean matchCase) {
        if (expr instanceof AttributeExpressionImpl) {
            AttributeExpressionImpl attr = (AttributeExpressionImpl) expr;
            if (attr.getPropertyName().equals(objectId)
                    || (!matchCase && attr.getPropertyName().equalsIgnoreCase(objectId))) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidIdValue(Expression expr) {
        return expr instanceof Literal;
    }

    public Id toIdExpression(Literal value, Object extraData) {
        FilterFactory factory = getFactory(extraData);
        HashSet<FeatureId> ids = new HashSet<>();
        ids.add(factory.featureId(featureIdPrefix + value.getValue().toString()));
        return factory.id(ids);
    }

}
