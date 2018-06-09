/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Panel that lists out all secuirty services, providing a toggle to collapse/expand each one
 * showing/hiding its contents.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServicesTogglePanel<T extends SecurityNamedServiceConfig>
        extends Panel {

    public SecurityNamedServicesTogglePanel(String id, IModel<List<T>> model) {
        super(id);

        Form form = new Form("form");
        add(form);

        form.add(
                new ListView<T>("services", model) {
                    @Override
                    protected void populateItem(final ListItem<T> item) {
                        IModel<T> model = item.getModel();
                        AjaxLink toggle =
                                new AjaxLink<T>("toggle", model) {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        if (item.get("panel") instanceof ContentPanel) {
                                            // toggle off
                                            item.addOrReplace(new WebMarkupContainer("panel"));
                                            item.get("toggle")
                                                    .add(
                                                            new AttributeModifier(
                                                                    "class",
                                                                    new Model("collapsed")));
                                        } else {
                                            // toggle on
                                            item.addOrReplace(
                                                    createPanel("panel", item.getModel()));
                                            item.get("toggle")
                                                    .add(
                                                            new AttributeModifier(
                                                                    "class",
                                                                    new Model("expanded")));
                                        }
                                        target.add(item);
                                    }
                                };
                        toggle.add(new Label("name", new PropertyModel(model, "name")));

                        boolean first = item.getIndex() == 0;
                        toggle.add(
                                new AttributeAppender(
                                        "class", new Model(first ? "expanded" : "collapsed"), " "));
                        item.add(toggle);

                        item.add(
                                first
                                        ? createPanel("panel", model)
                                        : new WebMarkupContainer("panel"));
                        item.setOutputMarkupId(true);
                    }
                });
    }

    protected abstract ContentPanel createPanel(String id, IModel<T> config);

    protected static class ContentPanel<T> extends Panel {

        public ContentPanel(String id, final IModel<T> model) {
            super(id);

            add(
                    new Link("edit") {
                        @Override
                        public void onClick() {
                            SecurityNamedServiceEditPage editPage =
                                    new SecurityNamedServiceEditPage(model);
                            editPage.setReturnPage(getPage());
                            setResponsePage(editPage);
                        }
                    });
        }
    }
}
