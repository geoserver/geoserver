/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Panel for the basic dbms parameters
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class BasicDbParamPanel extends Panel {

    String host;
    int port;
    String username;
    String password;
    String database;
    String schema;

    ConnectionPoolParamPanel connPoolPanel;
    WebMarkupContainer connPoolPanelContainer;
    Component connPoolLink;

    public BasicDbParamPanel(String id, String host, int port, boolean databaseRequired) {
        this(id, host, port, null, null, null, databaseRequired);
    }

    public BasicDbParamPanel(
            String id,
            String host,
            int port,
            String database,
            String schema,
            String username,
            boolean databaseRequired) {
        super(id);

        this.host = host;
        this.port = port;
        this.database = database;
        this.schema = schema;
        this.username = username;

        add(new TextField("host", new PropertyModel(this, "host")).setRequired(true));
        add(new TextField("port", new PropertyModel(this, "port")).setRequired(true));
        add(new TextField("username", new PropertyModel(this, "username")).setRequired(true));
        add(
                new PasswordTextField("password", new PropertyModel(this, "password"))
                        .setResetPassword(false)
                        .setRequired(false));
        add(
                new TextField("database", new PropertyModel(this, "database"))
                        .setRequired(databaseRequired));
        add(new TextField("schema", new PropertyModel(this, "schema")));

        connPoolLink = toggleConnectionPoolLink();
        add(connPoolLink);

        connPoolPanelContainer = new WebMarkupContainer("connPoolPanelContainer");
        connPoolPanelContainer.setOutputMarkupId(true);
        connPoolPanel = new ConnectionPoolParamPanel("connPoolPanel", true);
        connPoolPanel.setVisible(false);
        connPoolPanelContainer.add(connPoolPanel);
        add(connPoolPanelContainer);
    }

    /** Toggles the connection pool param panel */
    Component toggleConnectionPoolLink() {
        AjaxLink connPoolLink =
                new AjaxLink("connectionPoolLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        connPoolPanel.setVisible(!connPoolPanel.isVisible());
                        target.add(connPoolPanelContainer);
                        target.add(this);
                    }
                };
        connPoolLink.add(
                new AttributeModifier(
                        "class",
                        new AbstractReadOnlyModel() {

                            @Override
                            public Object getObject() {
                                return connPoolPanel.isVisible() ? "expanded" : "collapsed";
                            }
                        }));
        connPoolLink.setOutputMarkupId(true);
        return connPoolLink;
    }
}
