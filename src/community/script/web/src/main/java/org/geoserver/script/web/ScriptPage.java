/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

@SuppressWarnings("serial")
public class ScriptPage extends GeoServerSecuredPage {
    ScriptProvider provider = new ScriptProvider();

    GeoServerTablePanel<Script> table;

    GeoServerDialog dialog;

    ScriptSelectionRemovalLink removal;

    public ScriptPage() {
        // the middle table
        add(
                table =
                        new GeoServerTablePanel<Script>("table", provider, true) {
                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<Script> itemModel,
                                    Property<Script> property) {
                                if (property == ScriptProvider.NAME) {
                                    return scriptLink(id, itemModel);
                                } else if (property == ScriptProvider.TYPE) {
                                    return new Label(id, ScriptProvider.TYPE.getModel(itemModel));
                                } else if (property == ScriptProvider.EXTENSION) {
                                    return new Label(
                                            id, ScriptProvider.EXTENSION.getModel(itemModel));
                                } else if (property == ScriptProvider.FILE) {
                                    return new Label(id, ScriptProvider.FILE.getModel(itemModel));
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
        header.add(new BookmarkablePageLink<Object>("addNew", ScriptNewPage.class));

        // the removal button
        header.add(removal = new ScriptSelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component scriptLink(String id, final IModel<Script> itemModel) {
        IModel<?> nameModel = ScriptProvider.NAME.getModel(itemModel);
        IModel<?> fileModel = ScriptProvider.FILE.getModel(itemModel);
        return new SimpleBookmarkableLink(
                id,
                ScriptEditPage.class,
                nameModel,
                // @TODO type and extension instead of file
                "file",
                fileModel.getObject().toString());
    }
}
