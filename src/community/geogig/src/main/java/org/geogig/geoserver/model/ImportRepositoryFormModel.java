/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import java.net.URI;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.ImportRepositoryFormBean;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;

/**
 * Data model for importing an existing GeoGig repository. This model essentially wraps a {@link
 * RepositoryInfo} and uses {@link ImportRepositoryFormBean} to hold the UI pieces that are used to
 * construct the repository location {@link java.net.URI}.
 */
public class ImportRepositoryFormModel implements IModel<ImportRepositoryFormBean> {

    private static final long serialVersionUID = 1L;

    private final IModel<RepositoryInfo> repoInfoModel;
    private ImportRepositoryFormBean bean;

    public ImportRepositoryFormModel(IModel<RepositoryInfo> repoInfoModel) {
        this.repoInfoModel = repoInfoModel;
    }

    @Override
    public ImportRepositoryFormBean getObject() {
        if (bean == null) {
            bean = new ImportRepositoryFormBean();
            RepositoryInfo repoInfo = repoInfoModel.getObject();
            if (repoInfo != null && repoInfo.getLocation() != null) {
                final URI uri = repoInfo.getLocation();
                // get the scheme
                final String scheme = uri.getScheme();
                final String path = uri.getPath();
                switch (scheme) {
                    case "file":
                        // parse the driectroy out of the path
                        bean.setRepoDirectory(path);
                    case "postgresql":
                        // build the pgBean
                        bean.setPgBean(PostgresConfigBean.from(uri));
                        // get the name
                        final String repoName = repoInfo.getRepoName();
                        bean.setRepoName(repoName);
                }
            }
        }
        return bean;
    }

    @Override
    public void setObject(ImportRepositoryFormBean object) {
        this.bean = object;
    }

    @Override
    public void detach() {
        if (repoInfoModel != null) {
            repoInfoModel.detach();
        }
        bean = null;
    }
}
