/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.data.store.panel.ParamPanel;

/**
 * A reusable panel that provides a labeled numeric input field with validation and feedback.
 *
 * <p>This component combines a label and a numeric text field, providing a standardized way to collect numeric
 * parameters in the GeoServer web UI. It extends Apache Wicket's {@link Panel} class and implements the
 * {@link ParamPanel} interface for consistent integration with GeoServer's store configuration panels.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Generic type parameter for different number types (Integer, Long, Double, etc.)
 *   <li>Support for minimum/maximum value constraints
 *   <li>Customizable step value for incrementing/decrementing
 *   <li>Validation with feedback messages
 *   <li>Special handling for integer types to prevent decimal input
 *   <li>Proper conversion between string input and number type
 * </ul>
 *
 * @param <N> The specific Number subtype, which must also implement Comparable
 */
@SuppressWarnings("serial")
public class NumberParamPanel<N extends Number & Comparable<N>> extends Panel implements ParamPanel<N> {

    private NumberTextField<N> numberField;

    /**
     * Creates a new panel with a numeric input field.
     *
     * @param id The component ID
     * @param paramValue The model containing the numeric value
     * @param paramLabelModel The model for the field label text
     * @param type The class of the number type (Integer.class, Double.class, etc.)
     * @param validators Optional set of validators to apply to the field
     */
    @SafeVarargs
    public NumberParamPanel(
            String id,
            IModel<N> paramValue,
            IModel<String> paramLabelModel,
            Class<N> type,
            IValidator<N>... validators) {

        super(id, paramValue);
        Label label = new Label("paramName", paramLabelModel.getObject());
        add(label);

        numberField = new NumberTextField<>("paramValue", paramValue, type);
        numberField.setLabel(paramLabelModel);
        numberField.setConvertEmptyInputStringToNull(true);

        // For Integer/Long types, ensure only whole numbers are entered
        if (Integer.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
            // Add step="1" attribute to prevent decimal input. This actually doesn't prevent decimal input though
            numberField.add(AttributeModifier.replace("step", "1"));
        }

        if (validators != null) {
            for (IValidator<N> validator : validators) {
                numberField.add(validator);
            }
        }
        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(numberField);
        add(feedback);
    }

    /**
     * Returns the internal form component for direct access.
     *
     * @return The NumberTextField component used in this panel
     */
    @Override
    public FormComponent<N> getFormComponent() {
        return numberField;
    }

    /**
     * Sets whether this field is required.
     *
     * @param required True if the field is required, false otherwise
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setRequired(boolean required) {
        numberField.setRequired(required);
        return this;
    }

    /**
     * Sets the minimum allowed value for the input.
     *
     * @param min The minimum value allowed
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setMinimum(N min) {
        numberField.setMinimum(min);
        return this;
    }

    /**
     * Sets the maximum allowed value for the input.
     *
     * @param max The maximum value allowed
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setMaximum(N max) {
        numberField.setMaximum(max);
        return this;
    }

    /**
     * Sets the step value used for incrementing/decrementing the value.
     *
     * @param step The step value
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setStep(N step) {
        numberField.setStep(step);
        return this;
    }

    /**
     * Sets the minimum allowed value from a model.
     *
     * @param min Model for the minimum value
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setMinimum(IModel<N> min) {
        numberField.setMinimum(min);
        return this;
    }

    /**
     * Sets the maximum allowed value from a model.
     *
     * @param max Model for the maximum value
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setMaximum(IModel<N> max) {
        numberField.setMaximum(max);
        return this;
    }

    /**
     * Sets the step value from a model.
     *
     * @param step Model for the step value
     * @return This panel instance for method chaining
     */
    public NumberParamPanel<N> setStep(IModel<N> step) {
        numberField.setStep(step);
        return this;
    }
}
