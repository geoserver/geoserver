/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.jdbc.JDBCConnectAuthProvider;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link JDBCConnectAuthProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCAuthProviderPanel
        extends AuthenticationProviderPanel<JDBCConnectAuthProviderConfig> {

    private static final long serialVersionUID = 1L;
    FeedbackPanel feedbackPanel;
    String username, password;

    public JDBCAuthProviderPanel(String id, IModel<JDBCConnectAuthProviderConfig> model) {
        super(id, model);

        add(new UserGroupServiceChoice("userGroupServiceName"));
        add(new JDBCDriverChoice("driverClassName"));
        add(new TextField<String>("connectURL"));

        TextField<String> userNameField = new TextField<String>("username");
        userNameField.setModel(new PropertyModel<String>(this, "username"));
        userNameField.setRequired(false);
        add(userNameField);

        PasswordTextField pwdField = new PasswordTextField("password");
        pwdField.setModel(new PropertyModel<String>(this, "password"));
        pwdField.setRequired(false);
        pwdField.setResetPassword(true);
        add(pwdField);

        add(
                new AjaxSubmitLink("cxTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            test();
                            info(
                                    new StringResourceModel(
                                                    "connectionSuccessful",
                                                    JDBCAuthProviderPanel.this,
                                                    null)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Connection error", e);
                        } finally {
                            target.add(feedbackPanel);
                        }
                    }
                }.setDefaultFormProcessing(false));

        add(feedbackPanel = new FeedbackPanel("feedback"));
        feedbackPanel.setOutputMarkupId(true);
    }

    public void test() throws Exception {
        // since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent) get("driverClassName")).processInput();
        ((FormComponent) get("connectURL")).processInput();
        ((FormComponent) get("username")).processInput();
        ((FormComponent) get("password")).processInput();

        // do the test
        Class.forName(get("driverClassName").getDefaultModelObjectAsString());
        try (Connection cx =
                DriverManager.getConnection(
                        get("connectURL").getDefaultModelObjectAsString(),
                        get("username").getDefaultModelObjectAsString(),
                        get("password").getDefaultModelObjectAsString())) {}
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
