/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.catalog.LayerGroupInfo;
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
    private Link addLayersLink;
    
    
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
        header.add(new BookmarkablePageLink("addNew", CreateEoGroupPage.class));
        
        // the add layers button
        addLayersLink = createAddLayersLink("addLayersToSelected", table);
        addLayersLink.setOutputMarkupId(true);
        addLayersLink.setEnabled(false);
        header.add(addLayersLink);
        
        // the removal button
        removalLink = new DeleteEoGroupLink("removeSelected", table, dialog);        
        removalLink.setOutputMarkupId(true);
        removalLink.setEnabled(false);
        header.add(removalLink);

        table.setSelectionLinks(new AbstractLink[] { addLayersLink, removalLink });
        
        return header;
    }
    
    protected Link createAddLayersLink(String id, final LayerGroupTablePanel groupTable) {
        return new Link(id) {
            @Override
            public void onClick() {
                if (groupTable.getSelection().size() > 0) {
                    try {
                        LayerGroupInfo group = groupTable.getSelection().get(0);                        
                        setResponsePage(new AddEoLayerPage(group));
                    } catch (IndexOutOfBoundsException e) {
                        // this shouldn't happen
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, e.getMessage(), e);
                        }
                    }
                }
            }
        };
    }
    
    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}