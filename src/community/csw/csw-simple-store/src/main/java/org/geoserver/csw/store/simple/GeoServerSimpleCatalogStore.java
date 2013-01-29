/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.IOException;

import org.geoserver.config.GeoServerDataDirectory;

/**
 * An implementation of {@link SimpleCatalogStore} that is going to find the records in the
 * "catalog" subdirectory of the GeoServer data directory
 */
public class GeoServerSimpleCatalogStore extends SimpleCatalogStore {

    public GeoServerSimpleCatalogStore(GeoServerDataDirectory dataDirectory) throws IOException {
        super(dataDirectory.findOrCreateDir("catalog"));
    }

}
