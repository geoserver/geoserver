/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.Collections;
import java.util.List;

import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogCapabilities {

    /**
     * True if the store supports transactions (insert, update, delete), false otherwise
     */
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Returns the list of queriable properties supported by this implementation for the given type
     * name (empty by default)
     * 
     * @param typeName
     * @return
     */
    public List<Name> getQueriables(Name typeName) {
        return Collections.emptyList();
    }
}
