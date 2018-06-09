/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geogig.geoserver.web.repository.RepositoriesListPanel;
import org.geoserver.web.GeoServerSecuredPage;

/** Add/edit/remove repositories */
public class RepositoriesPage extends GeoServerSecuredPage {

    private final RepositoriesListPanel table;

    public RepositoriesPage() {

        table = new RepositoriesListPanel("table");
        table.setOutputMarkupId(true);
        add(table);

        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        header.add(new BookmarkablePageLink<String>("importExisting", RepositoryImportPage.class));
        header.add(new BookmarkablePageLink<String>("addNew", RepositoryEditPage.class));

        return header;
    }
}
