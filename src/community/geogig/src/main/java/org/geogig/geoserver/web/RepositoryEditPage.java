/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import javax.annotation.Nullable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.web.repository.RepositoryEditFormPanel;
import org.geoserver.web.GeoServerSecuredPage;

/** */
public class RepositoryEditPage extends GeoServerSecuredPage {

    public RepositoryEditPage() {
        this(null);
    }

    public RepositoryEditPage(@Nullable IModel<RepositoryInfo> repoInfo) {
        super();
        add(
                new RepositoryEditFormPanel("panel", repoInfo) {
                    private static final long serialVersionUID = -2629733074852452891L;

                    @Override
                    protected void saved(RepositoryInfo info, AjaxRequestTarget target) {
                        setResponsePage(RepositoriesPage.class);
                    }

                    @Override
                    protected void cancelled(AjaxRequestTarget target) {
                        setResponsePage(RepositoriesPage.class);
                    }
                });
    }
}
