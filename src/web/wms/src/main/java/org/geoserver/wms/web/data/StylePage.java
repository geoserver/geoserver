/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geoserver.web.wicket.StyleFormatLabel;
import org.geotools.api.filter.Filter;

/** Page listing all the styles, allows to edit, add, remove styles */
@SuppressWarnings("serial")
public class StylePage extends GeoServerSecuredPage {

    GeoServerTablePanel<StyleInfo> table;

    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public StylePage() {
        StyleProvider provider = new StyleProvider() {
            @Override
            protected Filter getFilter() {
                Filter baseFilter = Objects.requireNonNull(super.getFilter());

                StringValue wsParam = getPageParameters().get("workspace");
                StringValue layerParam = getPageParameters().get("layer");

                String targetWs = wsParam != null ? wsParam.toOptionalString() : null;
                String targetLayer = layerParam != null ? layerParam.toOptionalString() : null;

                // If a layer is specified, resolve it to styles and filter by those.
                if (targetLayer != null && !targetLayer.isEmpty()) {
                    Set<String> styleIds = new LinkedHashSet<>();

                    LayerInfo layer = getCatalog().getLayerByName(targetLayer);
                    if (layer != null) {
                        collectStyleIds(styleIds, layer.getDefaultStyle(), targetWs);
                        if (layer.getStyles() != null) {
                            for (StyleInfo s : layer.getStyles()) {
                                collectStyleIds(styleIds, s, targetWs);
                            }
                        }
                    } else {
                        LayerGroupInfo layerGroup = getCatalog().getLayerGroupByName(targetLayer);
                        if (layerGroup != null) {
                            for (LayerInfo li : layerGroup.layers()) {
                                if (li == null) continue;
                                collectStyleIds(styleIds, li.getDefaultStyle(), targetWs);
                                if (li.getStyles() != null) {
                                    for (StyleInfo s : li.getStyles()) {
                                        collectStyleIds(styleIds, s, targetWs);
                                    }
                                }
                            }
                        }
                    }

                    if (styleIds.isEmpty()) {
                        return Objects.requireNonNull(Predicates.acceptNone());
                    }

                    Filter combined = Predicates.and(
                            Objects.requireNonNull(baseFilter),
                            Objects.requireNonNull(Predicates.in("id", new ArrayList<>(styleIds))));
                    return Objects.requireNonNull(combined);
                }

                // If only workspace is specified, filter by style workspace.
                if (targetWs != null && !targetWs.isEmpty()) {
                    Filter combined = Predicates.and(
                            Objects.requireNonNull(baseFilter),
                            Objects.requireNonNull(Predicates.equal("workspace.name", targetWs)));
                    return Objects.requireNonNull(combined);
                }

                return baseFilter;
            }

            private void collectStyleIds(Set<String> styleIds, StyleInfo style, String targetWs) {
                if (style == null) return;
                if (style.getId() == null) return;
                if (targetWs != null && !targetWs.isEmpty()) {
                    // Keep global (workspace-less) styles as well when filtering by a layer workspace.
                    if (style.getWorkspace() != null
                            && !targetWs.equals(style.getWorkspace().getName())) {
                        return;
                    }
                }
                styleIds.add(style.getId());
            }
        };
        add(
                table = new GeoServerTablePanel<>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<StyleInfo> itemModel, Property<StyleInfo> property) {

                        if (property == StyleProvider.NAME) {
                            return styleLink(id, itemModel);
                        }
                        if (property == StyleProvider.WORKSPACE) {
                            return workspaceLink(id, itemModel);
                        }
                        if (property == StyleProvider.MODIFIED_TIMESTAMP) {
                            return new DateTimeLabel(id, StyleProvider.MODIFIED_TIMESTAMP.getModel(itemModel));
                        }
                        if (property == StyleProvider.CREATED_TIMESTAMP) {
                            return new DateTimeLabel(id, StyleProvider.CREATED_TIMESTAMP.getModel(itemModel));
                        }
                        if (property == StyleProvider.FORMAT) {
                            return new StyleFormatLabel(
                                    id,
                                    StyleProvider.FORMAT.getModel(itemModel),
                                    StyleProvider.FORMAT_VERSION.getModel(itemModel));
                        }
                        if (property == StyleProvider.MODIFIED_BY) {
                            return new Label(id, StyleProvider.MODIFIED_BY.getModel(itemModel));
                        }
                        return null;
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
        header.add(new BookmarkablePageLink<>("addNew", StyleNewPage.class));

        // the removal button
        header.add(
                removal = new SelectionRemovalLink("removeSelected", table, dialog) {
                    @Override
                    protected StringResourceModel canRemove(CatalogInfo object) {
                        if (isDefaultStyle(object)) {
                            return new StringResourceModel("cantRemoveDefaultStyle", StylePage.this, null);
                        }
                        return null;
                    }
                });
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component styleLink(String id, IModel<StyleInfo> model) {
        IModel<?> nameModel = StyleProvider.NAME.getModel(model);
        IModel<?> wsModel = StyleProvider.WORKSPACE.getModel(model);

        String name = (String) nameModel.getObject();
        String wsName = (String) wsModel.getObject();

        return new SimpleBookmarkableLink(
                id, StyleEditPage.class, nameModel, StyleEditPage.NAME, name, StyleEditPage.WORKSPACE, wsName);
    }

    Component workspaceLink(String id, IModel<StyleInfo> model) {
        IModel<?> wsNameModel = StyleProvider.WORKSPACE.getModel(model);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(id, WorkspaceEditPage.class, new Model<>(wsName), "name", wsName);
        } else {
            return new WebMarkupContainer(id);
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    protected static boolean isDefaultStyle(CatalogInfo catalogInfo) {
        if (catalogInfo instanceof StyleInfo s) {

            return s.getWorkspace() == null
                    && (StyleInfo.DEFAULT_POINT.equals(s.getName())
                            || StyleInfo.DEFAULT_LINE.equals(s.getName())
                            || StyleInfo.DEFAULT_POLYGON.equals(s.getName())
                            || StyleInfo.DEFAULT_RASTER.equals(s.getName())
                            || StyleInfo.DEFAULT_GENERIC.equals(s.getName()));
        } else {
            return false;
        }
    }
}
