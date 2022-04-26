package org.geoserver.opensearch.eo.store;

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
}
