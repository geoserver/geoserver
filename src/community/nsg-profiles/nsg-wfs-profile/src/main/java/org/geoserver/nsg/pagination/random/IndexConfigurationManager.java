/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Class used to parse the configuration properties stored in <b>nsg-profile</b> module folder:
 *
 * <ul>
 *   <li><b>resultSets.storage.path</b> path where to store the serialized GetFeatureRequest with
 *       name of random UUID.
 *   <li><b>resultSets.timeToLive</b> time to live value, all the stored requests that have not been
 *       used for a period of time bigger than this will be deleted.
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#DBTYPE}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#DATABASE}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#HOST}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#PORT}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#SCHEMA}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#USER}</b>
 *   <li><b>resultSets.db.{@link JDBCDataStoreFactory#PASSWD}</b>
 * </ul>
 *
 * All configuration properties is changeable at runtime so when this properties is updated the
 * module take the appropriate action:
 *
 * <ul>
 *   <li>When the index DB is changed the new DB should be used and the content of the old table
 *       moved to the new table. If the new DB already has the index table it should be emptied,
 *   <li>When the storage path is changed, the new storage path should be used and the old storage
 *       path content should be moved to the new one,
 *   <li>When the the time to live is changed the {@link #clean()} procedure will update.
 * </ul>
 *
 * <p>The class is also responsible to {@link #clean()} the stored requests (result sets) that have
 * not been used for a period of time bigger than the configured time to live value
 *
 * <p>
 *
 * @author sandr
 */
public final class IndexConfigurationManager implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(IndexConfigurationManager.class);

    static final String PROPERTY_DB_PREFIX = "resultSets.db.";

    static final String PROPERTY_FILENAME = "configuration.properties";

    static final String MODULE_DIR = "nsg-profile";

    static final String STORE_SCHEMA_NAME = "RESULT_SET";

    static final String STORE_SCHEMA =
            "ID:java.lang.String,created:java.lang.Long,updated:java.lang.Long";
    private final GeoServerDataDirectory dd;

    private final IndexConfiguration indexConfiguration = new IndexConfiguration();

    public IndexConfigurationManager(GeoServerDataDirectory dd) {
        this.dd = dd;
    }

    /*
     * Lock to synchronize activity of clean task with listener that changes the DB and file
     * resources
     */
    static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        Resource resource = dd.get(MODULE_DIR + "/" + PROPERTY_FILENAME);
        if (resource.getType() == Resource.Type.UNDEFINED) {
            Properties properties = new Properties();
            try (InputStream stream =
                    IndexConfigurationManager.class.getResourceAsStream("/" + PROPERTY_FILENAME)) {
                properties.load(stream);
                // Replace GEOSERVER_DATA_DIR placeholder
                properties.replaceAll(
                        (k, v) ->
                                ((String) v).replace("${GEOSERVER_DATA_DIR}", dd.root().getPath()));
            }
            // Create resource and save properties
            try (OutputStream out = resource.out()) {
                properties.store(out, null);
                out.close();
            } catch (Exception exception) {
                throw new RuntimeException(
                        "Error initializing paged results configurations.", exception);
            }
        }
        // make sure the default locations are created too
        dd.get(MODULE_DIR).get("resultSets").dir();
        dd.get(MODULE_DIR).get("db").get("resultSets").dir();
        loadConfigurations(resource);
        // Listen for changes in configuration file and reload properties
        resource.addListener(
                notify -> {
                    if (notify.getKind() == Kind.ENTRY_MODIFY) {
                        try {
                            loadConfigurations(resource);
                        } catch (Exception exception) {
                            throw new RuntimeException("Error reload configurations.", exception);
                        }
                    }
                });
    }

    /** Helper method that loads configuration file and changes environment setup */
    private void loadConfigurations(Resource resource) throws Exception {
        try {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().lock();
            Properties properties = new Properties();
            InputStream is = resource.in();
            properties.load(is);
            // Replace GEOSERVER_DATA_DIR placeholder
            properties.replaceAll(
                    (k, v) -> ((String) v).replace("${GEOSERVER_DATA_DIR}", dd.root().getPath()));
            is.close();
            // Reload database
            Map<String, Object> params = new HashMap<>();
            params.put(
                    JDBCDataStoreFactory.DBTYPE.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.DBTYPE.key));
            params.put(
                    JDBCDataStoreFactory.DATABASE.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.DATABASE.key));
            params.put(
                    JDBCDataStoreFactory.HOST.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.HOST.key));
            params.put(
                    JDBCDataStoreFactory.PORT.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.PORT.key));
            params.put(
                    JDBCDataStoreFactory.SCHEMA.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.SCHEMA.key));
            params.put(
                    JDBCDataStoreFactory.USER.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.USER.key));
            params.put(
                    JDBCDataStoreFactory.PASSWD.key,
                    properties.get(PROPERTY_DB_PREFIX + JDBCDataStoreFactory.PASSWD.key));
            /*
             * When the index DB is changed the new DB should be used and the content of the old
             * table moved to the new table. If the new DB already has the index table it should be
             * emptied
             */
            manageDBChange(params);
            /*
             * If the storage path is changed, the new storage path should be used and the old
             * storage path content should be moved to the new one
             */
            manageStorageChange(properties.get("resultSets.storage.path"));
            /*
             * Change time to live
             */
            manageTimeToLiveChange(properties.get("resultSets.timeToLive"));
        } catch (Exception exception) {
            throw new RuntimeException("Error reload configurations.", exception);
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    /** Helper method that store the time to live value */
    private void manageTimeToLiveChange(Object timneToLive) {
        try {
            if (timneToLive != null) {
                String timneToLiveStr = (String) timneToLive;
                indexConfiguration.setTimeToLive(Long.parseLong(timneToLiveStr), TimeUnit.SECONDS);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Error on change time to live", exception);
        }
    }

    /**
     * Helper method that move resources files form current folder to the new one, current storage
     * is deleted
     */
    private void manageStorageChange(Object newStorage) {
        try {
            if (newStorage != null) {
                String newStorageStr = (String) newStorage;
                Resource newResource = new FileSystemResourceStore(new File(newStorageStr)).get("");
                Resource exResource = indexConfiguration.getStorageResource();
                if (exResource != null
                        && !newResource
                                .dir()
                                .getAbsolutePath()
                                .equals(exResource.dir().getAbsolutePath())) {
                    exResource.delete();
                }
                indexConfiguration.setStorageResource(newResource);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Error on change store", exception);
        }
    }

    /** Helper method that move DB data from old store to new one */
    private void manageDBChange(Map<String, Object> params) {
        try {
            DataStore exDataStore = indexConfiguration.getCurrentDataStore();
            DataStore newDataStore = DataStoreFinder.getDataStore(params);
            if (exDataStore != null) {
                // New database is valid and is different from current one
                if (newDataStore != null && !isDBTheSame(params)) {
                    // Create table in new database
                    createFeatureType(newDataStore, true);
                    // Move data to new database
                    moveData(exDataStore, newDataStore);
                    // Dispose old database
                    exDataStore.dispose();
                }
            } else {
                // Create schema
                createFeatureType(newDataStore, false);
            }
            indexConfiguration.setCurrentDataStore(params, newDataStore);
        } catch (Exception exception) {
            throw new RuntimeException("Error reload DB configurations.", exception);
        }
    }

    /**
     * Helper method that check id the DB is the same, matching the JDBC configurations parameters.
     */
    private Boolean isDBTheSame(Map<String, Object> newParams) {
        Map<String, Object> currentParams = indexConfiguration.getCurrentDataStoreParams();
        boolean isTheSame =
                (currentParams.get(JDBCDataStoreFactory.DBTYPE.key) == null
                                && newParams.get(JDBCDataStoreFactory.DBTYPE.key) == null)
                        || (currentParams.get(JDBCDataStoreFactory.DBTYPE.key) != null
                                && newParams.get(JDBCDataStoreFactory.DBTYPE.key) != null
                                && currentParams
                                        .get(JDBCDataStoreFactory.DBTYPE.key)
                                        .equals(newParams.get(JDBCDataStoreFactory.DBTYPE.key)));
        isTheSame =
                isTheSame
                                && (currentParams.get(JDBCDataStoreFactory.DATABASE.key) == null
                                        && newParams.get(JDBCDataStoreFactory.DATABASE.key) == null)
                        || (currentParams.get(JDBCDataStoreFactory.DATABASE.key) != null
                                && newParams.get(JDBCDataStoreFactory.DATABASE.key) != null
                                && currentParams
                                        .get(JDBCDataStoreFactory.DATABASE.key)
                                        .equals(newParams.get(JDBCDataStoreFactory.DATABASE.key)));
        isTheSame =
                isTheSame
                                && (currentParams.get(JDBCDataStoreFactory.HOST.key) == null
                                        && newParams.get(JDBCDataStoreFactory.HOST.key) == null)
                        || (currentParams.get(JDBCDataStoreFactory.HOST.key) != null
                                && newParams.get(JDBCDataStoreFactory.HOST.key) != null
                                && currentParams
                                        .get(JDBCDataStoreFactory.HOST.key)
                                        .equals(newParams.get(JDBCDataStoreFactory.HOST.key)));
        isTheSame =
                isTheSame
                                && (currentParams.get(JDBCDataStoreFactory.PORT.key) == null
                                        && newParams.get(JDBCDataStoreFactory.PORT.key) == null)
                        || (currentParams.get(JDBCDataStoreFactory.PORT.key) != null
                                && newParams.get(JDBCDataStoreFactory.PORT.key) != null
                                && currentParams
                                        .get(JDBCDataStoreFactory.PORT.key)
                                        .equals(newParams.get(JDBCDataStoreFactory.PORT.key)));
        isTheSame =
                isTheSame
                                && (currentParams.get(JDBCDataStoreFactory.SCHEMA.key) == null
                                        && newParams.get(JDBCDataStoreFactory.SCHEMA.key) == null)
                        || (currentParams.get(JDBCDataStoreFactory.SCHEMA.key) != null
                                && newParams.get(JDBCDataStoreFactory.SCHEMA.key) != null
                                && currentParams
                                        .get(JDBCDataStoreFactory.SCHEMA.key)
                                        .equals(newParams.get(JDBCDataStoreFactory.SCHEMA.key)));
        return isTheSame;
    }

    /** Helper method that create a new table on DB to store resource informations */
    private void createFeatureType(DataStore dataStore, boolean forceDelete) throws Exception {
        boolean exists = dataStore.getNames().contains(new NameImpl(STORE_SCHEMA_NAME));
        // Schema exists
        if (exists) {
            // Delete of exist is required, and then create a new one
            if (forceDelete) {
                dataStore.removeSchema(STORE_SCHEMA_NAME);
                SimpleFeatureType schema =
                        DataUtilities.createType(STORE_SCHEMA_NAME, STORE_SCHEMA);
                dataStore.createSchema(schema);
            }
            // Schema not exists, create a new one
        } else {
            SimpleFeatureType schema = DataUtilities.createType(STORE_SCHEMA_NAME, STORE_SCHEMA);
            dataStore.createSchema(schema);
        }
    }

    /** Helper method that move resource informations from current DB to the new one */
    private void moveData(DataStore exDataStore, DataStore newDataStore) throws Exception {
        Transaction session = new DefaultTransaction("Moving");
        try {
            SimpleFeatureSource exFs = exDataStore.getFeatureSource(STORE_SCHEMA_NAME);
            SimpleFeatureStore newFs =
                    (SimpleFeatureStore) newDataStore.getFeatureSource(STORE_SCHEMA_NAME);
            newFs.setTransaction(session);
            newFs.addFeatures(exFs.getFeatures());
            session.commit();
        } catch (Throwable t) {
            session.rollback();
            throw new RuntimeException("Error on move data", t);
        } finally {
            session.close();
        }
    }

    /**
     * Delete all the stored requests (result sets) that have not been used for a period of time
     * bigger than the configured time to live value. Clean also related resource files.
     *
     * <p>Executed by scheduler, for details see Spring XML configuration
     */
    public void clean() throws Exception {
        Transaction session = new DefaultTransaction("RemoveOld");
        try {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().lock();
            // Remove record
            Long timeToLive = indexConfiguration.getTimeToLiveInSec();
            DataStore currentDataStore = indexConfiguration.getCurrentDataStore();
            Long liveTreshold = System.currentTimeMillis() - timeToLive * 1000;
            long featureRemoved = 0;
            if (currentDataStore != null) {
                SimpleFeatureStore store =
                        (SimpleFeatureStore) currentDataStore.getFeatureSource(STORE_SCHEMA_NAME);
                Filter filter = CQL.toFilter("updated < " + liveTreshold);

                SimpleFeatureCollection toRemoved = store.getFeatures(filter);
                // Remove file
                Resource currentResource = indexConfiguration.getStorageResource();
                try (SimpleFeatureIterator iterator = toRemoved.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        currentResource.get(feature.getID()).delete();
                        featureRemoved++;
                    }
                }
                store.removeFeatures(filter);
            }
            if (LOGGER.isLoggable(Level.FINEST)) {
                if (featureRemoved > 0) {
                    LOGGER.finest(
                            "CLEAN executed, removed "
                                    + featureRemoved
                                    + " stored requests older than "
                                    + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                            .format(new Date(liveTreshold)));
                }
            }
        } catch (Throwable t) {
            session.rollback();
            LOGGER.log(Level.WARNING, "Error on clean data", t);
        } finally {
            session.close();
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    public DataStore getCurrentDataStore() {
        IndexConfigurationManager.READ_WRITE_LOCK.readLock().lock();
        try {
            return indexConfiguration.getCurrentDataStore();
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public Map<String, Object> getCurrentDataStoreParams() {
        IndexConfigurationManager.READ_WRITE_LOCK.readLock().lock();
        try {
            return indexConfiguration.getCurrentDataStoreParams();
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public Resource getStorageResource() {
        IndexConfigurationManager.READ_WRITE_LOCK.readLock().lock();
        try {
            return indexConfiguration.getStorageResource();
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public Long getTimeToLiveInSec() {
        IndexConfigurationManager.READ_WRITE_LOCK.readLock().lock();
        try {
            return indexConfiguration.getTimeToLiveInSec();
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.readLock().unlock();
        }
    }

    /**
     * Class used to store the index result type configuration managed by {@link
     * IndexConfigurationManager}
     *
     * @author sandr
     */
    public class IndexConfiguration {

        private DataStore currentDataStore;

        private Resource storageResource;

        private Long timeToLiveInSec = 600L;

        private Map<String, Object> currentDataStoreParams;

        /** Store the DB parameters and the relative {@link DataStore} */
        public void setCurrentDataStore(
                Map<String, Object> currentDataStoreParams, DataStore currentDataStore) {
            this.currentDataStoreParams = currentDataStoreParams;
            this.currentDataStore = currentDataStore;
        }

        /** Store the reference to resource used to archive the serialized GetFeatureRequest */
        public void setStorageResource(Resource storageResource) {
            this.storageResource = storageResource;
        }

        /** Store the value of time to live of stored GetFeatureRequest */
        public void setTimeToLive(Long timeToLive, TimeUnit timeUnit) {
            this.timeToLiveInSec = timeUnit.toSeconds(timeToLive);
        }

        public DataStore getCurrentDataStore() {
            return currentDataStore;
        }

        public Map<String, Object> getCurrentDataStoreParams() {
            return currentDataStoreParams;
        }

        public Resource getStorageResource() {
            return storageResource;
        }

        public Long getTimeToLiveInSec() {
            return timeToLiveInSec;
        }
    }
}
