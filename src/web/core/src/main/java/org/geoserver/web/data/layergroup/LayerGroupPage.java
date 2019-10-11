/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/** Lists layer groups, allows removal and editing */
public class LayerGroupPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 5039809655908312633L;

    GeoServerTablePanel<LayerGroupInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerGroupPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        LayerGroupProvider provider = new LayerGroupProvider();
        add(
                table =
                        new GeoServerTablePanel<LayerGroupInfo>("table", provider, true) {

                            private static final long serialVersionUID = 714777934301159139L;

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<LayerGroupInfo> itemModel,
                                    Property<LayerGroupInfo> property) {

                                if (property == LayerGroupProvider.NAME) {
                                    return layerGroupLink(id, itemModel);
                                }
                                if (property == LayerGroupProvider.WORKSPACE) {
                                    return workspaceLink(id, itemModel);
                                }

                                if (property == LayerGroupProvider.MODIFIED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            LayerGroupProvider.MODIFIED_TIMESTAMP.getModel(
                                                    itemModel));
                                }
                                if (property == LayerGroupProvider.CREATED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            LayerGroupProvider.CREATED_TIMESTAMP.getModel(
                                                    itemModel));
                                }
                                if (property == LayerGroupProvider.ENABLED) {
                                    LayerGroupInfo layerGroupInfo = itemModel.getObject();
                                    // ask for enabled() instead of isEnabled() to account for
                                    // disabled
                                    // resource/store
                                    boolean enabled = layerGroupInfo.isEnabled();
                                    PackageResourceReference icon =
                                            enabled
                                                    ? icons.getEnabledIcon()
                                                    : icons.getDisabledIcon();
                                    Fragment f =
                                            new Fragment(id, "iconFragment", LayerGroupPage.this);
                                    f.add(new Image("layerIcon", icon));
                                    return f;
                                }
                                return null;
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                if (!table.getSelection().isEmpty()) {
                                    boolean canRemove = true;
                                    if (!isAuthenticatedAsAdmin()) {
                                        // if any global layer groups are selected, don't allow
                                        // delete
                                        for (LayerGroupInfo lg : table.getSelection()) {
                                            if (lg.getWorkspace() == null) {
                                                canRemove = false;
                                                break;
                                            }
                                        }
                                    }

                                    removal.setEnabled(canRemove);
                                } else {
                                    removal.setEnabled(false);
                                }
                                target.add(removal);
                            }
                        });

        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new BookmarkablePageLink<LayerGroupEditPage>("addNew", LayerGroupEditPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component layerGroupLink(String id, IModel<LayerGroupInfo> itemModel) {
        IModel<?> groupNameModel = LayerGroupProvider.NAME.getModel(itemModel);
        IModel<?> wsModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);

        String groupName = (String) groupNameModel.getObject();
        String wsName = (String) wsModel.getObject();

        if (wsName == null) {
            return new SimpleBookmarkableLink(
                    id,
                    LayerGroupEditPage.class,
                    groupNameModel,
                    LayerGroupEditPage.GROUP,
                    groupName);
        } else {
            return new SimpleBookmarkableLink(
                    id,
                    LayerGroupEditPage.class,
                    groupNameModel,
                    LayerGroupEditPage.GROUP,
                    groupName,
                    LayerGroupEditPage.WORKSPACE,
                    wsName);
        }
    }

    Component workspaceLink(String id, IModel<LayerGroupInfo> itemModel) {
        IModel<?> wsNameModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(
                    id, WorkspaceEditPage.class, new Model<String>(wsName), "name", wsName);
        } else {
            return new WebMarkupContainer(id);
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
