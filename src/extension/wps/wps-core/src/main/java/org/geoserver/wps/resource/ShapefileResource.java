/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.File;
import java.io.IOException;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.shapefile.ShapefileDataStore;

/**
 * Tracks and cleans up a shapefile store contained in its own private directory
 *
 * @author Andrea Aime - OpenGeo
 */
public class ShapefileResource implements WPSResource {
    Resource directory;

    ShapefileDataStore store;

    String name;

    public ShapefileResource(ShapefileDataStore store, File directory) throws IOException {
        this(store, Files.asResource(directory));
    }

    public ShapefileResource(ShapefileDataStore store, Resource directory) throws IOException {
        this.directory = directory;
        this.name = directory.path() + store.getTypeNames()[0] + ".shp";
        this.store = store;
    }

    public void delete() throws Exception {
        try {
            store.dispose();
        } finally {
            directory.delete();
        }
    }

    public String getName() {
        return name;
    }
}
