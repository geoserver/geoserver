/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.Objects;

/**
 * In memory representation of the information stored in the index tracking table. It's the
 * persistent parallel to an {@link Indexable}.
 */
class JDBCIndex {

    String collection;
    String queryable;

    String expression;
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getQueryable() {
        return queryable;
    }

    public void setQueryable(String queryable) {
        this.queryable = queryable;
    }

    @Override
    public String toString() {
        return "JDBCIndex{"
                + "collection='"
                + collection
                + '\''
                + ", queryable='"
                + queryable
                + '\''
                + ", expression='"
                + expression
                + '\''
                + ", name='"
                + name
                + '\''
                + '}';
    }

    /** Checks whether this JDBCIndex matches the given queryable and indexed expression */
    public boolean matches(String queryable, String expression) {
        return Objects.equals(this.queryable, queryable)
                && Objects.equals(this.expression, expression);
    }
}
