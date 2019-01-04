/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/** */
class PostgresConfigFormPanel extends FormComponentPanel<PostgresConfigBean> {

    private static final long serialVersionUID = 1L;

    private final TextParamPanel hostPanel;
    private final TextParamPanel portPanel;
    private final TextParamPanel dbPanel;
    private final TextParamPanel schemaPanel;
    private final TextParamPanel usernamePanel;
    private final PasswordParamPanel passwordPanel;

    public PostgresConfigFormPanel(String id, IModel<PostgresConfigBean> model) {
        super(id, model);

        setOutputMarkupId(true);
        hostPanel =
                new TextParamPanel(
                        "hostPanel",
                        new PropertyModel<>(model, "host"),
                        new ResourceModel("PostgresConfigFormPanel.host", "Host Name"),
                        true);
        hostPanel.getFormComponent().setType(String.class);
        add(hostPanel);
        portPanel =
                new TextParamPanel(
                        "portPanel",
                        new PropertyModel<>(model, "port"),
                        new ResourceModel("PostgresConfigFormPanel.port", "Port"),
                        false);
        // set the type for the port, and validators
        portPanel
                .getFormComponent()
                .setType(Integer.TYPE)
                .add(
                        (IValidator) RangeValidator.minimum(1025),
                        (IValidator) RangeValidator.maximum(65535));
        add(portPanel);
        dbPanel =
                new TextParamPanel(
                        "dbPanel",
                        new PropertyModel<>(model, "database"),
                        new ResourceModel("PostgresConfigFormPanel.database", "Database Name"),
                        true);
        dbPanel.getFormComponent().setType(String.class);
        add(dbPanel);
        schemaPanel =
                new TextParamPanel(
                        "schemaPanel",
                        new PropertyModel<>(model, "schema"),
                        new ResourceModel("PostgresConfigFormPanel.schema", "Schema Name"),
                        false);
        schemaPanel.getFormComponent().setType(String.class);
        add(schemaPanel);
        usernamePanel =
                new TextParamPanel(
                        "usernamePanel",
                        new PropertyModel<>(model, "username"),
                        new ResourceModel("PostgresConfigFormPanel.username", "Username"),
                        true);
        usernamePanel.getFormComponent().setType(String.class);
        add(usernamePanel);
        passwordPanel =
                new PasswordParamPanel(
                        "passwordPanel",
                        new PropertyModel<>(model, "password"),
                        new ResourceModel("PostgresConfigFormPanel.password", "Password"),
                        true);
        passwordPanel.getFormComponent().setType(String.class);
        add(passwordPanel);
    }

    @Override
    public void convertInput() {
        PostgresConfigBean bean = new PostgresConfigBean();
        // populate the bean
        String host = hostPanel.getFormComponent().getConvertedInput().toString().trim();
        Integer port = Integer.class.cast(portPanel.getFormComponent().getConvertedInput());
        String db = dbPanel.getFormComponent().getConvertedInput().toString().trim();
        Object schema = schemaPanel.getFormComponent().getConvertedInput();
        String username = usernamePanel.getFormComponent().getConvertedInput().toString().trim();
        String password = passwordPanel.getFormComponent().getConvertedInput();

        bean.setHost(host);
        bean.setPort(port);
        bean.setDatabase(db);
        bean.setUsername(username);
        bean.setPassword(password);
        if (schema == null || schema.toString().trim().isEmpty()) {
            bean.setSchema(null);
        } else {
            bean.setSchema(schema.toString().trim());
        }

        setConvertedInput(bean);
    }
}
