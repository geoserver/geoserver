/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.geotools.gce.pgraster.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.geotools.data.jdbc.datasource.DataSourceFinder;

/**
 * This class is the base class for the different sql dialects used in spatial extensions form
 * different vendors
 *
 * @author mcr
 * @since 2.5
 */
public class DBDialect {
    protected DataSource dataSource;

    protected Config config;

    /** Constructor */
    public DBDialect(Config config) {
        this.config = config;
    }

    /** Factory method for obtaining a DBDialect object for a special spatial extension */
    public static DBDialect getDBDialect(Config config) {
        SpatialExtension type = config.getSpatialExtension();
        if (type == SpatialExtension.PGRASTER) {
            return new DBDialect(config);
        }
        return null;
    }

    /** @return the sql type name for a blob (Binary Large Object) */
    protected String getBLOBSQLType() {
        return "BYTEA";
    }

    /** @return the sql type name for a Multipolygon */
    protected String getMultiPolygonSQLType() {
        return "MULTIPOLYGON";
    }

    /** @return sql datatype for 8 byte floating point */
    protected String getDoubleSQLType() {
        return "FLOAT8";
    }

    /** @return the config object for this dialect */
    protected Config getConfig() {
        return config;
    }

    /** @return datasource for this dialect object */
    private DataSource getDataSource() throws Exception {
        if (dataSource != null) {
            return dataSource;
        }

        Config config = getConfig();
        dataSource = DataSourceFinder.getDataSource(config.getDataSourceParams());

        return dataSource;
    }

    /** @return jdbc connection */
    public Connection getConnection() throws Exception {
        Connection con = getDataSource().getConnection();
        con.setAutoCommit(false);

        return con;
    }

    /** @return sql drop table statement for tableName */
    String getDropTableStatement(String tableName) {
        return "drop table " + tableName;
    }

    /**
     * @param tn sql table name
     * @return sql unregister spatial column statement for nt
     */
    protected String getUnregisterSpatialStatement(String tn) {
        return "select DropGeometryColumn('"
                + tn
                + "','"
                + getConfig().getGeomAttributeNameInSpatialTable()
                + "')";
    }

    /**
     * @param tn sql table name
     * @param srs name of spatial reference system to use
     * @return sql unregister spatial column statement for nt
     */
    protected String getRegisterSpatialStatement(String tn, String srs) {
        return "select AddGeometryColumn('"
                + tn
                + "','"
                + config.getGeomAttributeNameInSpatialTable()
                + "',"
                + srs
                + ",'"
                + getMultiPolygonSQLType()
                + "',2)";
    }

    /**
     * @param tn sql table name
     * @return sql create spatial index statement for tn
     */
    protected String getCreateIndexStatement(String tn) throws Exception {
        return "CREATE INDEX IX_"
                + tn
                + " ON "
                + tn
                + " USING gist("
                + getConfig().getGeomAttributeNameInSpatialTable()
                + ") ";
    }

    /** @return sql drop index statement */
    String getDropIndexStatment(String tn) {
        return "drop index IX_" + tn;
    }

    /** @return the create table statement for the master table */
    String getCreateMasterStatement() throws Exception {
        Config config = getConfig();
        String doubleType = getDoubleSQLType();
        String statement = "CREATE TABLE " + config.getMasterTable();
        statement += ("(" + config.getCoverageNameAttribute() + " CHARACTER (64)  NOT NULL");
        statement += ("," + config.getSpatialTableNameAtribute() + " VARCHAR (128)  NOT NULL");
        statement += ("," + config.getTileTableNameAtribute() + " VARCHAR (128)  NOT NULL");
        statement +=
                (","
                        + config.getResXAttribute()
                        + " "
                        + doubleType
                        + ","
                        + config.getResYAttribute()
                        + " "
                        + doubleType);
        statement +=
                (","
                        + config.getMinXAttribute()
                        + " "
                        + doubleType
                        + ","
                        + config.getMinYAttribute()
                        + " "
                        + doubleType);
        statement +=
                (","
                        + config.getMaxXAttribute()
                        + " "
                        + doubleType
                        + ","
                        + config.getMaxYAttribute()
                        + " "
                        + doubleType);
        statement += ",CONSTRAINT MASTER_PK PRIMARY KEY (";
        statement +=
                (config.getCoverageNameAttribute()
                        + ","
                        + config.getSpatialTableNameAtribute()
                        + ","
                        + config.getTileTableNameAtribute());
        statement += "))";

        return statement;
    }

    /** @return the create table statment for a tile table named tableName */
    String getCreateTileTableStatement(String tableName) throws Exception {
        String statement = "CREATE TABLE " + tableName;
        statement += ("(" + getConfig().getKeyAttributeNameInTileTable() + " CHAR(64) NOT NULL ");
        statement += ("," + getConfig().getBlobAttributeNameInTileTable() + " " + getBLOBSQLType());
        statement +=
                (",CONSTRAINT "
                        + tableName
                        + "_PK PRIMARY KEY("
                        + getConfig().getKeyAttributeNameInTileTable());
        statement += "))";

        return statement;
    }

    /** @return the sql create table statement for a spatial table */
    protected String getCreateSpatialTableStatement(String tableName) throws Exception {
        String statement = " CREATE TABLE " + tableName;
        statement +=
                (" (" + getConfig().getKeyAttributeNameInSpatialTable() + " CHAR(64) NOT NULL ");
        statement +=
                (",CONSTRAINT "
                        + tableName
                        + "_PK PRIMARY KEY("
                        + getConfig().getKeyAttributeNameInSpatialTable());
        statement += "))";

        return statement;
    }

    /** @return the sql create table statement for a combined spatial/tile table named tableName */
    protected String getCreateSpatialTableStatementJoined(String tableName) throws Exception {
        String statement = " CREATE TABLE " + tableName;
        statement +=
                (" (" + getConfig().getKeyAttributeNameInSpatialTable() + " CHAR(64) NOT NULL ");
        statement += ("," + getConfig().getBlobAttributeNameInTileTable() + " " + getBLOBSQLType());
        statement +=
                (",CONSTRAINT "
                        + tableName
                        + "_PK PRIMARY KEY("
                        + getConfig().getKeyAttributeNameInSpatialTable());
        statement += "))";

        return statement;
    }
}
