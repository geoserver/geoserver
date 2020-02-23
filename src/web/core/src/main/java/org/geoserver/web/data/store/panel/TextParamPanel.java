/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;

/**
 * A label with a text field. Can receive custom validators for the text field.
 *
 * @author Gabriel Roldan
 */
public class TextParamPanel<T> extends Panel implements ParamPanel {

    private static final long serialVersionUID = 5498443514886175158L;

    private TextField<T> textField;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    @SafeVarargs
    public TextParamPanel(
            final String id,
            final IModel<T> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required,
            IValidator<T>... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the text field, with a decorator for validations
        textField = new TextField<T>("paramValue", paramValue);
        textField.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        textField.setLabel(paramLabelModel);

        if (validators != null) {
            for (IValidator<T> validator : validators) {
                textField.add(validator);
            }
        }
        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(textField);
        add(feedback);
    }

    /** The text field stored inside the panel. */
    public FormComponent<T> getFormComponent() {
        return textField;
    }
}
