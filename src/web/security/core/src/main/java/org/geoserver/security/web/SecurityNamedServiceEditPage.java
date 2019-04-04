/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * Edit page for specific class of named security service.
 *
 * <p>Most of the work is delegated to {@link SecurityNamedServicePanelInfo} and {@link
 * SecurityNamedServicePanel}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecurityNamedServiceEditPage<T extends SecurityNamedServiceConfig>
        extends SecurityNamedServicePage<T> {

    SecurityNamedServicePanelInfo panelInfo;

    public SecurityNamedServiceEditPage(IModel<T> config) {
        // create the specific panel
        panelInfo = lookupPanelInfo(config);
        panel = createPanel("dummy", panelInfo, config);

        // set page title and description from the panel title and description
        add(new Label("name", config.getObject().getName()));
        add(new Label("title", createTitleModel(panelInfo)));
        add(new Label("description", createDescriptionModel(panelInfo)));

        if (SecurityNamedServiceTabbedPanel.class.isAssignableFrom(panelInfo.getComponentClass())) {
            // this panel supports tabs, layout in tabbed mode
            add(new TabbedLayoutPanel("panel", config));
        } else {
            // else layout in basic mode
            add(new BasicLayoutPanel("panel", config));
        }
    }

    class ContentPanel extends Panel {

        public ContentPanel(String id, IModel<T> config) {
            super(id, new Model());

            Form form = new Form("form", new CompoundPropertyModel<T>(config));
            add(form);
            form.add(panel = createPanel("panel", panelInfo, config));

            form.add(
                    new SubmitLink("save", form) {
                        @Override
                        public void onSubmit() {
                            handleSubmit(getForm());
                        }
                    }.setVisible(getSecurityManager().checkAuthenticationForAdminRole()));
            form.add(
                    new Link("cancel") {
                        @Override
                        public void onClick() {
                            doReturn();
                        }
                    });
        }
    }

    /*
     * throws the service panel into a basic form panel
     */
    class BasicLayoutPanel extends Panel {

        public BasicLayoutPanel(String id, IModel<T> config) {
            super(id, new Model());

            add(new ContentPanel("panel", config));
        }
    }

    /*
     * throws the service panel onto the first tab, and then delegates it to create additional
     * tabs.
     */
    class TabbedLayoutPanel extends Panel {

        public TabbedLayoutPanel(String id, final IModel<T> config) {
            super(id, new Model());

            List<ITab> tabs = new ArrayList<ITab>();

            // add the primary panel to the first tab
            tabs.add(
                    new AbstractTab(new StringResourceModel("settings", (IModel<?>) null)) {
                        @Override
                        public Panel getPanel(String panelId) {
                            return new ContentPanel(panelId, config);
                        }
                    });

            // add tabs contributed by the server
            tabs.addAll(((SecurityNamedServiceTabbedPanel) panel).createTabs(config));

            // add the error tab that displays any exceptions currently associated with the service
            try {
                panel.doLoad(config.getObject());
            } catch (final Exception e) {
                // add the error tab
                tabs.add(
                        new AbstractTab(new StringResourceModel("error", (IModel<?>) null)) {
                            @Override
                            public Panel getPanel(String panelId) {
                                return new ErrorPanel(panelId, e);
                            }
                        });
            }
            add(new TabbedPanel("panel", tabs));
        }
    }

    class ErrorPanel extends Panel {

        public ErrorPanel(String id, final Exception error) {
            super(id, new Model());

            add(new Label("message", new PropertyModel(error, "message")));
            add(new TextArea("stackTrace", new Model(handleStackTrace(error))));
            add(
                    new AjaxLink("copy") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            copyToClipBoard(handleStackTrace(error));
                        }
                    });
        }

        public String getLabelKey() {
            return "error";
        };

        String handleStackTrace(Exception error) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            error.printStackTrace(writer);
            writer.flush();

            return new String(out.toByteArray());
        }
    }

    void copyToClipBoard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    SecurityNamedServicePanelInfo lookupPanelInfo(IModel<T> model) {
        T config = model.getObject();
        Class serviceClass = null;
        try {
            serviceClass = Class.forName(config.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<SecurityNamedServicePanelInfo> panelInfos = new ArrayList();
        for (SecurityNamedServicePanelInfo pageInfo :
                GeoServerApplication.get().getBeansOfType(SecurityNamedServicePanelInfo.class)) {
            if (pageInfo.getServiceClass().isAssignableFrom(serviceClass)) {
                panelInfos.add(pageInfo);
            }
        }

        if (panelInfos.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find panel info for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }
        if (panelInfos.size() > 1) {
            // filter by strict equals
            List<SecurityNamedServicePanelInfo> l = new ArrayList(panelInfos);
            for (Iterator<SecurityNamedServicePanelInfo> it = l.iterator(); it.hasNext(); ) {
                final SecurityNamedServicePanelInfo targetPanelInfo = it.next();
                if (!targetPanelInfo.getServiceClass().equals(serviceClass)) {
                    it.remove();
                } else if (!targetPanelInfo.getServiceConfigClass().equals(config.getClass())) {
                    it.remove();
                }
            }
            if (l.size() == 1) {
                // filter down to one match
                return l.get(0);
            }
            throw new RuntimeException(
                    "Found multiple panel infos for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }

        // found just one
        return panelInfos.get(0);
    }
}
