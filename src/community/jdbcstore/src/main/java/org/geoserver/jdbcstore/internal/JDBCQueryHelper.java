/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.internal;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.io.input.ProxyInputStream;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Convencience classes and methods for JDBC Access and building queries.
 *
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class JDBCQueryHelper {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(JDBCQueryHelper.class);

    /**
     * Query DataType
     *
     * @param <T>
     */
    public static interface Type<T> {
        void setValue(PreparedStatement s, int index, T value) throws SQLException;

        T getValue(ResultSet rs, String colName) throws SQLException;
    }

    public static final Type<Integer> TYPE_INT =
            new Type<Integer>() {
                @Override
                public void setValue(PreparedStatement s, int index, Integer value)
                        throws SQLException {
                    s.setInt(index, value);
                }

                @Override
                public Integer getValue(ResultSet rs, String colName) throws SQLException {
                    int i = rs.getInt(colName);
                    if (rs.wasNull()) return null;
                    return i;
                }
            };

    public static final Type<Boolean> TYPE_BOOLEAN =
            new Type<Boolean>() {
                @Override
                public void setValue(PreparedStatement s, int index, Boolean value)
                        throws SQLException {
                    s.setInt(index, index);
                }

                @Override
                public Boolean getValue(ResultSet rs, String colName) throws SQLException {
                    boolean b = rs.getBoolean(colName);
                    if (rs.wasNull()) return null;
                    return b;
                }
            };

    public static final Type<String> TYPE_STRING =
            new Type<String>() {
                @Override
                public void setValue(PreparedStatement s, int index, String value)
                        throws SQLException {
                    s.setString(index, value);
                }

                @Override
                public String getValue(ResultSet rs, String colName) throws SQLException {
                    return rs.getString(colName);
                }
            };

    public static final Type<Timestamp> TYPE_TIMESTAMP =
            new Type<Timestamp>() {
                @Override
                public void setValue(PreparedStatement s, int index, Timestamp value)
                        throws SQLException {
                    s.setTimestamp(index, value);
                }

                @Override
                public Timestamp getValue(ResultSet rs, String colName) throws SQLException {
                    return rs.getTimestamp(colName);
                }
            };

    public static final Type<InputStream> TYPE_BLOB =
            new Type<InputStream>() {
                @Override
                public void setValue(PreparedStatement s, int index, InputStream value)
                        throws SQLException {
                    s.setBinaryStream(index, value);
                }

                @Override
                public InputStream getValue(ResultSet rs, String colName) throws SQLException {
                    return rs.getBinaryStream(colName);
                }
            };

    /**
     * Query Field.
     *
     * @param <T>
     */
    public static class Field<T> {
        private final String fieldName;
        private final String fieldExpression;
        private final Type<T> type;

        public Field(String fieldName, String fieldExpression, Type<T> type) {
            super();
            this.fieldName = fieldName;
            this.fieldExpression = fieldExpression;
            this.type = type;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldExpression() {
            return fieldExpression;
        }

        public Type<T> getType() {
            return type;
        }

        public T getValue(ResultSet rs) throws SQLException {
            return type.getValue(rs, fieldName);
        }
    }

    /**
     * Query parameter.
     *
     * @param <T>
     */
    public static class Parameter<T> {
        private final Type<T> type;
        private final T value;

        public Parameter(Type<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        public Type<T> getType() {
            return type;
        }

        public T getValue() {
            return value;
        }

        public void setValue(PreparedStatement s, int index) throws SQLException {
            type.setValue(s, index, value);
        }
    }

    /** Builds a prepared statement from a query */
    public static class QueryBuilder {
        private StringBuilder stringBuilder = new StringBuilder();
        private ArrayList<Parameter<?>> parameters = new ArrayList<Parameter<?>>();

        public QueryBuilder() {}

        public QueryBuilder(String string) {
            stringBuilder.append(string);
        }

        public void append(String s) {
            stringBuilder.append(s);
        }

        public void addParameter(Parameter<?> parameter) {
            parameters.add(parameter);
        }

        private void parameters(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < parameters.size(); i++) {
                parameters.get(i).setValue(ps, i + 1);
            }
        }

        public PreparedStatement toStatement(Connection c) throws SQLException {
            PreparedStatement ps = c.prepareStatement(stringBuilder.toString());
            try {
                parameters(ps);
                return ps;
            } catch (SQLException e) {
                ps.close();
                throw e;
            }
        }

        public PreparedStatement toStatement(Connection c, int autoGeneratedKeys)
                throws SQLException {
            PreparedStatement ps = c.prepareStatement(stringBuilder.toString(), autoGeneratedKeys);
            try {
                parameters(ps);
                return ps;
            } catch (SQLException e) {
                ps.close();
                throw e;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(stringBuilder.toString());
            for (Parameter<?> pam : parameters) {
                sb.append(": " + pam.getValue());
            }
            return sb.toString();
        }
    }

    /** Different ways to provide filter part of the query. */
    public static interface Selector {
        QueryBuilder appendCondition(QueryBuilder qb);
    }

    /** Filter or field. */
    public static class FieldSelector<T> implements Selector {
        private final Field<T> field;
        private final T value;

        FieldSelector(Field<T> field, T value) {
            this.field = field;
            this.value = value;
        }

        @Override
        public QueryBuilder appendCondition(QueryBuilder sb) {
            sb.append(field.getFieldExpression() + " = ?");
            sb.addParameter(new Parameter<T>(field.getType(), value));
            return sb;
        }
    }

    /**
     * Provides an assignment for an update query.
     *
     * @param <T>
     */
    protected static class Assignment<T> {
        private final Field<T> field;
        private final T value;

        public Assignment(Field<T> field, T value) {
            this.field = field;
            this.value = value;
        }

        public Field<T> getField() {
            return field;
        }

        public T getValue() {
            return value;
        }

        public QueryBuilder appendAssignment(QueryBuilder sb) {
            sb.append(field.getFieldName() + " = ? ");
            return addAsParameter(sb);
        }

        public QueryBuilder addAsParameter(QueryBuilder sb) {
            sb.addParameter(new Parameter<T>(field.getType(), value));
            return sb;
        }
    }

    private final DataSource ds;

    public JDBCQueryHelper(DataSource ds) {
        this.ds = ds;
    }

    private QueryBuilder createSelect(String table, Selector sel, Field<?>... fields) {
        QueryBuilder builder = new QueryBuilder();

        builder.append("SELECT ");
        {
            int i = 0;
            for (Field<?> field : fields) {
                if (i++ > 0) builder.append(", ");
                builder.append(field.fieldExpression);
            }
        }
        builder.append(" FROM " + table + " WHERE ");
        sel.appendCondition(builder);
        builder.append(";");

        return builder;
    }

    public Map<String, Object> selectQuery(String table, Selector sel, Field<?>... fields) {
        return anyQuery(createSelect(table, sel, fields), fields);
    }

    public List<Map<String, Object>> multiSelectQuery(
            String table, Selector sel, Field<?>... fields) {
        return anyMultiQuery(createSelect(table, sel, fields), fields);
    }

    public InputStream blobQuery(String table, Selector sel, Field<InputStream> field) {
        return anyBlobQuery(createSelect(table, sel, field), field);
    }

    public int deleteQuery(String table, Selector sel) {
        QueryBuilder builder = new QueryBuilder();

        builder.append("DELETE FROM " + table + " WHERE ");
        sel.appendCondition(builder);
        builder.append(";");

        return anyUpdateQuery(builder);
    }

    public int updateQuery(String table, Selector sel, Assignment<?>... assignments) {
        QueryBuilder builder = new QueryBuilder();

        builder.append("UPDATE " + table + " SET ");
        {
            int i = 0;
            for (Assignment<?> assign : assignments) {
                if (i++ > 0) builder.append(", ");
                assign.appendAssignment(builder);
            }
        }
        builder.append(" WHERE ");
        sel.appendCondition(builder);
        builder.append(";");

        return anyUpdateQuery(builder);
    }

    public Integer insertQuery(String table, Assignment<?>... assignments) {
        QueryBuilder builder = new QueryBuilder();

        builder.append("INSERT INTO " + table + "( ");
        {
            int i = 0;
            for (Assignment<?> assign : assignments) {
                if (i++ > 0) builder.append(", ");
                builder.append(assign.getField().getFieldName());
            }
        }
        builder.append(") VALUES ( ");
        {
            int i = 0;
            for (Assignment<?> assign : assignments) {
                if (i++ > 0) builder.append(", ");
                builder.append("?");
                assign.addAsParameter(builder);
            }
        }
        builder.append(");");

        List<Integer> result = anyInsertQuery(builder);
        assert (result == null || result.size() == 1);
        return result == null ? null : result.get(0);
    }

    public List<Map<String, Object>> anyMultiQuery(QueryBuilder query, Field<?>... fields) {
        LOGGER.log(Level.FINEST, query.toString());

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement stmt = query.toStatement(c)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                    while (rs.next()) {
                        Map<String, Object> result = new HashMap<String, Object>();
                        for (int i = 0; i < fields.length; i++) {
                            result.put(fields[i].getFieldName(), fields[i].getValue(rs));
                        }
                        results.add(result);
                    }
                    return results;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("MultiSelectQuery Failed", ex);
        }
    }

    public Map<String, Object> anyQuery(QueryBuilder query, Field<?>... fields) {
        LOGGER.log(Level.FINEST, query.toString());

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement stmt = query.toStatement(c)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        assert (rs.isLast());
                        Map<String, Object> result = new HashMap<String, Object>();
                        for (int i = 0; i < fields.length; i++) {
                            result.put(fields[i].getFieldName(), fields[i].getValue(rs));
                        }
                        return result;
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("SelectQuery Failed", ex);
        }
    }

    public int anyUpdateQuery(QueryBuilder query) {
        LOGGER.log(Level.FINEST, query.toString());

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement stmt = query.toStatement(c)) {
                return stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("UpdateQuery Failed", ex);
        }
    }

    public List<Integer> anyInsertQuery(QueryBuilder query) {
        LOGGER.log(Level.FINEST, query.toString());

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement stmt = query.toStatement(c, Statement.RETURN_GENERATED_KEYS)) {
                if (stmt.executeUpdate() <= 0) {
                    return null;
                }

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    List<Integer> list = new ArrayList<Integer>();
                    while (rs.next()) {
                        list.add(rs.getInt(1));
                    }
                    return list;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("InsertQuery Failed", ex);
        }
    }

    /** Blobs should always be queried with this method, not with the regular selects! */
    public InputStream anyBlobQuery(QueryBuilder query, Field<InputStream> field) {
        LOGGER.log(Level.FINEST, query.toString());

        Connection c;
        boolean closeConnection = false;
        try {
            c = ds.getConnection();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not connect to DataSource.", ex);
        }
        try {
            try (PreparedStatement stmt = query.toStatement(c)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        assert (rs.isLast());
                        InputStream is = field.getValue(rs);
                        return is == null ? null : new ClosingInputStreamWrapper(is, c);
                    } else {
                        closeConnection = true;
                        return null;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("BlobQuery Failed", ex);
        } finally {
            if (closeConnection) {
                try {
                    c.close();
                } catch (SQLException ex) {
                    throw new IllegalArgumentException("Error while closing connection.", ex);
                }
            }
        }
    }

    protected void runScript(Resource script) {
        NamedParameterJdbcOperations template = new NamedParameterJdbcTemplate(ds);

        try (InputStream in = script.in()) {
            Util.runScript(in, template.getJdbcOperations(), null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not execute provided sql script", ex);
        }
    }

    /** Ensures the connection is closed after returned inputstream of BLOB is closed. */
    protected static class ClosingInputStreamWrapper extends ProxyInputStream {
        Connection conn;

        public ClosingInputStreamWrapper(InputStream proxy, Connection conn) {
            super(proxy);
            this.conn = conn;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new IOException("Exception while closing connection", ex);
                }
            }
        }
    }
}
