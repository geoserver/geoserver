/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.geoserver.catalog.CoverageStoreInfo;

public class CoverageStoreEditPage extends org.geoserver.web.data.store.CoverageStoreEditPage {

    public CoverageStoreEditPage(CoverageStoreInfo store) {
        super(store);
    }

    @Override
    protected boolean doSaveStore(CoverageStoreInfo info) {
        if (info.getId() != null) {
            return super.doSaveStore(info);
        }
        // do nothing, not part of catalog yet
        return true;
    }
}
