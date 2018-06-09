/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.DataAccessEditPage;

public class DataStoreEditPage extends DataAccessEditPage {

    public DataStoreEditPage(DataStoreInfo store) {
        super(store);
    }

    @Override
    protected void doSaveStore(DataStoreInfo info) {
        if (info.getId() != null) {
            super.doSaveStore(info);
        }

        // do nothing, not part of catalog yet
    }
}
