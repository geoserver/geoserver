/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.imagemosaic.jdbc;

/**
 * This class implements the db dialect for postgis
 *
 * @author mcr
 */
public class PostgisDialect extends DBDialect {
    public PostgisDialect(Config config) {
        super(config);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getRegisterSpatialStatement(java.lang.String,
     *      java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getUnregisterSpatialStatement(java.lang.String)
     */
    @Override
    protected String getUnregisterSpatialStatement(String tn) {
        return "select DropGeometryColumn('"
                + tn
                + "','"
                + getConfig().getGeomAttributeNameInSpatialTable()
                + "')";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getCreateSpatialTableStatement(java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getCreateSpatialTableStatementJoined(java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getBLOBSQLType()
     */
    @Override
    protected String getBLOBSQLType() {
        return "BYTEA";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getMultiPolygonSQLType()
     */
    @Override
    protected String getMultiPolygonSQLType() {
        return "MULTIPOLYGON";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getDoubleSQLType()
     */
    @Override
    protected String getDoubleSQLType() {
        return "FLOAT8";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geotools.gce.imagemosaic.jdbc.DBDialect#getCreateIndexStatement(java.lang.String)
     */
    @Override
    protected String getCreateIndexStatement(String tn) throws Exception {
        return "CREATE INDEX IX_"
                + tn
                + " ON "
                + tn
                + " USING gist("
                + getConfig().getGeomAttributeNameInSpatialTable()
                + ") ";
    }
}
