/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;

/**
 * Wicket IModel implementation for storing the parent directory of a GeoGig repository that has a
 * local directory based backend (not a remote PostgreSQL backend). This is essentially a wrapping
 * IModel implementation to help transform {@link RepositoryInfo} data into a UI component for the
 * parent directory of a local directory based GeoGig backend.
 */
public class RepoDirModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    private final IModel<RepositoryInfo> repoModel;
    private String parentDirectory;

    public RepoDirModel(IModel<RepositoryInfo> repoModel) {
        this.repoModel = repoModel;
    }

    @Override
    public String getObject() {
        if (parentDirectory == null) {
            // get the directory form the URI
            URI fileLocation = repoModel.getObject().getLocation();
            if (fileLocation != null && "file".equals(fileLocation.getScheme())) {
                Path file = Paths.get(fileLocation);
                parentDirectory = file.normalize().getParent().toString();
            }
        }
        return parentDirectory;
    }

    @Override
    public void setObject(String object) {
        this.parentDirectory = object;
    }

    @Override
    public void detach() {
        if (repoModel != null) {
            repoModel.detach();
        }
        this.parentDirectory = null;
    }
}
