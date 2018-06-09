/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/** Sample wrapper for testing */
class WrapperConnection implements Connection {

    private WrapperConnectionData data = new WrapperConnectionData();

    WrapperConnection(Connection conn) {
        this.data.conn = conn;
    }

    public Connection getUnderlyingConnection() {
        return data.conn;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(data.conn)) {
            return iface.cast(data.conn);
        }
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(data.conn)) {
            return true;
        }
        return false;
    }

    public Statement createStatement() throws SQLException {
        return data.conn.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return data.conn.prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return data.conn.prepareCall(sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return data.conn.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        data.conn.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return data.conn.getAutoCommit();
    }

    public void commit() throws SQLException {
        data.conn.commit();
    }

    public void rollback() throws SQLException {
        data.conn.rollback();
    }

    public void close() throws SQLException {
        data.conn.close();
    }

    public boolean isClosed() throws SQLException {
        return data.conn.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return data.conn.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        data.conn.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return data.conn.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        data.conn.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return data.conn.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        data.conn.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return data.conn.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return data.conn.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        data.conn.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return data.conn.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return data.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return data.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return data.conn.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        data.conn.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        data.conn.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return data.conn.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return data.conn.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return data.conn.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        data.conn.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        data.conn.releaseSavepoint(savepoint);
    }

    public Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.prepareStatement(
                sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.prepareCall(
                sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return data.conn.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return data.conn.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return data.conn.prepareStatement(sql, columnNames);
    }

    public Clob createClob() throws SQLException {
        return data.conn.createClob();
    }

    public Blob createBlob() throws SQLException {
        return data.conn.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return data.conn.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return data.conn.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return data.conn.isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        data.conn.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        data.conn.setClientInfo(properties);
    }

    public String getClientInfo(String name) throws SQLException {
        return data.conn.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        return data.conn.getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return data.conn.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return data.conn.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        data.conn.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return data.conn.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        data.conn.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        data.conn.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return data.conn.getNetworkTimeout();
    }
}
