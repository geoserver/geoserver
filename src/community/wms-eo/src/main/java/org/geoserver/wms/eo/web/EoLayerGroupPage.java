/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * Lists EO layer groups, allows removal and editing.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
@SuppressWarnings("serial")
public class EoLayerGroupPage extends GeoServerSecuredPage {

    private LayerGroupTablePanel table;
    private GeoServerDialog dialog;
    private DeleteEoGroupLink removalLink;

    public EoLayerGroupPage() {
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));

        table = new LayerGroupTablePanel("table", EoLayerGroupProviderFilter.INSTANCE);
        table.setOutputMarkupId(true);
        add(table);

        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", EoLayerGroupNewPage.class));

        // the removal button
        removalLink = new DeleteEoGroupLink("removeSelected", table, dialog);
        removalLink.setOutputMarkupId(true);
        removalLink.setEnabled(false);
        header.add(removalLink);

        table.setSelectionLinks(new AbstractLink[] {removalLink});

        return header;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
