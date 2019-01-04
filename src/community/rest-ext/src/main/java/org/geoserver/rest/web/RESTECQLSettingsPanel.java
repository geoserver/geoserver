/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.rest.ecql.RESTUploadECQLPathMapper;
import org.geoserver.web.data.settings.SettingsPluginPanel;
import org.geoserver.web.util.MetadataMapModel;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;

/**
 * Simple Panel which adds a TextField for setting the ECQL expression for the WorkSpace or Global
 * Settings.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public class RESTECQLSettingsPanel extends SettingsPluginPanel {

    private static Logger LOGGER =
            Logging.getLogger("org.geoserver.rest.web.RESTECQLSettingsPanel");

    public RESTECQLSettingsPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);

        // Selection of the IModel associated to the metadata map
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");

        // TextField associated to the root directory to map
        TextArea ecqlexp =
                new TextArea(
                        "ecqlexp",
                        new MetadataMapModel(
                                metadata, RESTUploadECQLPathMapper.EXPRESSION_KEY, String.class));
        // Addition of the validator
        ecqlexp.add(
                new IValidator<String>() {

                    @Override
                    public void validate(IValidatable<String> validatable) {
                        // Extraction of the expression for the validation
                        String expression = validatable.getValue();

                        Expression ecql = null;
                        // First check on the Syntax of the expression
                        try {
                            ecql = ECQL.toExpression(expression);
                        } catch (CQLException e) {
                            LOGGER.info("Unable to parse the following Expression");
                            error("Unable to parse the following Expression:" + e.getSyntaxError());
                        }

                        // Selection of a FilterAttributeExtractor
                        if (ecql != null) {
                            FilterAttributeExtractor filter = new FilterAttributeExtractor();
                            ecql.accept(filter, null);
                            // Extraction of the attributes
                            List<String> attributes = Arrays.asList(filter.getAttributeNames());
                            // Invalid Attributes
                            List<String> invalid = new ArrayList<String>();
                            // Check on the attributes
                            for (String attribute : attributes) {
                                if (!(attribute.equalsIgnoreCase(RESTUploadECQLPathMapper.PATH)
                                        || attribute.equalsIgnoreCase(
                                                RESTUploadECQLPathMapper.NAME))) {
                                    invalid.add(attribute);
                                }
                            }

                            // If and only if an invalid attribute is present
                            if (!invalid.isEmpty()) {

                                StringBuilder string = new StringBuilder("Invalid Attributes: ");

                                for (String attribute : invalid) {
                                    string.append(attribute);
                                    string.append(", ");
                                }

                                // Removal of the last 2 characters
                                string.setLength(string.length() - 2);

                                // If invalid attributes have been found, they are reported
                                error(invalid.toString());
                            }
                        }
                    }
                });
        add(ecqlexp);
    }
}
