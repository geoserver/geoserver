/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerSecurityService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * New page for specific class of named security service.
 *
 * <p>Most of the work is delegated to {@link SecurityNamedServicePanelInfo} and {@link
 * SecurityNamedServicePanel}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecurityNamedServiceNewPage<
                S extends GeoServerSecurityService, T extends SecurityNamedServiceConfig>
        extends SecurityNamedServicePage<T> {

    Form form;
    WebMarkupContainer panelContainer;

    public SecurityNamedServiceNewPage(Class<S> serviceClass) {
        // keys that allow us to dynamically set the page title and description based on
        // type of service class / extension point
        add(new Label("title1", createTitleModel(serviceClass).getString()));
        add(new Label("title2", createTitleModel(serviceClass).getString()));

        List<SecurityNamedServicePanelInfo> panelInfos = lookupPanelInfos(serviceClass);

        AjaxLinkGroup<SecurityNamedServicePanelInfo> serviceLinks =
                new AjaxLinkGroup<SecurityNamedServicePanelInfo>("services", panelInfos) {

                    @Override
                    protected void populateItem(ListItem<SecurityNamedServicePanelInfo> item) {
                        SecurityNamedServicePanelInfo panelInfo = item.getModelObject();
                        item.add(
                                newLink("link", item.getModel())
                                        .add(new Label("title", createShortTitleModel(panelInfo)))
                                        .setEnabled(item.getIndex() > 0));
                        item.add(new Label("description", createDescriptionModel(panelInfo)));
                    }

                    @Override
                    protected void onClick(
                            AjaxLink<SecurityNamedServicePanelInfo> link,
                            AjaxRequestTarget target) {
                        updatePanel(link.getModelObject(), target);
                    }
                };

        add(new WebMarkupContainer("servicesContainer").add(serviceLinks).setOutputMarkupId(true));

        add(form = new Form<T>("form"));

        // add a container for the actual panel, since we will dynamically update it
        form.add(panelContainer = new WebMarkupContainer("panel"));
        panelContainer.setOutputMarkupId(true);

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        handleSubmit(getForm());
                    }
                });
        form.add(
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        doReturn();
                    }
                });

        updatePanel(panelInfos.get(0), null);
    }

    void updatePanel(SecurityNamedServicePanelInfo panelInfo, AjaxRequestTarget target) {
        // create a new config object
        T config = null;
        try {
            config = (T) panelInfo.getServiceConfigClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new WicketRuntimeException(
                    "Unable to create config class: " + panelInfo.getServiceConfigClass(), e);
        }

        config.setClassName(panelInfo.getServiceClass().getCanonicalName());

        // update the form model
        form.setModel(new CompoundPropertyModel<T>(config));

        // create the new panel
        panel = createPanel("content", panelInfo, new Model(config));

        // remove the old panel if it is there
        if (panelContainer.get("content") != null) {
            panelContainer.remove("content");
        }
        panelContainer.add(panel);

        if (target != null) {
            target.add(panelContainer);
        }
    }

    List<SecurityNamedServicePanelInfo> lookupPanelInfos(Class<S> serviceClass) {

        List<SecurityNamedServicePanelInfo> panelInfos = new ArrayList();
        for (SecurityNamedServicePanelInfo pageInfo :
                GeoServerApplication.get().getBeansOfType(SecurityNamedServicePanelInfo.class)) {
            if (serviceClass.isAssignableFrom(pageInfo.getServiceClass())) {
                panelInfos.add(pageInfo);
            }
        }

        if (panelInfos.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find panel info for service class: " + serviceClass);
        }

        return panelInfos;
    }

    abstract static class AjaxLinkGroup<T> extends ListView<T> {

        public AjaxLinkGroup(String id, List<T> list) {
            super(id, list);
        }

        public AjaxLinkGroup(String id) {
            super(id);
        }

        void init() {
            setOutputMarkupId(true);
        }

        protected AjaxLink<T> newLink(String id, IModel<T> model) {
            return (AjaxLink<T>)
                    new AjaxLink<T>(id, model) {
                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            // set all links enabled
                            AjaxLinkGroup.this.visitChildren(
                                    AjaxLink.class,
                                    (component, visit) -> {
                                        component.setEnabled(true);
                                        target.add(component);
                                        visit.dontGoDeeper();
                                    });
                            // set this link disabled
                            setEnabled(false);

                            // update
                            // target.add(AjaxLinkGroup.this.getParent());
                            target.add(this);

                            AjaxLinkGroup.this.onClick(this, target);
                        }
                    }.setOutputMarkupId(true);
        }

        protected abstract void onClick(AjaxLink<T> link, AjaxRequestTarget target);
    }
}
