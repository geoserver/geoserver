/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serial;
import java.security.InvalidParameterException;
import org.geoserver.importer.ImportTask;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;

/** Transform creating a new attribute based on the existing ones */
public class AttributeComputeTransform extends AbstractTransform implements InlineVectorTransform {

    @Serial
    private static final long serialVersionUID = 1L;

    /** field to remap */
    protected String field;

    /** type to remap to */
    protected Class<?> type;

    /** the expression to apply (stored as a string as CQL does not always round trip properly */
    protected String cql;

    protected transient Expression expression;

    public AttributeComputeTransform(String field, Class<?> type, String cql) throws CQLException {
        this.field = field;
        this.type = type;
        setCql(cql);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) throws CQLException {
        this.cql = cql;
        this.expression = ECQL.toExpression(cql);
    }

    Expression getExpression() throws CQLException {
        if (expression == null && cql != null) {
            expression = ECQL.toExpression(cql);
        }
        return expression;
    }

    @Override
    public SimpleFeatureType apply(ImportTask task, DataStore dataStore, SimpleFeatureType featureType)
            throws Exception {
        // validate the target attribute is not already there
        if (featureType.getDescriptor(field) != null) {
            throw new InvalidParameterException(
                    "The computed attribute " + field + " is already present in the " + "source feature type");
        }

        // remap the type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);
        builder.add(field, type);

        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object value = getExpression().evaluate(oldFeature);
        feature.setAttribute(field, value);

        return feature;
    }

    @Override
    public String toString() {
        return "AttributeComputeTransform{"
                + "field='"
                + field
                + '\''
                + ", type="
                + type
                + ", cql='"
                + cql
                + '\''
                + ", expression="
                + expression
                + '}';
    }
}
