/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layergroup.LayerGroupProvider;
import org.geoserver.web.data.layergroup.LayerGroupProviderFilter;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A layer group table panel.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
@SuppressWarnings("serial")
public class LayerGroupTablePanel extends GeoServerTablePanel<LayerGroupInfo> {

    protected AbstractLink[] editSelectionLinks;

    public LayerGroupTablePanel(String id) {
        this(id, null);
    }

    public LayerGroupTablePanel(String id, LayerGroupProviderFilter groupFilter) {
        super("table", new LayerGroupProvider(groupFilter), true);
    }

    public void setSelectionLinks(AbstractLink[] editSelectionLinks) {
        this.editSelectionLinks = editSelectionLinks;
    }

    public AbstractLink[] getSelectionLinks() {
        return editSelectionLinks;
    }

    @Override
    protected Component getComponentForProperty(
            String id, IModel<LayerGroupInfo> itemModel, Property<LayerGroupInfo> property) {
        if (property == LayerGroupProvider.NAME) {
            return createLayerGroupLink(id, itemModel);
        }

        if (property == LayerGroupProvider.WORKSPACE) {
            return createWorkspaceLink(id, itemModel);
        }
        return null;
    }

    @Override
    protected void onSelectionUpdate(AjaxRequestTarget target) {
        boolean canEdit = true;
        if (!getSelection().isEmpty()) {
            if (!isAuthenticatedAsAdmin()) {
                // if any global layer groups are selected, don't allow edit
                for (LayerGroupInfo lg : getSelection()) {
                    if (lg.getWorkspace() == null) {
                        canEdit = false;
                        break;
                    }
                }
            }
        } else {
            canEdit = false;
        }

        if (editSelectionLinks != null) {
            for (AbstractLink link : editSelectionLinks) {
                link.setEnabled(canEdit);
                target.add(link);
            }
        }
    }

    protected Component createLayerGroupLink(String id, IModel itemModel) {
        IModel groupNameModel = LayerGroupProvider.NAME.getModel(itemModel);
        IModel wsModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);

        String groupName = (String) groupNameModel.getObject();
        String wsName = (String) wsModel.getObject();

        return new SimpleBookmarkableLink(
                id,
                EoLayerGroupEditPage.class,
                groupNameModel,
                EoLayerGroupEditPage.GROUP,
                groupName,
                EoLayerGroupEditPage.WORKSPACE,
                wsName);
    }

    protected Component createWorkspaceLink(String id, IModel itemModel) {
        IModel wsNameModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(
                    id, WorkspaceEditPage.class, new Model(wsName), "name", wsName);
        } else {
            return new WebMarkupContainer(id);
        }
    }

    protected boolean isAuthenticatedAsAdmin() {
        return ComponentAuthorizer.ADMIN.isAccessAllowed(
                GeoServerSecuredPage.class, SecurityContextHolder.getContext().getAuthentication());
    }
}
