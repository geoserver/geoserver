/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.data.store.PasswordTextFieldWriteOnlyModel;
import org.geoserver.web.wicket.ContainsAutoCompleteBehavior;
import org.geoserver.web.wicket.JDBCUrlTemplates;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;

public class JDBCConnectionPoolPanel extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(JDBCConnectionPoolPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = -1579697287836672528L;

    public JDBCConnectionPoolPanel(String id, IModel<ConnectionPoolConfiguration> model) {
        super(id, model);

        TextField<String> driver = new TextField<>("jdbcDriver", new PropertyModel<>(model, "driver"));
        driver.setRequired(true);
        driver.add(new ContainsAutoCompleteBehavior(
                "org.postgresql.Driver", "oracle.jdbc.driver.OracleDriver", "org.hsqldb.jdbcDriver"));
        add(driver);

        TextField<String> url = new TextField<>("jdbcUrl", new PropertyModel<>(model, "url"));
        url.setRequired(true);
        url.add(new ContainsAutoCompleteBehavior(JDBCUrlTemplates.forRegisteredDrivers()));

        add(url);

        TextField<String> user = new TextField<>("jdbcUser", new PropertyModel<>(model, "username"));
        add(user);

        PasswordTextField password =
                new PasswordTextFieldWriteOnlyModel("jdbcPassword", new PropertyModel<>(model, "password"));
        password.setResetPassword(false);
        add(password);

        TextField<Integer> minConnections =
                new TextField<>("jdbcMinConnections", new PropertyModel<>(model, "minConnections"));
        minConnections.setRequired(true);
        minConnections.add(RangeValidator.minimum(0));
        add(minConnections);

        TextField<Integer> maxConnections =
                new TextField<>("jdbcMaxConnections", new PropertyModel<>(model, "maxConnections"));
        maxConnections.setRequired(true);
        maxConnections.add(RangeValidator.minimum(1));
        add(maxConnections);

        TextField<Integer> connectionTimeout =
                new TextField<>("jdbcConnectionTimeout", new PropertyModel<>(model, "connectionTimeout"));
        connectionTimeout.setRequired(true);
        connectionTimeout.add(RangeValidator.minimum(1));
        add(connectionTimeout);

        TextField<String> validationQuery =
                new TextField<>("jdbcValidationQuery", new PropertyModel<>(model, "validationQuery"));
        add(validationQuery);

        TextField<Integer> maxOpenPreparedStatements = new TextField<>(
                "jdbcMaxOpenPreparedStatements", new PropertyModel<>(model, "maxOpenPreparedStatements"));
        maxOpenPreparedStatements.setRequired(true);
        maxOpenPreparedStatements.add(RangeValidator.minimum(0));
        add(maxOpenPreparedStatements);
    }
}
