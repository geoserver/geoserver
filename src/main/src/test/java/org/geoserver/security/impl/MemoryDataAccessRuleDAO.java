/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Properties;

import org.geoserver.catalog.Catalog;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.vfny.geoserver.global.ConfigurationException;

/**
 * A {@link DataAccessRuleDAO} variant that lives in memory
 */
class MemoryDataAccessRuleDAO extends DataAccessRuleDAO {
    
    public MemoryDataAccessRuleDAO(Catalog rawCatalog, Properties props) throws ConfigurationException {
        super(rawCatalog, null);
        loadRules(props);
    }
    
    @Override
    void checkPropertyFile(boolean force) {
        // skip checking
        lastModified = Long.MAX_VALUE;
    }
}