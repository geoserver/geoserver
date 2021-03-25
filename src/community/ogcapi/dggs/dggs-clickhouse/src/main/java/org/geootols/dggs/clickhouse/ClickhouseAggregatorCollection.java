package org.geootols.dggs.clickhouse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.CloseableIterator;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.dggs.GroupedMatrixAggregate;
import org.geotools.dggs.GroupedMatrixAggregate.GroupByResult;
import org.geotools.dggs.MatrixAggregate;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PrimaryKey;
import org.geotools.jdbc.SQLDialect;
import org.geotools.util.logging.Logging;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

public class ClickhouseAggregatorCollection extends DecoratingSimpleFeatureCollection {

    static final Logger LOGGER = Logging.getLogger(ClickhouseAggregatorCollection.class);

    private final SimpleFeatureType featureType;
    private final Query query;
    private final JDBCDataStore store;

    public ClickhouseAggregatorCollection(
            SimpleFeatureCollection delegate,
            JDBCDataStore store,
            Query query,
            SimpleFeatureType featureType) {
        super(delegate);
        this.store = store;
        this.query = query;
        this.featureType = featureType;
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        if (visitor instanceof MatrixAggregate) {
            if (visit((MatrixAggregate) visitor)) return;
        } else if (visitor instanceof GroupedMatrixAggregate) {
            if (visit((GroupedMatrixAggregate) visitor)) return;
        } else {
            delegate.accepts(visitor, progress);
        }
    }

    private boolean visit(MatrixAggregate visitor) throws IOException {
        try {
            // is it possible to encode all functions?
            List<FeatureCalc> calculators = visitor.getCalculators();
            for (FeatureCalc fc : calculators) {
                if (matchAggregateFunction(fc) == null) return false;
            }

            SQLDialect dialect = store.getSQLDialect();
            FilterToSQL f2s = createFilterToSQL(featureType);
            f2s.setInline(true);

            // simple encoding, no limit/offset, no join support, no views,
            // but supports multiple attributes and expressions are supported.
            // TODO: move this to JDBCDataStore
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT ");
            for (FeatureCalc fc : calculators) {
                String function = matchAggregateFunction(fc);
                sql.append(function).append("(");
                Expression expression = getExpression(fc);
                if (expression == null) sql.append("*");
                else sql.append(f2s.encodeToString(expression));
                sql.append("), ");
            }
            sql.setLength(sql.length() - 2);
            sql.append(" FROM ");
            if (store.getDatabaseSchema() != null) {
                dialect.encodeSchemaName(store.getDatabaseSchema(), sql);
                sql.append(".");
            }
            dialect.encodeTableName(featureType.getTypeName(), sql);
            Filter filter = query.getFilter();
            if (filter != Filter.INCLUDE) {
                sql.append(" WHERE ");
                sql.append(f2s.encodeToString(filter));
            }

            try (Connection cx = store.getConnection(Transaction.AUTO_COMMIT);
                    Statement st = cx.createStatement();
                    ResultSet rs = st.executeQuery(sql.toString())) {
                if (!rs.next()) return false;
                List<Object> results = new ArrayList<>();
                for (int i = 1; i <= calculators.size(); i++) {
                    results.add(rs.getObject(i));
                }
                visitor.setResults(results);
                return true;
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private boolean visit(GroupedMatrixAggregate visitor) throws IOException {
        try {
            // is it possible to encode all functions?
            List<FeatureCalc> calculators = visitor.getCalculators();
            for (FeatureCalc fc : calculators) {
                if (matchAggregateFunction(fc) == null) return false;
            }

            SQLDialect dialect = store.getSQLDialect();
            FilterToSQL f2s = createFilterToSQL(featureType);
            f2s.setInline(true);

            // simple encoding, no limit/offset, no join support, no views,
            // but supports multiple attributes and expressions are supported.
            // TODO: move this to JDBCDataStore
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT ");
            List<Expression> groupByExpressions = visitor.getGroupBy();
            for (Expression groupBy : groupByExpressions) {
                sql.append(f2s.encodeToString(groupBy));
                sql.append(", ");
            }
            for (FeatureCalc fc : calculators) {
                String function = matchAggregateFunction(fc);
                sql.append(function).append("(");
                Expression expression = getExpression(fc);
                if (expression == null) sql.append("*");
                else sql.append(f2s.encodeToString(expression));
                sql.append("), ");
            }
            sql.setLength(sql.length() - 2);
            sql.append(" FROM ");
            if (store.getDatabaseSchema() != null) {
                dialect.encodeSchemaName(store.getDatabaseSchema(), sql);
                sql.append(".");
            }
            dialect.encodeTableName(featureType.getTypeName(), sql);
            Filter filter = query.getFilter();
            if (filter != Filter.INCLUDE) {
                sql.append(" WHERE ");
                sql.append(f2s.encodeToString(filter));
            }
            sql.append(" GROUP BY ");
            for (Expression groupBy : groupByExpressions) {
                sql.append(f2s.encodeToString(groupBy));
                sql.append(", ");
            }
            sql.setLength(sql.length() - 2);

            visitor.setResults(
                    new GroupedMatrixAggregate.IterableResult(
                            () ->
                                    new GroupedIterableResult(
                                            store,
                                            sql,
                                            groupByExpressions.size(),
                                            calculators.size())));
            return true;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static class GroupedIterableResult implements CloseableIterator<GroupByResult> {

        Connection cx;
        Statement st;
        ResultSet rs;
        Boolean next;
        int groupSize;
        int resultSize;

        public GroupedIterableResult(
                JDBCDataStore store, StringBuffer sql, int groupSize, int resultSize) {
            try {
                this.cx = store.getConnection(Transaction.AUTO_COMMIT);
                this.st = cx.createStatement();
                this.rs = st.executeQuery(sql.toString());
                this.groupSize = groupSize;
                this.resultSize = resultSize;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                try {
                    if (rs != null) rs.close();
                } finally {
                    try {
                        if (st != null) st.close();
                    } finally {
                        if (cx != null) cx.close();
                    }
                }
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                try {
                    next = rs.next();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to compute next aggregation result", e);
                    return false;
                }
            }
            return next;
        }

        @Override
        public GroupByResult next() {
            if (!hasNext()) throw new NoSuchElementException();
            this.next = null;
            try {
                List<Object> key = new ArrayList<>();
                for (int i = 0; i < groupSize; i++) {
                    key.add(rs.getObject(i + 1));
                }
                List<Object> values = new ArrayList<>();
                for (int i = 0; i < resultSize; i++) {
                    values.add(rs.getObject(groupSize + i + 1));
                }
                return new GroupByResult(key, values);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates a new instance of a filter to sql encoder.
     *
     * <p>The <tt>featureType</tt> may be null but it is not recommended. Such a case where this may
     * neccessary is when a literal needs to be encoded in isolation.
     */
    public FilterToSQL createFilterToSQL(SimpleFeatureType featureType) {
        return initializeFilterToSQL(
                ((BasicSQLDialect) store.getSQLDialect()).createFilterToSQL(), featureType);
    }

    /** Helper method to initialize a filter encoder instance. */
    protected <F extends FilterToSQL> F initializeFilterToSQL(
            F toSQL, final SimpleFeatureType featureType) {
        toSQL.setSqlNameEscape(store.getSQLDialect().getNameEscape());

        if (featureType != null) {
            // set up a fid mapper
            // TODO: remove this
            final PrimaryKey key;

            try {
                key = store.getPrimaryKey(featureType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            toSQL.setFeatureType(featureType);
            toSQL.setPrimaryKey(key);
            toSQL.setDatabaseSchema(store.getDatabaseSchema());
        }

        return toSQL;
    }

    protected String matchAggregateFunction(FeatureVisitor visitor) {
        Class<?> visitorClass = visitor.getClass();
        String function = null;
        // try to find a matching aggregate function walking up the hierarchy if necessary
        while (function == null && visitorClass != null) {
            function = store.getAggregateFunctions().get(visitorClass);
            visitorClass = visitorClass.getSuperclass();
        }
        if (function == null) {
            // this visitor don't match any aggregate function NULL will be returned
            LOGGER.info(
                    "Unable to find aggregate function matching visitor: " + visitor.getClass());
        }
        return function;
    }

    /**
     * Helper method for getting the expression from a visitor TODO: Remove this method when there
     * is an interface for aggregate visitors. See GEOT-2325 for details.
     */
    Expression getExpression(FeatureVisitor visitor) {
        if (visitor instanceof CountVisitor) {
            return null;
        }
        try {
            Method g = visitor.getClass().getMethod("getExpression", null);
            if (g != null) {
                Object result = g.invoke(visitor, null);
                if (result instanceof Expression) {
                    return (Expression) result;
                }
            }
        } catch (Exception e) {
            // ignore for now
        }

        return null;
    }
}
