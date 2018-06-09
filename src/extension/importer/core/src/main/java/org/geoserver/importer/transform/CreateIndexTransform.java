/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geotools.data.DataAccess;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class CreateIndexTransform extends AbstractTransform
        implements PostTransform, VectorTransform {

    private static final long serialVersionUID = 1L;

    private String field;

    public CreateIndexTransform(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void apply(ImportTask task, ImportData data) throws Exception {
        DataStoreInfo storeInfo = (DataStoreInfo) task.getStore();
        DataAccess store = storeInfo.getDataStore(null);
        if (store instanceof JDBCDataStore) {
            createIndex(task, (JDBCDataStore) store);
        } else {
            task.addMessage(
                    Level.WARNING, "Cannot create index on non database target. Not a big deal.");
        }
    }

    private void createIndex(ImportTask item, JDBCDataStore store) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        Exception error = null;
        String sql = null;
        try {
            conn = store.getConnection(Transaction.AUTO_COMMIT);
            stmt = conn.createStatement();
            String tableName = item.getLayer().getResource().getNativeName();
            String indexName = "\"" + tableName + "_" + field + "\"";
            sql = "CREATE INDEX " + indexName + " ON \"" + tableName + "\" (\"" + field + "\")";
            stmt.execute(sql);
        } catch (SQLException sqle) {
            error = sqle;
        } finally {
            store.closeSafe(stmt);
            store.closeSafe(conn);
        }
        if (error != null) {
            throw new Exception("Error creating index, SQL was : " + sql, error);
        }
    }
}
