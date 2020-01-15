/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CatalogInfo;
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

/** Page listing all the styles, allows to edit, add, remove styles */
@SuppressWarnings("serial")
public class StylePage extends GeoServerSecuredPage {

    GeoServerTablePanel<StyleInfo> table;

    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public StylePage() {
        StyleProvider provider = new StyleProvider();
        add(
                table =
                        new GeoServerTablePanel<StyleInfo>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<StyleInfo> itemModel,
                                    Property<StyleInfo> property) {

                                if (property == StyleProvider.NAME) {
                                    return styleLink(id, itemModel);
                                }
                                if (property == StyleProvider.WORKSPACE) {
                                    return workspaceLink(id, itemModel);
                                }
                                if (property == StyleProvider.MODIFIED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            StyleProvider.MODIFIED_TIMESTAMP.getModel(itemModel));
                                }
                                if (property == StyleProvider.CREATED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            StyleProvider.CREATED_TIMESTAMP.getModel(itemModel));
                                }
                                return null;
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
        header.add(new BookmarkablePageLink<StyleNewPage>("addNew", StyleNewPage.class));

        // the removal button
        header.add(
                removal =
                        new SelectionRemovalLink("removeSelected", table, dialog) {
                            @Override
                            protected StringResourceModel canRemove(CatalogInfo object) {
                                if (isDefaultStyle(object)) {
                                    return new StringResourceModel(
                                            "cantRemoveDefaultStyle", StylePage.this, null);
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
                id,
                StyleEditPage.class,
                nameModel,
                StyleEditPage.NAME,
                name,
                StyleEditPage.WORKSPACE,
                wsName);
    }

    Component workspaceLink(String id, IModel<StyleInfo> model) {
        IModel<?> wsNameModel = StyleProvider.WORKSPACE.getModel(model);
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

    protected static boolean isDefaultStyle(CatalogInfo catalogInfo) {
        if (catalogInfo instanceof StyleInfo) {
            StyleInfo s = (StyleInfo) catalogInfo;

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
