/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.geoserver.web.data.workspace.WorkspaceProvider.DEFAULT;
import static org.geoserver.web.data.workspace.WorkspaceProvider.ISOLATED;
import static org.geoserver.web.data.workspace.WorkspaceProvider.NAME;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/** Lists available workspaces, links to them, allows for addition and removal */
public class WorkspacePage extends GeoServerSecuredPage {
    private static final long serialVersionUID = 3084639304127909774L;
    WorkspaceProvider provider = new WorkspaceProvider();
    GeoServerTablePanel<WorkspaceInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public WorkspacePage() {
        // the middle table
        add(
                table =
                        new GeoServerTablePanel<WorkspaceInfo>("table", provider, true) {
                            private static final long serialVersionUID = 8028081894753417294L;

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<WorkspaceInfo> itemModel,
                                    Property<WorkspaceInfo> property) {
                                if (property == NAME) {
                                    return workspaceLink(id, itemModel);
                                } else if (property == DEFAULT) {
                                    if (getCatalog()
                                            .getDefaultWorkspace()
                                            .equals(itemModel.getObject()))
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                } else if (property == ISOLATED) {
                                    if (itemModel.getObject().isIsolated())
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                } else if (property == WorkspaceProvider.MODIFIED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            WorkspaceProvider.MODIFIED_TIMESTAMP.getModel(
                                                    itemModel));
                                } else if (property == WorkspaceProvider.CREATED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            WorkspaceProvider.CREATED_TIMESTAMP.getModel(
                                                    itemModel));
                                }

                                throw new IllegalArgumentException(
                                        "No such property " + property.getName());
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(table.getSelection().size() > 0);
                                target.add(removal);
                            }
                        });
        table.setOutputMarkupId(true);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<WorkspaceNewPage>("addNew", WorkspaceNewPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        // check for full admin, we don't allow workspace admins to add new workspaces
        header.setEnabled(isAuthenticatedAsAdmin());
        return header;
    }

    Component workspaceLink(String id, final IModel<WorkspaceInfo> itemModel) {
        IModel<?> nameModel = NAME.getModel(itemModel);
        return new SimpleBookmarkableLink(
                id, WorkspaceEditPage.class, nameModel, "name", (String) nameModel.getObject());
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
