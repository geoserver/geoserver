/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.Wrapper;
import org.geotools.data.DataStore;

/**
 * Delegates every method to the wrapped feature source. Subclasses will override selected methods
 * to perform their "decoration" job
 *
 * @author Andrea Aime - TOPP
 * @deprecated use org.geotools.data.store.DecoratingDataStore
 */
public abstract class DecoratingDataStore extends org.geotools.data.store.DecoratingDataStore
        implements Wrapper {

    public DecoratingDataStore(DataStore delegate) {
        super(delegate);
    }
}
