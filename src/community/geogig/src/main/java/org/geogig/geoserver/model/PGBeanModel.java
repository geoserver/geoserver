/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import java.net.URI;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;

/**
 * Wicket IModel implementation for PostgreSQL configuration bean. This is essentially a wrapping
 * IModel implementation to help transform {@link RepositoryInfo} data into the UI components for
 * configuring a PostgreSQL backend for GeoGig.
 */
public class PGBeanModel implements IModel<PostgresConfigBean> {

    private static final long serialVersionUID = 1L;

    private final IModel<RepositoryInfo> repoModel;
    private PostgresConfigBean bean;

    public PGBeanModel(IModel<RepositoryInfo> repoModel) {
        this.repoModel = repoModel;
    }

    @Override
    public PostgresConfigBean getObject() {
        if (bean == null) {
            // build the bean from the URI in the repo model, if it's a PostgreSQL location.
            URI location = repoModel.getObject().getLocation();
            if (location != null && "postgresql".equals(location.getScheme())) {
                // build a bean from the parts
                bean = PostgresConfigBean.from(location);
            } else {
                bean = PostgresConfigBean.newInstance();
            }
        }
        return bean;
    }

    @Override
    public void setObject(PostgresConfigBean object) {
        bean = object;
    }

    @Override
    public void detach() {
        if (repoModel != null) {
            repoModel.detach();
        }
        bean = null;
    }
}
