/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.restupload;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.util.RESTUploadPathMapperImpl;
import org.geoserver.rest.util.RESTUtils;

/**
 * {@link RESTUploadPathMapperImpl} implementation for the resumable REST upload
 *
 * @author Nicola Lagomarsini
 */
public class ResumableUploadPathMapper extends RESTUploadPathMapperImpl {

    private static final String DEFAULT_ROOT_DIRECTORY = "data";

    private String sourcePath;

    public ResumableUploadPathMapper(Catalog catalog) {
        super(catalog);
    }

    @Override
    public void mapItemPath(
            String workspace,
            String store,
            Map<String, String> storeParams,
            StringBuilder itemPath,
            String sourcePath)
            throws IOException {
        super.mapItemPath(workspace, store, storeParams, itemPath, sourcePath);
        if (!canExecute(sourcePath.toString())) {
            return;
        }
        String root = RESTUtils.getRootDirectory(workspace, store, catalog);
        if (root == null || root.isEmpty()) {
            root = DEFAULT_ROOT_DIRECTORY;
        }
        String destination = FilenameUtils.concat(root, itemPath.toString());
        itemPath.setLength(0);
        itemPath.append(destination);
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /*
     * Filter resource: only the file in sourcePath will be processed by this mapper
     */
    private Boolean canExecute(String itemPath) {
        Boolean canExecute = false;
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource tmpUploadFolder = loader.get(sourcePath);
        if (FilenameUtils.normalize(itemPath)
                .startsWith(FilenameUtils.normalize(tmpUploadFolder.toString()))) {
            canExecute = true;
        }
        return canExecute;
    }
}
