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

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(data.conn)) {
            return iface.cast(data.conn);
        }
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(data.conn)) {
            return true;
        }
        return false;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return data.conn.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return data.conn.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return data.conn.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return data.conn.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        data.conn.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return data.conn.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        data.conn.commit();
    }

    @Override
    public void rollback() throws SQLException {
        data.conn.rollback();
    }

    @Override
    public void close() throws SQLException {
        data.conn.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return data.conn.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return data.conn.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        data.conn.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return data.conn.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        data.conn.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return data.conn.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        data.conn.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return data.conn.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return data.conn.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        data.conn.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return data.conn.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return data.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return data.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return data.conn.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        data.conn.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        data.conn.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return data.conn.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return data.conn.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return data.conn.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        data.conn.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        data.conn.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.prepareStatement(
                sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return data.conn.prepareCall(
                sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return data.conn.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return data.conn.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return data.conn.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return data.conn.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return data.conn.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return data.conn.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return data.conn.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return data.conn.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        data.conn.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        data.conn.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return data.conn.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return data.conn.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return data.conn.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return data.conn.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        data.conn.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return data.conn.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        data.conn.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        data.conn.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return data.conn.getNetworkTimeout();
    }
}
