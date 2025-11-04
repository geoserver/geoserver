/* (c) 2014 - 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geotools.util.NumberRange;

/**
 * A form component for a {@link AttributeTypeInfo} object.
 *
 * <p>This panel provides the functionalities to edit an attribute and set restrictions on its value.
 *
 * @author Alessandro Ricchiuti
 */
public class AttributeTypeInfoEditPanel extends Panel {

    @Serial
    private static final long serialVersionUID = -4226325094883373205L;

    enum RESTRICTION_TYPE {
        NONE,
        OPTIONS,
        RANGE
    }

    private final IModel<AttributeTypeInfo> model;
    private final AttributeTypeInfo object;
    private ClassTextField typeTextField;
    private WebMarkupContainer restrictionsContainer;
    private DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDownChoice;
    private WebMarkupContainer optionsContainer;
    private ListMultipleChoice<Object> optionsListMultipleChoice;
    private TextField<Serializable> newOptionTextField;
    private WebMarkupContainer rangeContainer;
    private NumberTextField<? extends Number> rangeMinTextField;
    private NumberTextField<? extends Number> rangeMaxTextField;

    /**
     * Constructs the attribute type info edit panel with an explicit model.
     *
     * @param id The component id.
     * @param model The model, being a {@link AttributeTypeInfo}.
     */
    public AttributeTypeInfoEditPanel(String id, IModel<AttributeTypeInfo> model) {
        super(id, model);
        this.model = model;
        this.object = model.getObject();
        add(new Behavior() {
            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnLoadHeaderItem.forScript(adjustModalHeightScript()));
            }
        });
        initComponents();
    }

    void initComponents() {
        Form<AttributeTypeInfo> form = new Form<>("attributeForm", model);

        initAttributeInfoFields(form);

        initRestrictionsContainer();
        form.add(restrictionsContainer);

        form.add(newOptionValidator());
        form.add(rangeValidator());

        form.add(createFeedbackPanel());

        add(form);
    }

    private void initAttributeInfoFields(Form<AttributeTypeInfo> form) {
        TextField<String> nameTextField = new TextField<>("name", new PropertyModel<>(model, "name"));
        nameTextField.setRequired(true);

        initTypeTextField();

        TextArea<String> sourceTextArea = new TextArea<>("source", new PropertyModel<>(model, "source"));

        TextArea<String> descriptionTextArea =
                new TextArea<>("description", new PropertyModel<>(model, "description")) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) new InternationalStringConverter();
                    }
                };

        CheckBox nillableCheckBox = new CheckBox("nillable", new PropertyModel<>(model, "nillable"));

        form.add(nameTextField, typeTextField, sourceTextArea, descriptionTextArea, nillableCheckBox);
    }

    private void initTypeTextField() {
        typeTextField = new ClassTextField(Model.of(object.getBinding()));
        typeTextField.setType(Class.class);
        typeTextField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                restrictionTypeDropDownChoice.setModel(Model.of(RESTRICTION_TYPE.NONE));
                optionsListMultipleChoice.setChoices(new ArrayList<>());
                Class<?> convertedInput = typeTextField.getModelObject();
                rangeMinTextField.setType(convertedInput);
                rangeMaxTextField.setType(convertedInput);
                target.add(restrictionsContainer);
                target.add(restrictionTypeDropDownChoice);
                target.appendJavaScript(adjustModalHeightScript());
            }
        });
    }

    private void initRestrictionsContainer() {
        restrictionsContainer = new WebMarkupContainer("restrictionsContainer");
        restrictionsContainer.setOutputMarkupId(true);
        restrictionsContainer.setOutputMarkupPlaceholderTag(true);
        restrictionsContainer.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                component.setVisible(shouldRestrictionBeVisible());
            }
        });

        initRestrictionTypeDropDownChoice();
        restrictionsContainer.add(restrictionTypeDropDownChoice);

        initOptionsContainer();
        restrictionsContainer.add(optionsContainer);

        initRangeContainer();
        restrictionsContainer.add(rangeContainer);
    }

    private boolean shouldRestrictionBeVisible() {
        return typeTextField.getModelObject() != null && (isAttributeTypeString() || isAttributeTypeNumber());
    }

    private void initRestrictionTypeDropDownChoice() {
        restrictionTypeDropDownChoice = new DropDownChoice<>(
                "restrictionType",
                Model.of(computeActualRestrictionType()),
                new LoadableDetachableModel<List<RESTRICTION_TYPE>>() {
                    @Override
                    protected List<RESTRICTION_TYPE> load() {
                        return computeAvailableRestrictionTypes();
                    }
                },
                new IChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(RESTRICTION_TYPE type) {
                        return new StringResourceModel("AttributeTypeInfoEditPanel.restrictionType."
                                        + type.name().toLowerCase())
                                .getString();
                    }

                    @Override
                    public String getIdValue(RESTRICTION_TYPE type, int index) {
                        return type.name();
                    }
                });
        restrictionTypeDropDownChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(optionsContainer);
                target.add(rangeContainer);
                target.appendJavaScript(adjustModalHeightScript());
            }
        });
    }

    private RESTRICTION_TYPE computeActualRestrictionType() {
        if (object.getOptions() != null && !object.getOptions().isEmpty()) {
            return RESTRICTION_TYPE.OPTIONS;
        } else if (object.getRange() != null) {
            return RESTRICTION_TYPE.RANGE;
        } else {
            return RESTRICTION_TYPE.NONE;
        }
    }

    private List<RESTRICTION_TYPE> computeAvailableRestrictionTypes() {
        if (isAttributeTypeNumber()) {
            return List.of(RESTRICTION_TYPE.values());
        } else if (isAttributeTypeString()) {
            return List.of(RESTRICTION_TYPE.NONE, RESTRICTION_TYPE.OPTIONS);
        } else {
            return List.of();
        }
    }

    private boolean isAttributeTypeNumber() {
        return Number.class.isAssignableFrom(typeTextField.getModelObject());
    }

    private boolean isAttributeTypeString() {
        return String.class == typeTextField.getModelObject();
    }

    private String adjustModalHeightScript() {
        return "var modal = document.querySelector('.wicket-modal'); modal.style.height = '%spx';"
                .formatted(calculateModalHeight());
    }

    private int calculateModalHeight() {
        if (!shouldRestrictionBeVisible()) {
            return 350;
        }
        switch (restrictionTypeDropDownChoice.getModelObject()) {
            case NONE:
                return 450;
            case OPTIONS:
                return 600;
            case RANGE:
                return 500;
            default:
                return 400;
        }
    }

    private void initOptionsContainer() {
        optionsContainer = new WebMarkupContainer("optionsContainer");
        optionsContainer.setOutputMarkupPlaceholderTag(true);
        optionsContainer.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                component.setVisible(restrictionTypeDropDownChoice.getModelObject() == RESTRICTION_TYPE.OPTIONS);
            }
        });

        initOptionsListMultipleChoice();
        initNewOptionTextField();

        optionsContainer.add(
                optionsListMultipleChoice, createRemoveOptionButton(), newOptionTextField, createAddOptionButton());
    }

    private void initOptionsListMultipleChoice() {
        List<Object> options = object.getOptions();
        if (options == null) {
            options = new ArrayList<>();
        }

        /* renderer added to avoid locale conversion of numeric options */
        optionsListMultipleChoice = new ListMultipleChoice<>(
                "options", new Model<>(new ArrayList<>()), new ArrayList<>(options), (IChoiceRenderer<Object>)
                        String::valueOf);
        optionsListMultipleChoice.setOutputMarkupId(true);
    }

    private void initNewOptionTextField() {
        newOptionTextField = new TextField<>("newOption", new Model<>());
        newOptionTextField.setOutputMarkupPlaceholderTag(true);
        newOptionTextField.setOutputMarkupId(true);
    }

    private AjaxButton createRemoveOptionButton() {
        return new AjaxButton("removeOption") {
            @Override
            @SuppressWarnings("unchecked")
            public void onSubmit(AjaxRequestTarget target) {
                Collection<Object> selection = optionsListMultipleChoice.getModelObject();
                List<Object> choices = (List<Object>) optionsListMultipleChoice.getChoices();

                choices.removeAll(selection);
                selection.clear();

                optionsListMultipleChoice.setModelObject(selection);
                target.add(optionsListMultipleChoice);
            }
        };
    }

    private AjaxButton createAddOptionButton() {
        return new AjaxButton("addOption") {
            @Override
            @SuppressWarnings("unchecked")
            public void onSubmit(AjaxRequestTarget target) {
                String newOptionValue = newOptionTextField.getInput();
                if (newOptionValue != null && !newOptionValue.isBlank()) {
                    List<Object> choices = (List<Object>) optionsListMultipleChoice.getChoices();
                    choices.add(newOptionValue);

                    newOptionTextField.setModelObject(null);
                    newOptionTextField.modelChanged();
                    target.add(newOptionTextField);
                    target.add(optionsListMultipleChoice);
                }
            }
        };
    }

    private void initRangeContainer() {
        rangeContainer = new WebMarkupContainer("rangeContainer");
        rangeContainer.setOutputMarkupPlaceholderTag(true);
        rangeContainer.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                component.setVisible(restrictionTypeDropDownChoice.getModelObject() == RESTRICTION_TYPE.RANGE);
            }
        });

        initRangeTextFields();

        rangeContainer.add(rangeMinTextField, rangeMaxTextField);
    }

    @SuppressWarnings("unchecked")
    private void initRangeTextFields() {
        Number minValue = 0;
        Number maxValue = 0;

        if (object.getRange() != null) {
            NumberRange<? extends Number> range = object.getRange();
            range = range.castTo((Class) object.getBinding());
            minValue = range.getMinValue();
            maxValue = range.getMaxValue();
        }

        rangeMinTextField = new NumberTextField("rangeMin", new Model<>(minValue), object.getBinding());
        rangeMinTextField.setOutputMarkupId(true);
        rangeMinTextField.setLabel(new StringResourceModel("AttributeTypeInfoEditPanel.minInclusive"));

        rangeMaxTextField = new NumberTextField("rangeMax", new Model<>(maxValue), object.getBinding());
        rangeMaxTextField.setOutputMarkupId(true);
        rangeMaxTextField.setLabel(new StringResourceModel("AttributeTypeInfoEditPanel.maxInclusive"));
    }

    private IFormValidator newOptionValidator() {
        return new IFormValidator() {
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                if (restrictionTypeDropDownChoice.getModelObject() != RESTRICTION_TYPE.OPTIONS) {
                    return new FormComponent<?>[] {};
                } else {
                    return new FormComponent<?>[] {newOptionTextField};
                }
            }

            @Override
            public void validate(Form<?> form) {
                try {
                    String input = newOptionTextField.getInput();
                    if (input != null && !input.isBlank() && isAttributeTypeNumber()) {
                        Double.parseDouble(input);
                    }
                } catch (NumberFormatException ex) {
                    newOptionTextField.error(
                            new StringResourceModel("AttributeTypeInfoEditPanel.optionTypeNumberErrorMessage")
                                    .getString());
                }
            }
        };
    }

    private IFormValidator rangeValidator() {
        return new IFormValidator() {
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                if (restrictionTypeDropDownChoice.getModelObject() != RESTRICTION_TYPE.RANGE) {
                    return new FormComponent<?>[] {};
                } else {
                    return new FormComponent<?>[] {rangeMinTextField, rangeMaxTextField};
                }
            }

            @Override
            public void validate(Form<?> form) {
                Number min = rangeMinTextField.getConvertedInput();
                Number max = rangeMaxTextField.getConvertedInput();

                if (min != null && max != null && min.doubleValue() > max.doubleValue()) {
                    rangeMaxTextField.error(
                            new StringResourceModel("AttributeTypeInfoEditPanel.rangeMinMaxErrorMessage").getString());
                }
            }
        };
    }

    private FeedbackPanel createFeedbackPanel() {
        FeedbackPanel feedbackPanel = new FeedbackPanel("dialogFeedback");
        feedbackPanel.setOutputMarkupId(true);
        return feedbackPanel;
    }

    @SuppressWarnings("unchecked")
    public void finalizeAttributeValues() {

        if (ifBindingHasBeenChanged()) {
            object.setBinding(typeTextField.getModelObject());
        }

        switch (restrictionTypeDropDownChoice.getModelObject()) {
            case NONE:
                object.setOptions(null);
                object.setRange(null);
                break;
            case RANGE:
                object.setOptions(null);
                object.setRange(createRangeFromRangeFields());
                break;
            case OPTIONS:
                object.setRange(null);
                object.setOptions((List<Object>) optionsListMultipleChoice.getChoices());
                break;
        }
    }

    private boolean ifBindingHasBeenChanged() {
        return object.getBinding() != typeTextField.getModelObject();
    }

    /**
     * Since object range is not tied with the range fields models, this method creates a new {@link NumberRange} typed
     * as per the specified {@link #typeTextField}' model.
     */
    @SuppressWarnings("unchecked")
    private NumberRange<? extends Number> createRangeFromRangeFields() {
        Class<?> type = typeTextField.getModelObject();

        Number min = rangeMinTextField.getConvertedInput();
        Number max = rangeMaxTextField.getConvertedInput();

        if (Byte.class.equals(type)) {
            byte byteMin = (min != null) ? min.byteValue() : Byte.MIN_VALUE;
            byte byteMax = (max != null) ? max.byteValue() : Byte.MAX_VALUE;
            return NumberRange.create(byteMin, byteMax);
        }

        if (Short.class.equals(type)) {
            short shortMin = (min != null) ? min.shortValue() : Short.MIN_VALUE;
            short shortMax = (max != null) ? max.shortValue() : Short.MAX_VALUE;
            return NumberRange.create(shortMin, shortMax);
        }

        double doubleMin = min != null ? min.doubleValue() : Double.NEGATIVE_INFINITY;
        double doubleMax = max != null ? max.doubleValue() : Double.POSITIVE_INFINITY;

        NumberRange<Double> range = NumberRange.create(doubleMin, doubleMax);
        return range.castTo((Class) type);
    }
}
