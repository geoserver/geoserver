/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.Objects;
import org.opengis.filter.expression.Expression;

/**
 * Represents an indexable property essential traits:
 *
 * <ul>
 *   <li>The queryable name
 *   <li>The expression backing it
 *   <li>The type of field, influencing how the index is built
 * </ul>
 */
public class Indexable {

    /** Field types, primarily for index creation */
    public enum FieldType {
        JsonString,
        JsonInteger,
        JsonFloat,
        JsonDouble,
        Geometry,
        Array,
        Other
    }

    String queryable;
    Expression expression;
    FieldType fieldType;

    public Indexable(String queryable, Expression expression, FieldType fieldType) {
        this.queryable = queryable;
        this.expression = expression;
        this.fieldType = fieldType;
    }

    public String getQueryable() {
        return queryable;
    }

    public void setQueryable(String queryable) {
        this.queryable = queryable;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Indexable indexable = (Indexable) o;
        return Objects.equals(queryable, indexable.queryable)
                && Objects.equals(expression, indexable.expression)
                && fieldType == indexable.fieldType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryable, expression, fieldType);
    }

    @Override
    public String toString() {
        return "Indexable{"
                + "name='"
                + queryable
                + '\''
                + ", expression="
                + expression
                + ", fieldType="
                + fieldType
                + '}';
    }
}
