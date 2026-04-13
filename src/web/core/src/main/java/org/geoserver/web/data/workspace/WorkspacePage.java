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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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

    private String targetWorkspaceStr = null;
    private String targetLayerStr = null;
    private String targetGroupStr = null;

    WorkspaceProvider provider = new WorkspaceProvider() {

        @Override
        protected Filter getContextFilter() {

            if (targetGroupStr != null) {
                String targetGroup =
                        (targetWorkspaceStr != null) ? targetWorkspaceStr + ":" + targetGroupStr : targetGroupStr;
                LayerGroupInfo gi = getCatalog().getLayerGroupByName(targetGroup);
                if (gi != null) {
                    LayerGroupHelper helper = new LayerGroupHelper(gi);
                    List<String> ids = new ArrayList<>();
                    for (LayerInfo li : helper.allLayers()) {
                        if (li.getResource() != null
                                && li.getResource().getStore() != null
                                && li.getResource().getStore().getWorkspace() != null) {
                            ids.add(li.getResource().getStore().getWorkspace().getId());
                        }
                    }
                    return ids.isEmpty() ? Filter.EXCLUDE : Predicates.in("id", ids);
                }
                return Filter.EXCLUDE;
            }

            if (targetLayerStr != null) {
                String targetLayer =
                        (targetWorkspaceStr != null) ? targetWorkspaceStr + ":" + targetLayerStr : targetLayerStr;

                LayerGroupInfo gi = getCatalog().getLayerGroupByName(targetLayer);
                if (gi != null) {
                    LayerGroupHelper helper = new LayerGroupHelper(gi);
                    List<String> ids = new ArrayList<>();
                    for (LayerInfo li : helper.allLayers()) {
                        if (li.getResource() != null
                                && li.getResource().getStore() != null
                                && li.getResource().getStore().getWorkspace() != null) {
                            ids.add(li.getResource().getStore().getWorkspace().getId());
                        }
                    }
                    return ids.isEmpty() ? Filter.EXCLUDE : Predicates.in("id", ids);
                }

                LayerInfo li = getCatalog().getLayerByName(targetLayer);
                if (li != null
                        && li.getResource() != null
                        && li.getResource().getStore() != null
                        && li.getResource().getStore().getWorkspace() != null) {
                    String workspaceId =
                            li.getResource().getStore().getWorkspace().getId();
                    return Predicates.equal("id", workspaceId);
                }

                return Filter.EXCLUDE;
            }

            if (targetWorkspaceStr != null) {
                return Predicates.equal("name", targetWorkspaceStr);
            }

            return null;
        }
    };
    GeoServerTablePanel<WorkspaceInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public WorkspacePage(PageParameters parameters) {

        StringValue wsParam = parameters.get("workspace");
        StringValue layerParam = parameters.get("layer");
        StringValue groupParam = parameters.get("group");

        if (!wsParam.isEmpty()) {
            this.targetWorkspaceStr = wsParam.toString();
        }
        if (!layerParam.isEmpty()) {
            this.targetLayerStr = layerParam.toString();
        }
        if (!groupParam.isEmpty()) {
            this.targetGroupStr = groupParam.toString();
        }
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

    public WorkspacePage() {
        this(new PageParameters());
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
