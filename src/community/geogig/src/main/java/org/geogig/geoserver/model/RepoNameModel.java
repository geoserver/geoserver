/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;

/**
 * Wicket IModel implementation for storing a GeoGig repository name. This model is used by all
 * GeoGig repository configuration types and wraps {@link RepositoryInfo} to handle the repo name
 * for the UI.
 */
public class RepoNameModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    private final IModel<RepositoryInfo> repoModel;
    private String repoName;

    public RepoNameModel(IModel<RepositoryInfo> repoModel) {
        super();
        this.repoModel = repoModel;
    }

    @Override
    public String getObject() {
        if (repoName == null) {
            repoName = repoModel.getObject().getRepoName();
        }
        return repoName;
    }

    @Override
    public void setObject(String object) {
        repoName = object;
    }

    @Override
    public void detach() {
        if (repoModel != null) {
            repoModel.detach();
        }
        repoName = null;
    }
}
