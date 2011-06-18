package org.geoserver.wfsv;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;

import org.geoserver.data.test.LiveDbmsData;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.jdbc.JDBCDataStore;

/***
 * Live dbms data using a legacy {@link JDBCDataStore} as the data source
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class LegacyDbmsTestData extends LiveDbmsData {

    public LegacyDbmsTestData(File dataDirSourceDirectory, String fixtureId, File sqlScript) {
        super(dataDirSourceDirectory, fixtureId, sqlScript);
    }

    protected Connection getDatabaseConnection(DataStore ds) throws IOException {
        if (ds instanceof JDBCDataStore) {
            return ((JDBCDataStore) ds).getConnection(Transaction.AUTO_COMMIT);
        } else {
            return null;
        }
    }
}
