/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.geogig.geoserver.config.PostgresConfigBean;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.MaximumValidator;
import org.apache.wicket.validation.validator.MinimumValidator;

/**
 *
 */
class PostgresConfigFormPanel extends FormComponentPanel<PostgresConfigBean> {

    private static final long serialVersionUID = 1L;

    private final TextField<String> hostField;
    private final TextField<Integer> portField;
    private final TextField<String> dbField;
    private final TextField<String> schemaField;
    private final TextField<String> usernameField;
    private final PasswordTextField passwordField;

    public PostgresConfigFormPanel(String id, IModel<PostgresConfigBean> model) {
        super(id, model);

        setOutputMarkupId(true);
        add(hostField = new TextField<>("pgHost", new PropertyModel<String>(model, "host"),
                String.class));
        hostField.setRequired(true);
        add(portField = new TextField<>("pgPort", new PropertyModel<Integer>(model, "port"),
                Integer.TYPE));
        portField.add(new MinimumValidator<>(1025), new MaximumValidator<>(65535));
        add(dbField = new TextField<>("pgDatabase", new PropertyModel<String>(model, "database"),
                String.class));
        dbField.setRequired(true);
        add(schemaField = new TextField<>("pgSchema", new PropertyModel<String>(model, "schema"),
                String.class));
        add(usernameField = new TextField<>("pgUsername", new PropertyModel<String>(model,
                "username"), String.class));
        usernameField.setRequired(true);
        add(passwordField = new PasswordTextField("pgPassword", new PropertyModel<String>(model,
                "password")).setResetPassword(false));
        passwordField.setType(String.class);
        passwordField.setRequired(true);
    }

    @Override
    protected void convertInput() {
        PostgresConfigBean bean = getModelObject();
        if (bean == null) {
            bean = new PostgresConfigBean();
        }
        // populate the bean
        String host = hostField.getConvertedInput().trim();
        Integer port = portField.getConvertedInput();
        String db = dbField.getConvertedInput().trim();
        String schema = schemaField.getConvertedInput();
        String username = usernameField.getConvertedInput().trim();
        String password = passwordField.getConvertedInput().trim();

        bean.setHost(host);
        bean.setPort(port);
        bean.setDatabase(db);
        bean.setUsername(username);
        bean.setPassword(password);
        if (schema == null || schema.trim().isEmpty()) {
            bean.setSchema(null);
        } else {
            bean.setSchema(schema);

        }

        setConvertedInput(bean);
    }
}
