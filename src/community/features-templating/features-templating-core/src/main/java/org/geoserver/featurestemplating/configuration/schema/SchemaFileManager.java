/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;

/** Helper class that provides methods to manage the template file. */
public class SchemaFileManager {

    private Catalog catalog;
    private GeoServerDataDirectory dd;

    public SchemaFileManager(Catalog catalog, GeoServerDataDirectory dd) {
        this.catalog = catalog;
        this.dd = dd;
    }

    /** @return the singleton instance of this class. */
    public static SchemaFileManager get() {
        return GeoServerExtensions.bean(SchemaFileManager.class);
    }

    /**
     * Return a {@link Resource} from a template info.
     *
     * @param templateInfo the template info for which we want to retrieve the corresponding resource.
     * @return the resource that corresponds to the template info.
     */
    public Resource getSchemaResource(SchemaInfo templateInfo) {
        String featureType = templateInfo.getFeatureType();
        String workspace = templateInfo.getWorkspace();
        String schemaName = templateInfo.getSchemaName();
        String extension = templateInfo.getExtension();
        Resource resource;
        if (featureType != null) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureType);
            resource = dd.get(fti, schemaName + "." + extension);
        } else if (workspace != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
            resource = dd.get(ws, schemaName + "." + extension);
        } else {
            resource = dd.get(SchemaInfoDAOImpl.SCHEMA_DIR, schemaName + "." + extension);
        }
        return resource;
    }

    /**
     * Delete the template file associated to the template info passed as an argument.
     *
     * @param schemaInfo the templateInfo for which we want to delete the corresponding template file.
     * @return true if the delete process was successful false otherwise.
     */
    public boolean delete(SchemaInfo schemaInfo) {
        return getSchemaResource(schemaInfo).delete();
    }

    /**
     * Return the directory where the template file is as a File object.
     *
     * @param schemaInfo the template info to which the desired template file is associated.
     * @return the directoryu where the template file associated to the templateInfo is placed.
     */
    public File getTemplateLocation(SchemaInfo schemaInfo) {
        String featureType = schemaInfo.getFeatureType();
        String workspace = schemaInfo.getWorkspace();
        Resource resource = null;
        if (featureType != null) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureType);
            resource = dd.get(fti);
        } else if (workspace != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
            resource = dd.get(ws);
        } else {
            resource = dd.get(SchemaInfoDAOImpl.SCHEMA_DIR);
        }
        File destDir = resource.dir();
        if (!destDir.exists() || !destDir.isDirectory()) {
            destDir.mkdir();
        }
        return destDir;
    }

    /**
     * Save a template in string form to the directory defined for the template info object.
     *
     * @param schemaInfo the template info object.
     * @param rawTemplate the template content to save to a file.
     */
    public void saveTemplateFile(SchemaInfo schemaInfo, String rawTemplate) {
        File destDir = getTemplateLocation(schemaInfo);
        try {
            File file = new File(destDir, schemaInfo.getSchemaName() + "." + schemaInfo.getExtension());
            if (!file.exists()) file.createNewFile();
            synchronized (this) {
                try (FileOutputStream fos = new FileOutputStream(file, false)) {
                    fos.write(rawTemplate.getBytes());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
