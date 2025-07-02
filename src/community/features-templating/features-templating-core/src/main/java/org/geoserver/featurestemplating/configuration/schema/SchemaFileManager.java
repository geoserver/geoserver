/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.configuration.FileManagerBase;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;

/** Helper class that provides methods to manage the schema file. */
public class SchemaFileManager extends FileManagerBase<SchemaInfo> {

    public SchemaFileManager(Catalog catalog, GeoServerDataDirectory dd) {
        super(catalog, dd);
    }

    /** @return the singleton instance of this class. */
    public static SchemaFileManager get() {
        return GeoServerExtensions.bean(SchemaFileManager.class);
    }

    @Override
    protected String getFeatureType(SchemaInfo info) {
        return info.getFeatureType();
    }

    @Override
    protected String getWorkspace(SchemaInfo info) {
        return info.getWorkspace();
    }

    @Override
    protected String getName(SchemaInfo info) {
        return info.getSchemaName();
    }

    @Override
    protected String getExtension(SchemaInfo info) {
        return info.getExtension();
    }

    @Override
    protected String getDir() {
        return SchemaInfoDAOImpl.SCHEMA_DIR;
    }

    @Override
    protected String getFileType() {
        return "schema";
    }

    /**
     * Return a {@link Resource} from a schema info.
     *
     * @param schemaInfo the schema info for which we want to retrieve the corresponding resource.
     * @return the resource that corresponds to the schema info.
     */
    public Resource getSchemaResource(SchemaInfo schemaInfo) {
        return getResource(schemaInfo);
    }

    /**
     * Delete the schema file associated to the schema info passed as an argument.
     *
     * @param schemaInfo the schemaInfo for which we want to delete the corresponding schema file.
     * @return true if the delete process was successful false otherwise.
     */
    public boolean delete(SchemaInfo schemaInfo) {
        return super.delete(schemaInfo);
    }

    /**
     * Return the directory where the schema file is as a File object.
     *
     * @param schemaInfo the schema info to which the desired schema file is associated.
     * @return the directory where the schema file associated to the schemaInfo is placed.
     */
    public File getSchemaLocation(SchemaInfo schemaInfo) {
        return getLocation(schemaInfo);
    }

    /**
     * Save a schema in string form to the directory defined for the schema info object.
     *
     * @param schemaInfo the schema info object.
     * @param rawSchema the schema content to save to a file.
     */
    public void saveSchemaFile(SchemaInfo schemaInfo, String rawSchema) {
        saveFile(schemaInfo, rawSchema);
    }
}
