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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment.Variable;
import org.geotools.gce.imagemosaic.jdbc.Config;
import org.geotools.gce.imagemosaic.jdbc.DBDialect;
import org.geotools.gce.imagemosaic.jdbc.ImageMosaicJDBCReader;
import org.geotools.util.URLs;
import org.geotools.util.Utilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * A JDBC PGRaster configuration builder class. It allows to configure a PGRaster based
 * ImageMosaicJDBC starting from: - a folder containing several tiles previously created through
 * gdal_retile - a configuration bean specifying PG credentials, database, table, coverage name and
 * files extensions
 *
 * <p>The configuration may be specified in 2 different ways: 1) through an input string with the
 * following format:
 * pgraster://USER:PASS@HOST:PORT:DATABASE.SCHEMA.TABLE@EPSGCODE:*.FILE_EXTENSION?OPTIONS#/PATH/TO/RASTER_TILES/"
 *
 * <p>In this case, a JDBCPGRasterConfigurationBean instance will be created on top of this String.
 * That String also contains the RASTER_TILES PATH representing the folder containing tiles
 * previously created with gdal_retile. Note that gdal_retile will create tiles using that
 * structure: - level 0 is put into the target dir - level i is put into subfolder i inside target
 * dir
 *
 * <p>NOTE that -useDirForEachRow gdal option isn't currently supported by the importer.
 *
 * <p>FILE_EXTENSION and OPTIONS configuration properties are optional (in case the user did the
 * tiles import on his own)
 *
 * <p>2) a JDBCPGRasterConfigurationBean instance. The input string simply contains the RASTER_TILES
 * PATH on this case.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class JDBCPGRasterConfigurationBuilder {

    private static final String RASTER2PGSQL_PATH_KEY = "RASTER2PGSQL_PATH";

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

    private static final String OPTIONS_KEY = "$OPTIONS";

    private static final String SCHEMA_KEY = "$SCHEMA";

    private static final String EPSG_CODE_KEY = "$EPSG_CODE";

    private static final String TABLE_PREFIX_KEY = "$TABLE_PREFIX";

    private static final String SQL_FILE_KEY = "$SQL_FILE";

    private static final String DATABASE_KEY = "$DATABASE";

    private static final String PGUSER_KEY = "$USER";

    private static final String PASSWORD_KEY = "$PASSWORD";

    private static final String PORT_KEY = "$PORT";

    private static final String HOST_KEY = "$HOST";

    private static final String FILES_KEY = "$FILES";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String DEFAULT_OPTIONS = "-t 128x128";

    private static final String TABLE_CREATION_SQL =
            "create table "
                    + MOSAIC_KEY
                    + " (NAME varchar(254) not null,"
                    + "TileTable varchar(254)not null, minX FLOAT8, minY FLOAT8, maxX FLOAT8, maxY FLOAT8, resX FLOAT8, resY FLOAT8,"
                    + "primary key (NAME,TileTable))";

    private static final String TABLE_CHECK_SQL = "SELECT tiletable FROM " + MOSAIC_KEY;

    private static final String TILE_TABLE_CHECK_SQL = "SELECT COUNT(rid) FROM " + MOSAIC_KEY;

    private static final String TILETABLE_INSERTION_SQL =
            "insert into " + MOSAIC_KEY + " (NAME,TileTable) values (?,?)";

    private static final Logger LOGGER = Logging.getLogger(ImageMosaicJDBCReader.class);

    private static final String TEMPLATE_FILE_NAME = "coverage.pgraster.template.xml";

    private static String RASTER2PGSQL_COMMAND = "raster2pgsql";

    private static String EXECUTE = "execute";

    private static final int DEFAULT_EPSG_CODE = 4326;

    private static String IMPORT_COMMAND;

    private static String TEMPLATE;

    private static String PATH;

    private static String RASTER2PGSQL_PATH;

    private static boolean available;

    private static volatile boolean init = false;

    private JDBCPGrasterConfigurationBean configBean;

    private URL configDir;

    static {
        initTemplate();
        checkRaster2Pgsql();
    }

    /** Check whether the raster2pgsql script is available */
    public static boolean isRaster2PgsqlAvailable() {
        checkRaster2Pgsql();
        return available;
    }

    /** Check the availability of raster2pgsql command */
    public static void checkRaster2Pgsql() {
        if (init == false) {
            synchronized (LOGGER) {
                if (init) {
                    return;
                }
                PATH = System.getenv("PATH");
                RASTER2PGSQL_PATH = System.getProperty(RASTER2PGSQL_PATH_KEY);
                File file = null;
                try {
                    // Executing a raster2pgsql invokation to check for its availability
                    String OS = System.getProperty("os.name").toLowerCase();
                    final File dir;
                    if (OS.indexOf("win") >= 0) {
                        EXECUTE = "execute.bat";
                        String executable = RASTER2PGSQL_COMMAND;
                        final String executablePath = RASTER2PGSQL_PATH + File.separatorChar;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("RASTER2PGSQL_PATH = " + RASTER2PGSQL_PATH);
                        }
                        dir = new File(executablePath);
                        executable = executablePath + executable + ".exe";
                        RASTER2PGSQL_COMMAND = executable;
                    } else {
                        dir = new File(".");
                    }
                    IMPORT_COMMAND =
                            RASTER2PGSQL_COMMAND
                                    + " "
                                    + OPTIONS_KEY
                                    + " -F "
                                    + FILES_KEY
                                    + " "
                                    + SCHEMA_KEY
                                    + "."
                                    + TABLE_PREFIX_KEY
                                    + " > "
                                    + SQL_FILE_KEY
                                    + LINE_SEPARATOR
                                    + "psql -d "
                                    + DATABASE_KEY
                                    + " -U "
                                    + PGUSER_KEY
                                    + " -h "
                                    + HOST_KEY
                                    + " -p "
                                    + PORT_KEY
                                    + " -f "
                                    + SQL_FILE_KEY;
                    final ExecTask task = new ExecTask();
                    task.setExecutable(RASTER2PGSQL_COMMAND);
                    task.setDir(dir);
                    task.createArg().setValue("-G");
                    Variable variable = new Variable();
                    variable.setKey("PATH");
                    variable.setValue(PATH);
                    task.addEnv(variable);

                    // Logging to a temporary file to avoid logging on system out
                    file = File.createTempFile("r2pg", ".tmp");
                    task.setOutput(file);
                    task.execute();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("raster2pgsql script is available");
                    }
                    available = true;
                } catch (BuildException | IOException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(
                                "Failed to invoke the raster2pgsql script. This is not a problem "
                                        + "unless you need to use the raster2pgsql script to automatically configure pgrasters.\n"
                                        + e.toString());
                    }
                    available = false;
                } finally {
                    init = true;
                    if (file != null) {
                        file.delete();
                    }
                }
            }
        }
    }

    /** Initialize the string template parsing the available resource */
    private static void initTemplate() {
        try (InputStream stream =
                        JDBCPGRasterConfigurationBuilder.class.getResourceAsStream(
                                TEMPLATE_FILE_NAME);
                InputStreamReader streamReader = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(streamReader)) {

            // Replace with some dedicate method which setup string from resources.
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            TEMPLATE = stringBuilder.toString();
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(e.getLocalizedMessage());
            }
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
        try {
            JDBCPGRasterConfigurationBuilder builder = null;
            JDBCPGrasterConfigurationBean config = null;
            // Fail in case no raster2pgsql script is available
            if (url.startsWith("pgraster")) {
                // Parse the string containing configuration inline
                config = parseConfig(url);
                final int fileURLIndex = url.indexOf("#");
                final String dataUrl = url.substring(fileURLIndex + 1);
                final String fileUrl = dataUrl.startsWith("file:/") ? dataUrl : "file://" + dataUrl;
                builder = new JDBCPGRasterConfigurationBuilder(config, new URL(fileUrl));
            } else {
                if (hints != null && hints.containsKey(JDBCPGrasterConfigurationBean.CONFIG_KEY)) {
                    // Get the configuration from hints
                    Object object = hints.get(JDBCPGrasterConfigurationBean.CONFIG_KEY);
                    if (object != null && object instanceof JDBCPGrasterConfigurationBean) {
                        config = (JDBCPGrasterConfigurationBean) object;
                        builder = new JDBCPGRasterConfigurationBuilder(config, new URL(url));
                    }
                }
            }
            if (builder != null) {
                if (!isRaster2PgsqlAvailable()
                        && config != null
                        && (config.getFileExtension() != null
                                || config.getImportOptions() != null)) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(
                                "The specified URL refers to a pgraster but raster2pgsql script is unavailable.\n"
                                        + "Automatic configuration won't be performed. In case raster tiles have been manually imported, "
                                        + "make sure to leave fileExtensions and importOptions parameters empty and repeat the coverage configuration.");
                        return null;
                    }
                }

                return builder.buildConfiguration();
            }
            return new URL(url);
        } catch (MalformedURLException mfe) {
            throw new IllegalArgumentException(mfe);
        }
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

            // Parsing options
            final int optionsStartIndex = pgrasterUrl.indexOf("?", tableEndIndex + 1);
            final String options =
                    optionsStartIndex != -1
                            ? pgrasterUrl.substring(optionsStartIndex + 1, fileURLIndex)
                            : DEFAULT_OPTIONS;

            // Parsing file extensions of raster to be imported (if any)
            final int fileExtensionEndIndex =
                    optionsStartIndex != -1 ? optionsStartIndex : fileURLIndex;
            final String fileExtension =
                    pgrasterUrl.substring(tableEndIndex + 1, fileExtensionEndIndex);

            Properties datastoreProperties = new Properties();
            datastoreProperties.put(HOST_KEY.substring(1), host);
            datastoreProperties.put(PGUSER_KEY.substring(1), pguser);
            datastoreProperties.put(PORT_KEY.substring(1), port);
            datastoreProperties.put(PASSWORD_KEY.substring(1), password);
            datastoreProperties.put(DATABASE_KEY.substring(1), db);

            JDBCPGrasterConfigurationBean bean =
                    new JDBCPGrasterConfigurationBean(
                            datastoreProperties,
                            table,
                            "rt" + table,
                            fileExtension,
                            table,
                            options,
                            schema,
                            epsgCode);
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
        final List<File> filesToBeDeleted = new ArrayList<>();

        if (!configFile.exists()) {
            // Config file doesn't exist. Need to create it
            try {

                // TODO: we may consider adding support for external folder where to store both
                // config file
                // and script file
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
                        importTilesIntoDB(connection, filesToBeDeleted);
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
            } finally {
                for (File file : filesToBeDeleted) {
                    try {
                        file.delete();
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(
                                    "Exception occurred while deleting temp file: "
                                            + file.getAbsolutePath()
                                            + "\n"
                                            + e.getLocalizedMessage());
                        }
                    }
                }
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
            final Connection connection,
            final String tableName,
            final String coverageName,
            final List<String> tileTables) {

        // Prepare main insertion/update queries
        final String createMetadataTableQuery = TABLE_CREATION_SQL.replace((MOSAIC_KEY), tableName);
        final String insertTileQuery = TILETABLE_INSERTION_SQL.replace((MOSAIC_KEY), tableName);
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
            try (PreparedStatement ps = connection.prepareStatement(insertTileQuery)) {

                // Inserting tile tables
                for (String tileTable : tileTables) {
                    ps.setString(1, coverageName);
                    ps.setString(2, tileTable);
                    ps.execute();
                }
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
     * Populate the DB with rasters produced in advance by the user with gdal_retile
     *
     * @param filesToBeDeleted a List which will contains files to be deleted
     */
    private void importTilesIntoDB(final Connection connection, final List<File> filesToBeDeleted)
            throws SQLException, IOException {
        final File configDirectory = URLs.urlToFile(configDir);

        // Preliminary check on configuration directory validity
        if (!configDirectory.exists()) {
            throw new IllegalArgumentException("Specified URL doesn't exist: " + configDir);
        }

        if (!configDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Specified URL doesn't refer to a directory: " + configDir);
        }

        // Preparing main configuration parameters
        final String tablePrefix = configBean.getTileTablePrefix();
        final String importOptions = configBean.getImportOptions();
        final String fileExtension = configBean.getFileExtension();
        final String schema = configBean.getSchema();

        // Database properties
        final Properties datastoreProperties = configBean.getDatastoreProperties();
        final String database = (String) datastoreProperties.get(DATABASE_KEY.substring(1));
        final String pguser = (String) datastoreProperties.get(PGUSER_KEY.substring(1));
        final String password = (String) datastoreProperties.get(PASSWORD_KEY.substring(1));
        final String port = (String) datastoreProperties.get(PORT_KEY.substring(1));
        final String host = (String) datastoreProperties.get(HOST_KEY.substring(1));

        final List<String> tileTables = new ArrayList<>();

        // scan tiles folder created by gdal_retile
        // Note that GDAL put tiles for level 0 straight on the folder.
        // additional levels are stored on folder 1, 2, 3 and so on

        final File dataDir = URLs.urlToFile(configDir);

        // Preparing a single script containing all the raster2pgsql commands to be invoked
        String script =
                createScript(
                        dataDir,
                        database,
                        schema,
                        host,
                        port,
                        pguser,
                        tablePrefix,
                        fileExtension,
                        tileTables,
                        importOptions,
                        filesToBeDeleted);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Executing the script");
        }
        executeScript(dataDir, script, password, filesToBeDeleted);

        // Step 2: create mosaic table for metadata and insert tile tables into metadata table
        final String tableName = configBean.getTableName();
        final String coverageName = configBean.getCoverageName();

        // TODO: We should check previous table exists before creating metadata table
        createMetadataTable(connection, tableName, coverageName, tileTables);
    }

    /**
     * Create a shell script that does raster2pgsql invokations which do the raster tiles import
     * into the PGRaster database
     *
     * @param dataDir the main datadir (containing tiles)
     * @param database the database
     * @param schema the schema where tiles need to be added
     * @param host the postgres DB server
     * @param port the postgres DB server port
     * @param pguser the postgres user
     * @param tablePrefix the prefix to be put at the head of each table name containing tiles at
     *     different levels
     * @param fileExtension the file extension of the raster files to be imported (as an instance,
     *     *.png or *.tif)
     * @param tileTables a List where table names will be added.
     * @param filesToBeDeleted a List of files which need to be deleted at the end of the import
     * @return the script content as a String
     */
    private static String createScript(
            final File dataDir,
            final String database,
            final String schema,
            final String host,
            final String port,
            final String pguser,
            final String tablePrefix,
            final String fileExtension,
            final List<String> tileTables,
            final String importOptions,
            List<File> filesToBeDeleted) {
        // Prepare the raster2pgsql command by replacing values
        final String mainCommand =
                prepareMainCommand(
                        importOptions, fileExtension, database, pguser, schema, host, port);

        final StringBuilder commands = new StringBuilder();
        final File[] files = dataDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // scan folders for tiles to be imported
                    final String importCommand =
                            updateCommand(
                                    file,
                                    mainCommand,
                                    tablePrefix,
                                    fileExtension,
                                    tileTables,
                                    filesToBeDeleted);
                    commands.append(importCommand).append(LINE_SEPARATOR);
                }
            }
        }
        String importCommand =
                updateCommand(
                        dataDir,
                        mainCommand,
                        tablePrefix,
                        fileExtension,
                        tileTables,
                        filesToBeDeleted);
        commands.append(importCommand).append(LINE_SEPARATOR);
        return commands.toString();
    }

    /**
     * Execute all the raster2pgsql import commands in one step
     *
     * @param script the script content
     */
    private void executeScript(
            final File dataDir,
            final String script,
            final String password,
            final List<File> filesToBeDeleted)
            throws IOException {
        final File scriptFile = new File(dataDir, EXECUTE);
        filesToBeDeleted.add(scriptFile);
        writeToFile(scriptFile, script);

        ExecTask task = new ExecTask();
        task.setExecutable(scriptFile.getAbsolutePath());
        task.setDir(dataDir);
        // Setting Postgres password to authenticate DB Import
        Variable variable = new Variable();
        variable.setKey("PGPASSWORD");
        variable.setValue(password);
        task.addEnv(variable);

        // Setting PATH variable
        Variable varPath = new Variable();
        varPath.setKey("PATH");
        varPath.setValue(PATH);
        task.addEnv(variable);
        task.execute();
    }

    /**
     * Replace the main command with proper values for each argument.
     *
     * @param file the folder containing files to be imported
     * @param mainCommand the original command with $PARAM to be replaced
     * @param tablePrefix the prefix of the table
     * @param fileExtension the file type do be inserted
     * @param filesToBeDeleted a List of files to be deleted at the end of import step
     */
    private static String updateCommand(
            File file,
            String mainCommand,
            String tablePrefix,
            String fileExtension,
            List<String> tileTables,
            List<File> filesToBeDeleted) {
        final String folderName = file.getName();
        final String tileTable = tablePrefix + folderName;
        tileTables.add(tileTable);
        String command = mainCommand.replace(TABLE_PREFIX_KEY, tileTable);
        command =
                command.replace(
                        FILES_KEY, file.getAbsolutePath() + File.separatorChar + fileExtension);
        final String fileToBeDeleted =
                file.getAbsolutePath() + File.separatorChar + tileTable + ".sql";
        filesToBeDeleted.add(new File(fileToBeDeleted));
        command = command.replace(SQL_FILE_KEY, fileToBeDeleted);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Import Script to be executed: " + command);
        }
        return command;
    }

    /** Prepare the main raster2pgsql command by updating parameters */
    private static String prepareMainCommand(
            String importOptions,
            String fileExtension,
            String database,
            String pguser,
            String schema,
            String host,
            String port) {
        String mainCommand = IMPORT_COMMAND;

        if (importOptions == null) {
            importOptions = DEFAULT_OPTIONS;
        }
        mainCommand = mainCommand.replace(OPTIONS_KEY, importOptions);
        mainCommand = mainCommand.replace(SCHEMA_KEY, schema);
        mainCommand = mainCommand.replace(DATABASE_KEY, database);
        mainCommand = mainCommand.replace(PGUSER_KEY, pguser);
        mainCommand = mainCommand.replace(HOST_KEY, host);
        mainCommand = mainCommand.replace(PORT_KEY, port);

        return mainCommand;
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
