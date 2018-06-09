/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

public class VirtualTableCallback implements FeatureTypeCallback {

    @Override
    public boolean canHandle(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        return dataAccess instanceof JDBCDataStore
                && info.getMetadata() != null
                && (info.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE)
                        instanceof VirtualTable);
    }

    @Override
    public boolean initialize(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {
        JDBCDataStore jstore = (JDBCDataStore) dataAccess;
        VirtualTable vt =
                info.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class);

        FeatureType ft = null;
        // building the virtual table structure is expensive, see if the VT is already registered in
        // the db
        if (jstore.getVirtualTables().containsValue(vt)) {
            // if the virtual table is already registered in the store (and equality in the test
            // above guarantees the structure is the same), we can just get the schema from it
            // directly
            ft = jstore.getSchema(vt.getName());
            // paranoid check: make sure nobody changed the vt structure while we fetched
            // the data (rather unlikely, even more unlikely would be
            if (!jstore.getVirtualTables().containsValue(vt)) {
                ft = null;
            }
        }

        if (ft == null) {
            if (temporaryName != null) {
                jstore.createVirtualTable(new VirtualTable(temporaryName.getLocalPart(), vt));
                return true;
            } else {
                jstore.createVirtualTable(vt);
            }
        }

        return false;
    }

    @Override
    public void flush(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess)
            throws IOException {
        // nothing to do
    }

    @Override
    public void dispose(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {
        JDBCDataStore ds = (JDBCDataStore) dataAccess;
        if (temporaryName != null) {
            ds.dropVirtualTable(temporaryName.getLocalPart());
        } else {
            ds.dropVirtualTable(info.getNativeName());
        }
    }
}
