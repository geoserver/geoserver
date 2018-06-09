/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.util.Locale;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.ColorPickerField;

/**
 * A label with a text field. Can receive custom validators for the text field.
 *
 * @author Gabriel Roldan
 */
public class ColorPickerPanel extends Panel {

    private static final long serialVersionUID = 6661147317307298452L;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    public ColorPickerPanel(
            final String id,
            final IModel paramVale,
            final IModel paramLabelModel,
            final boolean required,
            IValidator... validators) {
        // make the value of the text field the model of this panel, for easy
        // value retriaval
        super(id, paramVale);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the color picker. Notice that we need to convert between RRGGBB and
        // #RRGGBB,
        ColorPickerField textField =
                new ColorPickerField("paramValue", paramVale) {
                    private static final long serialVersionUID = 4185457152965032989L;

                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (type.isAssignableFrom(String.class)) {
                            return (IConverter<C>)
                                    new IConverter<String>() {
                                        private static final long serialVersionUID =
                                                4343895199315509104L;

                                        public String convertToString(String input, Locale locale) {
                                            if (input.startsWith("#")) {
                                                return input.substring(1);
                                            } else {
                                                return input;
                                            }
                                        }

                                        public String convertToObject(String value, Locale locale) {
                                            if (value.equals("")) return value;
                                            return "#" + value;
                                        }
                                    };
                        }
                        return super.getConverter(type);
                    }
                };
        textField.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error
        // would look like
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
}
