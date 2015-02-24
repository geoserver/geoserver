/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gss.GSSInfo.GSSMode;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.jdbc.JDBCUtils;
import org.geotools.data.postgis.VersionedPostgisDataStore;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Provides core services used by both Central and Unit behaviors
 * 
 * @author Andrea Aime - OpenGeo
 */
public class GSSCore {

    // metadata tables and sql to build them
    static final String SYNCH_TABLES = "synch_tables";

    static final String SYNC_TABLES_CREATION = "CREATE TABLE synch_tables(\n"
            + "table_id SERIAL PRIMARY KEY, \n" // 
            + "table_name VARCHAR(256) NOT NULL, \n" //
            + "type CHAR(1) NOT NULL CHECK (type in ('p', 'b', '2')))";

    static final String SYNCH_HISTORY = "synch_history";

    static final String SYNCH_HISTORY_CREATION = "CREATE TABLE synch_history(\n"
            + "id SERIAL PRIMARY KEY,\n" //
            + "table_name VARCHAR(256) NOT NULL,\n" //
            + "local_revision BIGINT NOT NULL,\n" //
            + "central_revision BIGINT,\n" //
            + "unique(table_name, local_revision, central_revision))";

    static final String SYNCH_CONFLICTS = "synch_conflicts";

    // conflict can be in 'c', conflict, 'r', resolved or 'm', clean merge state
    // clean merge is a marker stating that the same change occurred both locally
    // and in central, and as such it should not be reported in GetDiff
    static final String SYNCH_CONFLICTS_CREATION = "CREATE TABLE synch_conflicts(\n"
            + "id SERIAL PRIMARY KEY,\n" + "table_name VARCHAR(256) NOT NULL,\n" // 
            + "feature_id UUID NOT NULL,\n" //
            + "local_revision BIGINT NOT NULL,\n" //  
            + "date_created TIMESTAMP NOT NULL,\n" // 
            + "state CHAR(1) NOT NULL CHECK (state in ('c', 'r', 'm')),\n" //  
            + "date_resolved TIMESTAMP,\n" //
            + "local_feature TEXT,\n" // 
            + "unique(table_name, feature_id, local_revision))";

    static final String SYNCH_UNITS = "synch_units";

    static final String SYNCH_UNITS_CREATION = "CREATE TABLE synch_units (\n" //
            + "  unit_id SERIAL PRIMARY KEY,\n" //
            + "  unit_name VARCHAR(1024) NOT NULL,\n" //
            + "  unit_address VARCHAR(2048) NOT NULL,\n" // 
            + "  synch_user VARCHAR(256),\n" //
            + "  synch_password VARCHAR(256),\n" // 
            + "  time_start TIME,\n" // 
            + "  time_end TIME,\n" // 
            + "  synch_interval REAL,\n" // 
            + "  synch_retry REAL,\n" // 
            + "  errors BOOLEAN\n" //
            + ");\n" // 
            + "select AddGeometryColumn('synch_units','geom',4326,'GEOMETRY',2)";

    static final String SYNCH_UNIT_TABLES = "synch_unit_tables";

    static final String SYNCH_UNIT_TABLES_CREATION = // 
    "CREATE TABLE synch_unit_tables (\n" + //
            "   id SERIAL PRIMARY KEY," + //
            "   unit_id INTEGER NOT NULL REFERENCES synch_units(unit_id),\n" + //
            "   table_id INTEGER NOT NULL REFERENCES synch_tables(table_id),\n" + //
            "   last_synchronization TIMESTAMP,\n" + // 
            "   last_failure TIMESTAMP,\n" + //
            "   getdiff_central_revision BIGINT,\n" + // 
            "   last_unit_revision BIGINT,\n" + //
            "   unique (unit_id, table_id)\n" + // 
            ")";

    static final String SYNCH_OUTSTANDING = "synch_outstanding";

    static final String SYNCH_OUTSTANDING_CREATION = //
    "CREATE VIEW synch_outstanding \n"
            + "AS SELECT synch_tables.*, \n"
            + "          synch_units.*, \n"
            + "          synch_unit_tables.last_synchronization,\n"
            + "          synch_unit_tables.last_failure, \n"
            + "          synch_unit_tables.getdiff_central_revision, \n"
            + "          synch_unit_tables.last_unit_revision\n"
            + "FROM (synch_units inner join synch_unit_tables \n"
            + "                  on synch_units.unit_id = synch_unit_tables.unit_id)\n"
            + "     inner join synch_tables \n"
            + "     on synch_tables.table_id = synch_unit_tables.table_id\n"
            + "WHERE ((time_start < LOCALTIME AND LOCALTIME < time_end) \n"
            + "        OR (time_start IS NULL) OR (time_end IS NULL))\n"
            + "      AND ((now() - last_synchronization > synch_interval * interval '1 minute') \n"
            + "        OR last_synchronization IS NULL)\n"
            + "      AND (last_failure is null OR"
            + "           now() - last_failure > synch_retry * interval '1 minute');\n"
            + "INSERT INTO geometry_columns VALUES('', 'public', 'synch_outstanding', 'geom', 2, 4326, 'GEOMETRY')";

    GeoServer geoServer;

    public GSSCore(GeoServer geoServer) {
        this.geoServer = geoServer;

        // try to version enable the tables that need to
        versionEnableTables();

        // add a listener that will try to version enable tables on config changes
        geoServer.addListener(new ConfigurationListenerAdapter() {
            
            @Override
            public void handlePostServiceChange(ServiceInfo service) {
                if (service instanceof GSSInfo) {
                    versionEnableTables();
                }
            }
            
            @Override
            public void handlePostGlobalChange(org.geoserver.config.GeoServerInfo global) {
                versionEnableTables();
            }
        });
    }

    void versionEnableTables() {
        try {
            ensureEnabled();
        } catch (Exception e) {
            // nothing to do really, the service might not be configured enough
        }
    }

    GSSInfo getServiceInfo() {
        return geoServer.getService(GSSInfo.class);
    }

    /**
     * Checks the module is ready to be used. TODO: move this to a listener so that we don't do all
     * the ckecks for every request
     */
    void ensureEnabled() {
        GSSInfo info = getServiceInfo();

        // basic sanity checks on the config
        if (info == null) {
            throw new GSSException("The service is not properly configured, gssInfo not found");
        }

        if (info.getMode() == null) {
            throw new GSSException("The gss mode has not been configured");
        }

        if (info.getVersioningDataStore() == null || !info.getVersioningDataStore().isEnabled()) {
            throw new GSSException("The service is disabled as the "
                    + "versioning datastore is not available/disabled");
        }

        FeatureIterator<SimpleFeature> fi = null;
        try {
            // basic sanity checks on the datastore
            DataAccess ds = info.getVersioningDataStore().getDataStore(null);
            if (!(ds instanceof VersionedPostgisDataStore)) {
                throw new GSSException(
                        "The store attached to the gss module is not a PostGIS versioning one");
            }
            VersionedPostgisDataStore dataStore = (VersionedPostgisDataStore) ds;

            Set<String> typeNames = new HashSet<String>(Arrays.asList(dataStore.getTypeNames()));

            // the synchronized tables list
            if (!typeNames.contains(SYNCH_TABLES)) {
                runStatement(dataStore, SYNC_TABLES_CREATION);
            }
            dataStore.setVersioned(SYNCH_TABLES, false, null, null);

            // version enable all tables that are supposed to be shared
            fi = dataStore.getFeatureSource(SYNCH_TABLES).getFeatures().features();
            while (fi.hasNext()) {
                String tableName = (String) fi.next().getAttribute("table_name");
                dataStore.setVersioned(tableName, true, null, null);
            }
            fi.close();

            if (info.getMode() == GSSMode.Unit) {
                // hmm... we should really try to use createSchema() instead, but atm
                // we don't have the necessary control over it

                // the unit synchronisation history
                if (!typeNames.contains(SYNCH_HISTORY)) {
                    runStatement(dataStore, SYNCH_HISTORY_CREATION);
                }
                dataStore.setVersioned(SYNCH_HISTORY, false, null, null);

                // the conflict table
                if (!typeNames.contains(SYNCH_CONFLICTS)) {
                    runStatement(dataStore, SYNCH_CONFLICTS_CREATION);
                }
                dataStore.setVersioned(SYNCH_CONFLICTS, true, null, null);
            } else {
                if (!typeNames.contains(SYNCH_UNITS)) {
                    runStatement(dataStore, SYNCH_UNITS_CREATION);
                }
                dataStore.setVersioned(SYNCH_UNITS, false, null, null);

                if (!typeNames.contains(SYNCH_UNIT_TABLES)) {
                    runStatement(dataStore, SYNCH_UNIT_TABLES_CREATION);
                }
                dataStore.setVersioned(SYNCH_UNITS, false, null, null);

                if (!typeNames.contains(SYNCH_OUTSTANDING)) {
                    runStatement(dataStore, SYNCH_OUTSTANDING_CREATION);
                }
            }

        } catch (Exception e) {
            throw new GSSException("A problem occurred while checking the versioning store", e);
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

    }

    void runStatement(VersionedPostgisDataStore dataStore, String sqlStatement) throws IOException,
            SQLException {
        Connection conn = null;
        Statement st = null;
        try {
            conn = dataStore.getConnection(Transaction.AUTO_COMMIT);
            st = conn.createStatement();
            st.execute(sqlStatement);
        } finally {
            JDBCUtils.close(st);
            JDBCUtils.close(conn, Transaction.AUTO_COMMIT, null);
        }
    }

    public void ensureUnitEnabled() {
        ensureEnabled();

        if (getServiceInfo().getMode() != GSSMode.Unit) {
            throw new GSSException("gss configured in Central mode, won't do Unit service calls");
        }
    }

    public void ensureCentralEnabled() {
        ensureEnabled();

        if (getServiceInfo().getMode() != GSSMode.Central) {
            throw new GSSException("gss configured in Unit mode, won't do synchronisation services");
        }
    }

    /**
     * Finds the versioning datastore configured for this service
     * 
     * @return
     * @throws IOException
     */
    public VersioningDataStore getVersioningStore() throws IOException {
        return (VersioningDataStore) getServiceInfo().getVersioningDataStore().getDataStore(null);
    }

    /**
     * Returns the datastore configuration for this service
     * 
     * @return
     * @throws IOException
     */
    public DataStoreInfo getVersioningStoreInfo() {
        return getServiceInfo().getVersioningDataStore();
    }

    /**
     * Returns the operation mode
     * 
     * @return
     */
    public GSSMode getMode() {
        return getServiceInfo().getMode();
    }

    /**
     * Applies the specified transaction to the provided feature store
     * 
     * @param changes
     * @param store
     */
    public void applyChanges(TransactionType changes,
            FeatureStore<SimpleFeatureType, SimpleFeature> store) throws IOException {
        if (changes == null)
            return;

        List<DeleteElementType> deletes = changes.getDelete();
        List<UpdateElementType> updates = changes.getUpdate();
        List<InsertElementType> inserts = changes.getInsert();
        for (DeleteElementType delete : deletes) {
            store.removeFeatures(delete.getFilter());
        }
        for (UpdateElementType update : updates) {
            List<PropertyType> props = update.getProperty();
            List<AttributeDescriptor> atts = new ArrayList<AttributeDescriptor>(props.size());
            List<Object> values = new ArrayList<Object>(props.size());
            for (PropertyType prop : props) {
                atts.add(store.getSchema().getDescriptor(prop.getName().getLocalPart()));
                values.add(prop.getValue());
            }
            AttributeDescriptor[] attArray = (AttributeDescriptor[]) atts
                    .toArray(new AttributeDescriptor[atts.size()]);
            Object[] valArray = (Object[]) values.toArray(new Object[values.size()]);
            store.modifyFeatures(attArray, valArray, update.getFilter());
        }
        for (InsertElementType insert : inserts) {
            List<SimpleFeature> features = insert.getFeature();
            store.addFeatures(DataUtilities.collection(features));
        }
    }

    /**
     * Returns the number of changes contained in the transaction
     * 
     * @param changes
     * @return
     */
    public int countChanges(TransactionType changes) {
        if (changes == null) {
            return 0;
        }

        int count = 0;
        count += changes.getDelete().size();
        count += changes.getUpdate().size();
        count += changes.getInsert().size();

        return count;
    }

    public static void main(String[] args) {
        System.out.println("Unit tables creation");
        System.out.println("--------------------");
        System.out.println();
        System.out.println(SYNC_TABLES_CREATION);
        System.out.println(SYNCH_HISTORY_CREATION);
        System.out.println(SYNCH_CONFLICTS_CREATION);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("Central tables creation");
        System.out.println("--------------------");
        System.out.println();
        System.out.println(SYNCH_UNITS_CREATION);
        System.out.println(SYNCH_UNIT_TABLES_CREATION);
        System.out.println(SYNCH_OUTSTANDING_CREATION);
    }
}
