/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.ExtensionPriority;

/**
 * Default implementation of the {@link RESTUploadPathMapper} interface. This implementation simply
 * changes the input file root directory with the one defined inside the {@link MetadataMap} of the
 * {@link SettingsInfo} class.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public class RESTUploadExternalPathMapper extends RESTUploadPathMapperImpl
        implements ExtensionPriority {

    public RESTUploadExternalPathMapper(Catalog catalog) {
        super(catalog);
    }

    public void mapStorePath(
            StringBuilder rootDir, String workspace, String store, Map<String, String> storeParams)
            throws IOException {
        // Get the external root definition from the settings
        String externalRoot = RESTUtils.getRootDirectory(workspace, store, catalog);

        // If nothing is set, then the file root directory is not mapped
        if (externalRoot == null || externalRoot.isEmpty()) {
            return;
        }

        // Removal of the old input root
        rootDir.setLength(0);
        // Setting of the new root directory and workspace and store if present
        rootDir.append(externalRoot);
        // NOTE that each directory ends with the directory name and not with a path separator
        // Appending the Workspace directory if present
        if (workspace != null && !workspace.isEmpty()) {
            rootDir.append(File.separator);
            rootDir.append(workspace);
        }

        // Appending the Store directory if present
        if (store != null && !store.isEmpty()) {
            rootDir.append(File.separator);
            rootDir.append(store);
        }
    }

    public int getPriority() {
        return 0;
    }
}
