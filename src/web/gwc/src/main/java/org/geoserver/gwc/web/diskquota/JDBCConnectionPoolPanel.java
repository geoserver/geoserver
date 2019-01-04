/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;

public class JDBCConnectionPoolPanel extends Panel {

    private static final long serialVersionUID = -1579697287836672528L;

    public JDBCConnectionPoolPanel(String id, IModel<ConnectionPoolConfiguration> model) {
        super(id, model);

        TextField<String> driver =
                new TextField<String>("jdbcDriver", new PropertyModel<String>(model, "driver"));
        driver.setRequired(true);
        AutoCompleteSettings as = new AutoCompleteSettings();
        as.setPreselect(true).setShowListOnEmptyInput(true).setShowCompleteListOnFocusGain(true);
        driver.add(
                new ContainsAutoCompleteBehavior(
                        "org.postgresql.Driver",
                        "oracle.jdbc.driver.OracleDriver",
                        "org.h2.Driver"));
        add(driver);

        TextField<String> url =
                new TextField<String>("jdbcUrl", new PropertyModel<String>(model, "url"));
        url.setRequired(true);
        url.add(
                new ContainsAutoCompleteBehavior(
                        "jdbc:h2://{server}:{9092}/{db-name}",
                        "jdbc:postgresql:[{//host}[:{5432}/]]{database}",
                        "jdbc:oracle:thin:@{server}[:{1521}]:{database_name}"));

        add(url);

        TextField<String> user =
                new TextField<String>("jdbcUser", new PropertyModel<String>(model, "username"));
        add(user);

        PasswordTextField password =
                new PasswordTextField("jdbcPassword", new PropertyModel<String>(model, "password"));
        password.setResetPassword(false);
        add(password);

        TextField<Integer> minConnections =
                new TextField<Integer>(
                        "jdbcMinConnections", new PropertyModel<Integer>(model, "minConnections"));
        minConnections.setRequired(true);
        minConnections.add(RangeValidator.minimum(0));
        add(minConnections);

        TextField<Integer> maxConnections =
                new TextField<Integer>(
                        "jdbcMaxConnections", new PropertyModel<Integer>(model, "maxConnections"));
        maxConnections.setRequired(true);
        maxConnections.add(RangeValidator.minimum(1));
        add(maxConnections);

        TextField<Integer> connectionTimeout =
                new TextField<Integer>(
                        "jdbcConnectionTimeout",
                        new PropertyModel<Integer>(model, "connectionTimeout"));
        connectionTimeout.setRequired(true);
        connectionTimeout.add(RangeValidator.minimum(1));
        add(connectionTimeout);

        TextField<String> validationQuery =
                new TextField<String>(
                        "jdbcValidationQuery", new PropertyModel<String>(model, "validationQuery"));
        add(validationQuery);

        TextField<Integer> maxOpenPreparedStatements =
                new TextField<Integer>(
                        "jdbcMaxOpenPreparedStatements",
                        new PropertyModel<Integer>(model, "maxOpenPreparedStatements"));
        maxOpenPreparedStatements.setRequired(true);
        maxOpenPreparedStatements.add(RangeValidator.minimum(0));
        add(maxOpenPreparedStatements);
    }

    /**
     * Matches any of the specified choices provided they contain the text typed by the user (in a
     * case insensitive way)
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class ContainsAutoCompleteBehavior extends AutoCompleteBehavior<String> {
        private static final long serialVersionUID = 993566054116148859L;
        private List<String> choices;

        public ContainsAutoCompleteBehavior(List<String> choices) {
            super(
                    new AbstractAutoCompleteTextRenderer<String>() {
                        private static final long serialVersionUID = 3192368880726583011L;

                        @Override
                        protected String getTextValue(String object) {
                            return object;
                        }
                    });
            settings.setPreselect(true)
                    .setShowListOnEmptyInput(true)
                    .setShowCompleteListOnFocusGain(true);
            this.choices = new ArrayList<String>(choices);
        }

        public ContainsAutoCompleteBehavior(String... choices) {
            this(Arrays.asList(choices));
        }

        @Override
        protected Iterator<String> getChoices(String input) {
            String ucInput = input.toUpperCase();
            List<String> result = new ArrayList<String>();
            for (String choice : choices) {
                if (choice.toUpperCase().contains(ucInput)) {
                    result.add(choice);
                }
            }

            return result.iterator();
        }
    }
}
