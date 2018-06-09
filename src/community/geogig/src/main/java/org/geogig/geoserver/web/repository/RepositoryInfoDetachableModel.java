/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;

/**
 * A RepositoryInfo detachable model that holds the store id to retrieve it on demand from the
 * catalog
 */
public class RepositoryInfoDetachableModel extends LoadableDetachableModel<RepositoryInfo> {

    private static final long serialVersionUID = -6829878983583733186L;

    String id;

    public RepositoryInfoDetachableModel(RepositoryInfo repoInfo) {
        super(repoInfo);
        this.id = repoInfo.getId();
    }

    @Override
    protected RepositoryInfo load() {
        return RepositoryManager.get().get(id);
    }
}
