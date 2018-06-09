/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.vfny.geoserver.util.DataStoreUtils;

public class Database extends ImportData {

    /** Database connection parameters */
    Map<String, Serializable> parameters;

    /** List of tables */
    List<Table> tables = new ArrayList<Table>();

    public Database(Map<String, Serializable> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public List<Table> getTables() {
        return tables;
    }

    @Override
    public String getName() {
        String database = (String) parameters.get(JDBCDataStoreFactory.DATABASE.key);
        if (database != null) {
            // file based databases might be a full path to a file (sqlite, h2, etc..) use only
            // the last part
            database = FilenameUtils.getBaseName(database);
        }
        return database;
    }

    /** Loads the available tables from this database. */
    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        tables = new ArrayList<Table>();
        DataStoreFactorySpi factory =
                (DataStoreFactorySpi) DataStoreUtils.aquireFactory(parameters);
        if (factory == null) {
            throw new IOException("Unable to find data store for specified parameters");
        }

        m.setTask("Loading tables");
        DataStore store = factory.createDataStore(parameters);
        if (store == null) {
            throw new IOException("Unable to create data store from specified parameters");
        }

        format = DataFormat.lookup(parameters);

        try {
            for (String typeName : store.getTypeNames()) {
                Table tbl = new Table(typeName, this);
                tbl.setFormat(format);
                tables.add(tbl);
            }
        } finally {
            // TODO: cache the datastore for subsquent calls
            store.dispose();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parameters.containsKey(JDBCDataStoreFactory.USER.key)) {
            sb.append(parameters.get(JDBCDataStoreFactory.USER.key)).append("@");
        }
        if (parameters.containsKey(JDBCDataStoreFactory.HOST.key)) {
            sb.append(parameters.get(JDBCDataStoreFactory.HOST.key));
        }
        if (parameters.containsKey(JDBCDataStoreFactory.PORT.key)) {
            sb.append(":").append(parameters.get(JDBCDataStoreFactory.PORT.key));
        }
        if (sb.length() > 0) {
            sb.append("/");
        }
        sb.append(getName());
        return sb.toString();
    }

    @Override
    public void reattach() {
        for (Table t : tables) {
            t.setDatabase(this);
        }
    }

    @Override
    public Table part(final String name) {
        return Iterables.find(
                tables,
                new Predicate<Table>() {
                    @Override
                    public boolean apply(Table input) {
                        return name.equals(input.getName());
                    }
                });
    }

    private Object readResolve() {
        tables = tables != null ? tables : new ArrayList();
        return this;
    }
}
