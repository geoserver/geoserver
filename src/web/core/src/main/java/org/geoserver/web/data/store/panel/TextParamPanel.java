/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
@SuppressWarnings("serial")
public class TextParamPanel extends Panel implements ParamPanel {

    private TextField textField;
    
    /**
     * 
     * @param id
     * @param paramsMap
     * @param paramName
     * @param paramLabelModel
     * @param required
     * @param validators
     *            any extra validator that should be added to the input field, or {@code null}
     */
    public TextParamPanel(final String id, final IModel paramValue, final IModel paramLabelModel,
            final boolean required, IValidator... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue);

        // the label
        String requiredMark = required ? " *" : ""; 
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the text field, with a decorator for validations
        textField = new TextField("paramValue", paramValue);
        textField.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        textField.setLabel(paramLabelModel);

        if (validators != null) {
            for (IValidator validator : validators) {
                textField.add(validator);
            }
        }
        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(textField);
        add(feedback);
    }
    
    /**
     * The text field stored inside the panel. 
     * @return
     */
    public FormComponent getFormComponent() {
        return textField;
    }
}
