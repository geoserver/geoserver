/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2013, Open Source Geospatial Foundation (OSGeo)
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

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.gce.pgraster.PostgisRasterGridCoverage2DReader;
import org.geotools.util.URLs;
import org.geotools.util.Utilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * A JDBC PGRaster configuration builder class. It allows to configure a PGRaster based
 * ImageMosaicJDBC starting from a configuration bean specifying PG credentials, database, table,
 * coverage name and files extensions
 *
 * <p>The configuration may be specified through an input string with the following format:
 * {@literal pgraster://USER:PASS@HOST:PORT:DATABASE.SCHEMA.TABLE@EPSGCODE}, or through a {@link
 * JDBCPGrasterConfigurationBean} instance.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class JDBCPGRasterConfigurationBuilder {

    /**
     * A configuration builder constructor
     *
     * @param configBean the configuration bean
     * @param configDir the directory containing data to be configured
     */
    public JDBCPGRasterConfigurationBuilder(
            JDBCPGrasterConfigurationBean configBean, URL configDir) {
        this.configBean = configBean;
        this.configDir = configDir;
    }

    // Simple constants KEYs
    private static final String MOSAIC_KEY = "$MASTER_TABLE";

    private static final String COVERAGE_KEY = "$COVERAGE_NAME";

    private static final String EPSG_CODE_KEY = "$EPSG_CODE";

    private static final String DATABASE_KEY = "$DATABASE";

    private static final String PGUSER_KEY = "$USER";

    private static final String PASSWORD_KEY = "$PASSWORD";

    private static final String PORT_KEY = "$PORT";

    private static final String HOST_KEY = "$HOST";

    private static final String TABLE_CREATION_SQL =
            "create table if not exists "
                    + MOSAIC_KEY
                    + " (NAME varchar(254) not null,"
                    + "TileTable varchar(254)not null, minX FLOAT8, minY FLOAT8, maxX FLOAT8, maxY FLOAT8, resX FLOAT8, resY FLOAT8,"
                    + "primary key (NAME,TileTable))";

    private static final String TABLE_CHECK_SQL = "SELECT tiletable FROM " + MOSAIC_KEY;

    private static final String TILE_TABLE_CHECK_SQL = "SELECT COUNT(rid) FROM " + MOSAIC_KEY;

    // private static final String TILETABLE_INSERTION_SQL =
    // "insert into " + MOSAIC_KEY + " (NAME,TileTable) values (?,?)";

    private static final Logger LOGGER = Logging.getLogger(PostgisRasterGridCoverage2DReader.class);

    private static final String TEMPLATE_FILE_NAME = "coverage.pgraster.template.xml";

    private static final int DEFAULT_EPSG_CODE = 4326;

    private static String TEMPLATE;

    private JDBCPGrasterConfigurationBean configBean;

    private URL configDir;

    static {
        initTemplate();
    }

    /** Initialize the string template parsing the available resource */
    private static void initTemplate() {
        URL resource = JDBCPGRasterConfigurationBuilder.class.getResource(TEMPLATE_FILE_NAME);
        try {
            TEMPLATE =
                    Resources.readLines(resource, StandardCharsets.UTF_8).stream()
                            .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Prepare the PGRaster configuration steps: - importing tiles into the DB and create metadata
     * table if needed - prepare the URL of the XML configuration file and return that URL.
     *
     * <p>The input url String may represent: - 1) the path of the folder containing tiles to be
     * mosaicked, previously generated with gdal_retile. 2) an inline configuration specification in
     * that form:
     * pgraster://USER:PASS@HOST:PORT:DATABASE.SCHEMA.TABLE@EPSGCODE:*.FILE_EXTENSION?OPTIONS#/PATH/TO/RASTER_TILES/"
     *
     * @param url a String referring to the mosaic to be configured.
     * @param hints hints containing a {@link JDBCPGrasterConfigurationBean} bean in case the url
     *     string doesn't specify the configuration params inline.
     * @return the URL of the generated XML file containing the ImageMosaicJDBC configuration
     *     (mapping + connection + coverage configs)
     */
    public static URL createConfiguration(final String url, final Hints hints) {
        Utilities.ensureNonNull("url", url);
        URL configUrl = null;
        try {
            if (url.startsWith("pgraster")) {
                // Parse the string containing configuration inline
                JDBCPGrasterConfigurationBean config = parseConfig(url);
                final int fileURLIndex = url.indexOf("#");
                final String dataUrl = url.substring(fileURLIndex + 1);
                final String fileUrl = dataUrl.startsWith("file:/") ? dataUrl : "file://" + dataUrl;
                configUrl =
                        new JDBCPGRasterConfigurationBuilder(config, new URL(fileUrl))
                                .buildConfiguration();
            } else if (hints != null
                    && hints.containsKey(JDBCPGrasterConfigurationBean.CONFIG_KEY)) {
                // Get the configuration from hints
                Object object = hints.get(JDBCPGrasterConfigurationBean.CONFIG_KEY);
                if (object instanceof JDBCPGrasterConfigurationBean) {
                    JDBCPGrasterConfigurationBean config = (JDBCPGrasterConfigurationBean) object;
                    configUrl =
                            new JDBCPGRasterConfigurationBuilder(config, new URL(url))
                                    .buildConfiguration();
                }
            }
            if (null == configUrl) configUrl = new URL(url);
        } catch (MalformedURLException mfe) {
            throw new IllegalArgumentException(mfe);
        }

        return configUrl;
    }

    /**
     * Extract configuration parameters from the provided string. See {@link
     * JDBCPGRasterConfigurationBuilder#createConfiguration(String, Hints)} for the syntax of this
     * string
     */
    private static JDBCPGrasterConfigurationBean parseConfig(final String pgrasterUrl) {
        if (pgrasterUrl != null && pgrasterUrl.startsWith("pgraster:/")) {
            final int fileURLIndex = pgrasterUrl.indexOf("#");
            if (fileURLIndex < 0) {
                throw new IllegalArgumentException(
                        "The specified URL doesn't contain the data folder");
            }

            // Not sure why the GeoserverDataDirectory.findDataFile eats a "/" char. 10 should be 11
            final int prefix = pgrasterUrl.startsWith("pgraster://") ? 11 : 10;

            // Parsing pgUser
            final int pguserEndIndex = pgrasterUrl.indexOf(":", prefix);
            final String pguser = pgrasterUrl.substring(prefix, pguserEndIndex);

            // Parsing password
            final int passwordEndIndex = pgrasterUrl.indexOf("@");
            final String password = pgrasterUrl.substring(pguserEndIndex + 1, passwordEndIndex);

            // Parsing host
            final int hostEndIndex = pgrasterUrl.indexOf(":", passwordEndIndex + 1);
            final String host = pgrasterUrl.substring(passwordEndIndex + 1, hostEndIndex);

            // Parsing port
            final int portEndIndex = pgrasterUrl.indexOf(":", hostEndIndex + 1);
            final String port = pgrasterUrl.substring(hostEndIndex + 1, portEndIndex);

            // Parsing Database
            final int dbEndIndex = pgrasterUrl.indexOf(".", portEndIndex + 1);
            final String db = pgrasterUrl.substring(portEndIndex + 1, dbEndIndex);

            // Parsing schema
            final int schemaEndIndex = pgrasterUrl.indexOf(".", dbEndIndex + 1);
            final String schema = pgrasterUrl.substring(dbEndIndex + 1, schemaEndIndex);

            // Parsing table
            final int tableEndIndex = pgrasterUrl.indexOf(":", schemaEndIndex + 1);

            // Parsing EPSGCode
            int epsgCode = DEFAULT_EPSG_CODE;
            final int epsgStartIndex = pgrasterUrl.indexOf("@", schemaEndIndex + 1);
            if (epsgStartIndex != -1) {
                try {
                    epsgCode =
                            Integer.parseInt(
                                    pgrasterUrl.substring(epsgStartIndex + 1, tableEndIndex));
                } catch (NumberFormatException nfe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe(
                                "Unable to parse the specified EPSGCode. Proceeding with DEFAULT:"
                                        + DEFAULT_EPSG_CODE
                                        + " due to : "
                                        + nfe.getLocalizedMessage());
                    }
                }
            }

            final String table =
                    pgrasterUrl.substring(
                            schemaEndIndex + 1,
                            epsgStartIndex != -1 ? epsgStartIndex : tableEndIndex);

            Properties datastoreProperties = new Properties();
            datastoreProperties.put(HOST_KEY.substring(1), host);
            datastoreProperties.put(PGUSER_KEY.substring(1), pguser);
            datastoreProperties.put(PORT_KEY.substring(1), port);
            datastoreProperties.put(PASSWORD_KEY.substring(1), password);
            datastoreProperties.put(DATABASE_KEY.substring(1), db);

            JDBCPGrasterConfigurationBean bean =
                    new JDBCPGrasterConfigurationBean(
                            datastoreProperties, table, "rt" + table, table, schema, epsgCode);
            return bean;
        }
        return null;
    }

    /**
     * Main mosaic configuration method: given a folder containing several tiles, this static helper
     * method does this: i - import raster tiles into the database ii - create metadata table and
     * put new tables on it iii - prepare configuration file from template
     */
    private URL buildConfiguration() {
        // Step 1: Validate configuration
        validateConfiguration();

        // Step 2: Check for/Create Config file
        final File configFile =
                new File(
                        URLs.urlToFile(configDir).getAbsolutePath()
                                + File.separatorChar
                                + configBean.getCoverageName()
                                + ".pgraster.xml");

        URL url = URLs.fileToUrl(configFile);
        Config config = null;

        if (!configFile.exists()) {
            // Config file doesn't exist. Need to create it
            try {
                createConfigFile(configFile);
                config = Config.readFrom(url);
                DBDialect dialect = DBDialect.getDBDialect(config);
                try (Connection connection = dialect.getConnection()) {

                    // Step 3: configure raster into DB in case they haven't been imported by hand
                    // Manual import may be preferred by the user when dealing with huge datasets
                    if (!isMosaicAlreadyInDB(connection)) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(
                                    "Proceeding with raster tiles automatic import using raster2pgsql");
                        }
                        initDb(connection);
                    } else {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info(
                                    "Skipping raster tiles import "
                                            + "since metadata tables and tile tables already exists into the DB");
                        }
                    }
                }
            } catch (Exception e) {
                // Rollback on configFile (In case something went wrong, delete the configFile)
                if (configFile.exists()) {
                    configFile.delete();
                }
                throw new RuntimeException(e);
            }
        }
        // Return the URL of the configuration xml
        return url;
    }

    /**
     * Check whether the specified mosaic is already available in the Database. In that case I don't
     * need to do the raster tiles import.
     *
     * @param connection the connection to be used to check for the database presence
     */
    private boolean isMosaicAlreadyInDB(final Connection connection) {

        final String selectMetadataTableQuery =
                TABLE_CHECK_SQL.replace((MOSAIC_KEY), configBean.getTableName());
        try {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Looking for mosaic table already created");
            }
            try (PreparedStatement ps = connection.prepareStatement(selectMetadataTableQuery);
                    ResultSet set = ps.executeQuery()) {
                final boolean allTablesArePresent = checkTileTables(set, connection);
                connection.commit();
                return allTablesArePresent;
            }
        } catch (SQLException e) {
            // The required table may not exists... We need to create it
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(e.getLocalizedMessage());
            }
            try {
                connection.rollback();
            } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Exception occurred while doing rollback:\n" + e.getLocalizedMessage());
                }
            }
        }
        return false;
    }

    /**
     * Check whether the main metadata table is present and all the tileTables specified inside it
     * (if any) exists too. In that case, no need to do the raster import step.
     *
     * @param set the {@link ResultSet} coming from the previous selection made on tileTable column.
     */
    private boolean checkTileTables(ResultSet set, Connection connection) throws SQLException {
        boolean proceed = true;
        boolean found = false;

        // Check that metadata table exists and all referred tile tables exixst too
        while (set.next() && proceed) {
            // A table with the specified name has been found.
            // Checking whether the raster tile table exist too
            final String tileTableName = set.getString("tiletable");
            final String selectMetadataTableQuery =
                    TILE_TABLE_CHECK_SQL.replace((MOSAIC_KEY), tileTableName);
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Looking for mosaic table already created");
                }
                try (PreparedStatement ps = connection.prepareStatement(selectMetadataTableQuery);
                        ResultSet tileSet = ps.executeQuery()) {
                    if (tileSet.next()) {
                        // We have found the tileTable referred inside metadata table
                        // Therefore, proceeding with the next one
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("tile table " + tileTableName + " has been found");
                        }
                        if (set.isLast()) {
                            // I'm checking the latest entry. All tables have been found.
                            // No more checks are needed
                            proceed = false;
                            found = true;
                        }
                        continue;
                    }
                }
            } catch (SQLException sqle) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(sqle.getLocalizedMessage());
                }
                final String message =
                        "Database contains the metadata table but some referred tile tables are missing. \n"
                                + "Please, cleanup your database and retry the configuration";
                // Logging a message reporting that the database is inconsistent (metadata table
                // exists, but only a few
                // referred raster tile table actually exist). We need some user intervention
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe(message);
                }
                throw new IllegalArgumentException(message);
            }
        }

        return found;
    }

    /** Check that all the configuration parameters are available */
    private void validateConfiguration() {
        Utilities.ensureNonNull("configBean", configBean);

        final Properties properties = configBean.getDatastoreProperties();
        Utilities.ensureNonNull("datastoreProperties", properties);

        final String schema = configBean.getSchema();
        Utilities.ensureNonNull("schema", schema);

        final String tileTablePrefix = configBean.getTileTablePrefix();
        Utilities.ensureNonNull("tileTablePrefix", tileTablePrefix);

        final String coverageName = configBean.getCoverageName();
        Utilities.ensureNonNull("coverageName", coverageName);

        final String tableName = configBean.getTableName();
        Utilities.ensureNonNull("tableName", tableName);
    }

    /**
     * Create the metadata table which will contain the tile tables references.
     *
     * @param tileTables the tile Table names
     * @param tableName the name of the master table where tile tables will be added
     * @param coverageName the name of the coverage to which tile tables are related
     */
    private void createMetadataTable(
            final Connection connection, final String tableName, final String coverageName) {

        // Prepare main insertion/update queries
        final String createMetadataTableQuery = TABLE_CREATION_SQL.replace((MOSAIC_KEY), tableName);

        boolean created = false;
        try {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Creating mosaic table");
            }
            // Create the metadata table
            connection.prepareStatement(createMetadataTableQuery).execute();
            created = true;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("updating mosaic table");
            }
            // commit
            connection.commit();
        } catch (SQLException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe(
                        "Exception occurred while "
                                + (created ? "updating" : "creating")
                                + " metadata tables. Proceeding with rollback\n"
                                + e.getLocalizedMessage());
            }
            try {
                connection.rollback();
            } catch (SQLException ex) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Exception occurred while doing rollback:\n" + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Initialize the master table
     *
     * @param filesToBeDeleted a List which will contains files to be deleted
     */
    private void initDb(final Connection connection) throws SQLException, IOException {
        final File configDirectory = URLs.urlToFile(configDir);

        // Preliminary check on configuration directory validity
        if (!configDirectory.exists()) {
            throw new IllegalArgumentException("Specified URL doesn't exist: " + configDir);
        }

        if (!configDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Specified URL doesn't refer to a directory: " + configDir);
        }

        // Step 2: create mosaic table for metadata and insert tile tables into metadata table
        final String tableName = configBean.getTableName();
        final String coverageName = configBean.getCoverageName();

        // TODO: We should check previous table exists before creating metadata table
        createMetadataTable(connection, tableName, coverageName);
    }

    /**
     * Create the configuration file containing the information to configure the ImageMosaic
     *
     * @param configFile the file where to store the configuration
     */
    private void createConfigFile(final File configFile) throws IOException {
        final String config = updateValues(configBean);
        storeConfig(configFile, config);
    }

    /**
     * Replace all the Jolly Strings from the Template with actual values.
     *
     * @param configBean the Configuration bean containing custom values to replace the jolly.
     */
    private String updateValues(JDBCPGrasterConfigurationBean configBean) {
        Properties datastoreProperties = configBean.getDatastoreProperties();
        String config =
                TEMPLATE.replace(MOSAIC_KEY, configBean.getTableName())
                        .replace(COVERAGE_KEY, configBean.getCoverageName())
                        .replace(
                                PGUSER_KEY,
                                datastoreProperties.getProperty(PGUSER_KEY.substring(1)))
                        .replace(
                                DATABASE_KEY,
                                datastoreProperties.getProperty(DATABASE_KEY.substring(1)))
                        .replace(
                                PASSWORD_KEY,
                                datastoreProperties.getProperty(PASSWORD_KEY.substring(1)))
                        .replace(PORT_KEY, datastoreProperties.getProperty(PORT_KEY.substring(1)))
                        .replace(HOST_KEY, datastoreProperties.getProperty(HOST_KEY.substring(1)))
                        .replace(EPSG_CODE_KEY, Integer.toString(configBean.getEpsgCode()));
        return config;
    }

    /**
     * Store the configuration as an XML files containing all the config params.
     *
     * @param configFile the output file where to store the config
     * @param config the configuration XML String to be stored
     */
    private void storeConfig(final File configFile, final String config) throws IOException {
        writeToFile(configFile, config);
    }

    /**
     * Write a String content to the specified file.
     *
     * @param file the file where to store the content
     * @param content the string to be written
     */
    private static void writeToFile(final File file, final String content) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes());
        }
    }
}
