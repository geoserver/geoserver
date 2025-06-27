/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;

/** Abstract base class for file managers handling resources associated with feature types or workspaces. */
public abstract class FileManagerBase<T> {

    protected Catalog catalog;
    protected GeoServerDataDirectory dd;

    public FileManagerBase(Catalog catalog, GeoServerDataDirectory dd) {
        this.catalog = catalog;
        this.dd = dd;
    }

    protected abstract String getFeatureType(T info);

    protected abstract String getWorkspace(T info);

    protected abstract String getName(T info);

    protected abstract String getExtension(T info);

    protected abstract String getDir();

    protected abstract String getFileType();

    public Resource getResource(T info) {
        String featureType = getFeatureType(info);
        String workspace = getWorkspace(info);
        String name = getName(info);
        String extension = getExtension(info);
        Resource resource;
        if (featureType != null) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureType);
            resource = dd.get(fti, name + "." + extension);
        } else if (workspace != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
            resource = dd.get(ws, name + "." + extension);
        } else {
            resource = dd.get(getDir(), name + "." + extension);
        }
        return resource;
    }

    public boolean delete(T info) {
        return getResource(info).delete();
    }

    public File getLocation(T info) {
        String featureType = getFeatureType(info);
        String workspace = getWorkspace(info);
        Resource resource;
        if (featureType != null) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureType);
            resource = dd.get(fti);
        } else if (workspace != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
            resource = dd.get(ws);
        } else {
            resource = dd.get(getDir());
        }
        File destDir = resource.dir();
        if (!destDir.exists() || !destDir.isDirectory()) {
            destDir.mkdir();
        }
        return destDir;
    }

    public void saveFile(T info, String rawContent) {
        File destDir = getLocation(info);
        try {
            File file = new File(destDir, getName(info) + "." + getExtension(info));
            if (!file.exists()) file.createNewFile();
            synchronized (this) {
                try (FileOutputStream fos = new FileOutputStream(file, false)) {
                    fos.write(rawContent.getBytes());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
