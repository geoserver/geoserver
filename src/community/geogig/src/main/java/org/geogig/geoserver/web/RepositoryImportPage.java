/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import javax.annotation.Nullable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.web.repository.RepositoryImportFormPanel;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Page object for importing existing GeoGig repositories. This is very similar to {@link
 * RepositoryEditPage}, but uses a {@link RepositoryImportFormPanel} instead of an edit panel.
 */
public class RepositoryImportPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 1L;

    public RepositoryImportPage() {
        this(null);
    }

    public RepositoryImportPage(@Nullable IModel<RepositoryInfo> repoInfo) {
        super();
        add(
                new RepositoryImportFormPanel("panel", repoInfo) {
                    private static final long serialVersionUID = 1L;

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
