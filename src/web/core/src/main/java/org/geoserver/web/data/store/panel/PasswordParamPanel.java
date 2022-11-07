/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.data.store.PasswordTextFieldWriteOnlyModel;

/**
 * A label with a password field
 *
 * @author Gabriel Roldan
 */
public class PasswordParamPanel extends Panel implements ParamPanel<String> {

    private static final long serialVersionUID = -7801141820174575611L;

    private final PasswordTextField passwordField;

    public PasswordParamPanel(
            final String id,
            final IModel<String> model,
            final IModel<String> paramLabelModel,
            final boolean required) {
        super(id, model);
        String requiredMark = required ? " *" : "";
        add(new Label("paramName", paramLabelModel.getObject() + requiredMark));

        passwordField = new PasswordTextFieldWriteOnlyModel("paramValue", model);
        passwordField.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        passwordField.setLabel(paramLabelModel);

        FormComponentFeedbackBorder requiredFieldFeedback =
                new FormComponentFeedbackBorder("border");

        requiredFieldFeedback.add(passwordField);

        add(requiredFieldFeedback);
    }

    @Override
    public PasswordTextField getFormComponent() {
        return passwordField;
    }
}
