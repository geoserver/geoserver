/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.io.File;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.mongodb.data.SchemaStoreDirectory;
import org.geotools.data.mongodb.data.SchemaStoreDirectoryProvider;

/** This class provides a location for MongoDB HTTP schemas inside GeoServer data directory. */
public class GeoserverSchemaDirecotry implements SchemaStoreDirectory {

    private File workspaceDirectory;
    private String name = "geoserver workspace directory";
    private int priority = 10;

    public GeoserverSchemaDirecotry(DataDirectoryResourceStore dataDir) {
        // write in geoserver_data_dir/mongodb_http_schemas/..
        Resource resource = dataDir.get("");
        workspaceDirectory = new File(resource.dir(), "mongodb_http_schemas");
        SchemaStoreDirectoryProvider.addStoreDirectory(this);
    }

    @Override
    public File getDirectory() {
        return workspaceDirectory;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getName() {
        return name;
    }
}
