/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic.jdbc.custom;

import java.util.Properties;
import org.geotools.util.factory.Hints.Key;

/**
 * A Configuration bean storing datastore properties, tableName, coverageName, table prefixes, file
 * extension
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class JDBCPGrasterConfigurationBean {

    public static final Key CONFIG_KEY = new Key(JDBCPGrasterConfigurationBean.class);

    public JDBCPGrasterConfigurationBean(
            Properties datastoreProperties,
            String tableName,
            String tileTablePrefix,
            String fileExtension,
            String coverageName,
            String importOptions,
            String schema,
            final int epsgCode) {
        this.datastoreProperties = datastoreProperties;
        this.tableName = tableName;
        this.tileTablePrefix = tileTablePrefix;
        this.fileExtension = fileExtension;
        this.coverageName = coverageName;
        this.importOptions = importOptions;
        this.schema = schema;
        this.epsgCode = epsgCode;
    }

    private Properties datastoreProperties;

    private String tableName;

    private String tileTablePrefix;

    private String fileExtension;

    private String coverageName;

    private String importOptions;

    private String schema;

    private int epsgCode;

    public Properties getDatastoreProperties() {
        return datastoreProperties;
    }

    public void setDatastoreProperties(Properties datastoreProperties) {
        this.datastoreProperties = datastoreProperties;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCoverageName() {
        return coverageName;
    }

    public String getTileTablePrefix() {
        return tileTablePrefix;
    }

    public void setTileTablePrefix(String tileTablePrefix) {
        this.tileTablePrefix = tileTablePrefix;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setCoverageName(String coverageName) {
        this.coverageName = coverageName;
    }

    public String getImportOptions() {
        return importOptions;
    }

    public void setImportOptions(String importOptions) {
        this.importOptions = importOptions;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getEpsgCode() {
        return epsgCode;
    }

    public void setEpsgCode(int epsgCode) {
        this.epsgCode = epsgCode;
    }
}
