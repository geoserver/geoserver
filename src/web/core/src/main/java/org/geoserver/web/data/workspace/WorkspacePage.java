/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.geoserver.web.data.workspace.WorkspaceProvider.DEFAULT;
import static org.geoserver.web.data.workspace.WorkspaceProvider.ISOLATED;
import static org.geoserver.web.data.workspace.WorkspaceProvider.NAME;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.StringValue;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GsIcon;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geotools.api.filter.Filter;

/** Lists available workspaces, links to them, allows for addition and removal */
public class WorkspacePage extends GeoServerSecuredPage {
    @Serial
    private static final long serialVersionUID = 3084639304127909774L;

    WorkspaceProvider provider = new WorkspaceProvider() {

        @Override
        protected Filter getFilter() {
            Filter baseFilter = Objects.requireNonNull(super.getFilter());
            StringValue wsParam = getPageParameters().get("workspace");
            StringValue layerParam = getPageParameters().get("layer");

            if (!layerParam.isEmpty()) {
                String targetLayer;
                if (!wsParam.isEmpty()) {
                    targetLayer = wsParam.toString() + ":" + layerParam.toString();
                } else {
                    targetLayer = layerParam.toString();
                }
                LayerGroupInfo gi = getCatalog().getLayerGroupByName(targetLayer);
                if (gi != null) {
                    LayerGroupHelper helper = new LayerGroupHelper(gi);
                    List<String> ids = new ArrayList<>();
                    for (LayerInfo li : helper.allLayers()) {
                        if (li.getResource() != null
                                && li.getResource().getStore() != null
                                && li.getResource().getStore().getWorkspace() != null) {
                            String workspaceId =
                                    li.getResource().getStore().getWorkspace().getId();
                            ids.add(workspaceId);
                        }
                    }
                    return Predicates.and(baseFilter, Predicates.in("id", ids));
                }
                LayerInfo li = getCatalog().getLayerByName(targetLayer);
                if (li != null) {
                    if (li.getResource() != null
                            && li.getResource().getStore() != null
                            && li.getResource().getStore().getWorkspace() != null) {
                        String workspaceId =
                                li.getResource().getStore().getWorkspace().getId();
                        return Predicates.and(baseFilter, Predicates.equal("id", workspaceId));
                    }
                }
            }
            if (!wsParam.isEmpty()) {
                String targetWs = wsParam.toString();
                Filter workspaceFilter = Predicates.equal("resource.store.workspace.name", targetWs);
                return Predicates.and(baseFilter, workspaceFilter);
            }
            return baseFilter;
        }
    };
    GeoServerTablePanel<WorkspaceInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public WorkspacePage() {
        // the middle table
        add(
                table = new GeoServerTablePanel<>("table", provider, true) {
                    @Serial
                    private static final long serialVersionUID = 8028081894753417294L;

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<WorkspaceInfo> itemModel, Property<WorkspaceInfo> property) {
                        if (property == NAME) {
                            return workspaceLink(id, itemModel);
                        } else if (property == DEFAULT) {
                            if (getCatalog().getDefaultWorkspace().equals(itemModel.getObject()))
                                return new GsIcon(id, CatalogIconFactory.ENABLED_ICON);
                            else return new Label(id, "");
                        } else if (property == ISOLATED) {
                            if (itemModel.getObject().isIsolated())
                                return new GsIcon(id, CatalogIconFactory.ENABLED_ICON);
                            else return new Label(id, "");
                        } else if (property == WorkspaceProvider.MODIFIED_TIMESTAMP) {
                            return new DateTimeLabel(id, WorkspaceProvider.MODIFIED_TIMESTAMP.getModel(itemModel));
                        } else if (property == WorkspaceProvider.CREATED_TIMESTAMP) {
                            return new DateTimeLabel(id, WorkspaceProvider.CREATED_TIMESTAMP.getModel(itemModel));
                        } else if (property == WorkspaceProvider.MODIFIED_BY) {
                            return new Label(id, WorkspaceProvider.MODIFIED_BY.getModel(itemModel));
                        }

                        throw new IllegalArgumentException("No such property " + property.getName());
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        removal.setEnabled(!table.getSelection().isEmpty());
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
        header.add(new BookmarkablePageLink<>("addNew", WorkspaceNewPage.class));

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
                id, WorkspaceEditPage.class, nameModel, "workspace", (String) nameModel.getObject());
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
