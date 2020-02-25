/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geotools.util.logging.Logging;

/**
 * Reusable form component for jdbc connect configurations
 *
 * @author Chrisitian Mueller
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCConnectionPanel<T extends JDBCSecurityServiceConfig>
        extends FormComponentPanel<T> {

    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    FeedbackPanel feedbackPanel;

    public JDBCConnectionPanel(String id, IModel<T> model) {
        super(id, new Model());

        add(
                new AjaxCheckBox("jndi") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        WebMarkupContainer c =
                                (WebMarkupContainer)
                                        JDBCConnectionPanel.this.get("cxPanelContainer");

                        // reset any values that were set
                        ((ConnectionPanel) c.get("cxPanel")).resetModel();

                        // replace old panel
                        c.addOrReplace(createCxPanel("cxPanel", getModelObject()));

                        target.add(c);
                    }
                });

        boolean useJNDI = model.getObject().isJndi();
        add(
                new WebMarkupContainer("cxPanelContainer")
                        .add(createCxPanel("cxPanel", useJNDI))
                        .setOutputMarkupId(true));

        add(
                new AjaxSubmitLink("cxTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            ((ConnectionPanel)
                                            JDBCConnectionPanel.this.get(
                                                    "cxPanelContainer:cxPanel"))
                                    .test();
                            info(
                                    new StringResourceModel(
                                                    "connectionSuccessful",
                                                    JDBCConnectionPanel.this,
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

    ConnectionPanel createCxPanel(String id, boolean useJNDI) {
        return useJNDI ? new JNDIConnectionPanel(id) : new BasicConnectionPanel(id);
    }

    abstract class ConnectionPanel extends FormComponentPanel {

        public ConnectionPanel(String id) {
            super(id, new Model());
        }

        public abstract void resetModel();

        public abstract void test() throws Exception;
    }

    class BasicConnectionPanel extends ConnectionPanel {

        public BasicConnectionPanel(String id) {
            super(id);

            add(new JDBCDriverChoice("driverClassName").setRequired(true));
            add(new TextField("connectURL").setRequired(true));
            add(new TextField("userName").setRequired(true));

            PasswordTextField pwdField = new PasswordTextField("password");
            pwdField.setRequired(false);

            // avoid reseting the password which results in an
            // empty password on saving a modified configuration
            pwdField.setResetPassword(false);

            add(pwdField);
        }

        @Override
        public void resetModel() {
            // get("userGroupServiceName").setDefaultModelObject(null);
        }

        @Override
        public void test() throws Exception {
            // since this wasn't a regular form submission, we need to manually update component
            // models
            ((FormComponent) get("driverClassName")).processInput();
            ((FormComponent) get("connectURL")).processInput();
            ((FormComponent) get("userName")).processInput();
            ((FormComponent) get("password")).processInput();

            // do the test
            Class.forName(get("driverClassName").getDefaultModelObjectAsString());
            try (Connection cx =
                    DriverManager.getConnection(
                            get("connectURL").getDefaultModelObjectAsString(),
                            get("userName").getDefaultModelObjectAsString(),
                            get("password").getDefaultModelObjectAsString())) {}
        }
    }

    class JNDIConnectionPanel extends ConnectionPanel {

        public JNDIConnectionPanel(String id) {
            super(id);

            add(new TextField("jndiName").setRequired(true));
        }

        @Override
        public void resetModel() {
            // get("groupSearchBase").setDefaultModelObject(null);
            // get("groupSearchFilter").setDefaultModelObject(null);
        }

        @Override
        public void test() throws Exception {
            // since this wasn't a regular form submission, we need to manually update component
            // models
            ((FormComponent) get("jndiName")).processInput();

            Context initialContext = new InitialContext();
            try {
                DataSource datasource =
                        (DataSource)
                                initialContext.lookup(
                                        get("jndiName").getDefaultModelObjectAsString());
                try (Connection con = datasource.getConnection()) {}
            } finally {
                initialContext.close();
            }
        }
    }
}
