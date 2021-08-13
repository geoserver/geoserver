/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.data.resource.LocalesDropdown;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.GrowableInternationalString;

/**
 * An abstract panel meant to be used to display international content.
 *
 * @param <C> the type of the AbstractTextComponent implementation meant to be used as the text
 *     field
 */
public abstract class InternationalStringPanel<C extends AbstractTextComponent<String>>
        extends FormComponentPanel<GrowableInternationalString> {

    GrowableStringModel growableModel;

    GeoServerTablePanel<GrowableStringModel.InternationalStringEntry> tablePanel;

    C nonInternationalComponent;

    InternationalEntriesProvider provider;

    static final String CHECKBOX_SUFFIX = "_i18nCheckbox";

    GeoServerAjaxFormLink geoServerAjaxFormLink;

    public InternationalStringPanel(
            String id,
            IModel<GrowableInternationalString> model,
            C nonInternationalComponent,
            WebMarkupContainer checkBoxContainer) {
        super(id, model);
        // in case the internationalComponent is null set a new instance
        // with the non international value as the starting one.
        setObjectIfMissing(model, nonInternationalComponent);
        this.nonInternationalComponent = nonInternationalComponent;
        this.nonInternationalComponent.setOutputMarkupId(true);
        this.nonInternationalComponent.setOutputMarkupPlaceholderTag(true);
        initUI(new GrowableStringModel(model), checkBoxContainer);
        setOutputMarkupId(true);
        setOutputMarkupId(true);
    }

    private void setObjectIfMissing(
            IModel<GrowableInternationalString> model, C nonInternationalComponent) {
        GrowableInternationalString obj = model.getObject();
        if (obj == null || obj.getLocales().isEmpty()) {
            if (nonInternationalComponent.getModelObject() != null) {
                model.setObject(
                        new GrowableInternationalString(
                                nonInternationalComponent.getModelObject()));
            } else {
                model.setObject(new GrowableInternationalString());
            }
        }
    }

    private void initUI(GrowableStringModel model, WebMarkupContainer checkBoxContainer) {
        this.growableModel = model;
        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setOutputMarkupId(true);
        boolean i18nVisible = i18nVisible();
        nonInternationalComponent.setVisible(!i18nVisible);
        AjaxCheckBox checkbox =
                new AjaxCheckBox(
                        checkBoxContainer.getId() + CHECKBOX_SUFFIX, new Model<>(i18nVisible)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        // this does get form submits, processing here is not useful.
                        // see the AjaxFormSubmitBehavior below
                    }
                };
        AjaxFormSubmitBehavior checkboxSubmitter =
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        switchVisibility(target);
                    }

                    private void switchVisibility(AjaxRequestTarget target) {
                        if (checkbox.getConvertedInput().booleanValue()) {
                            container.setVisible(true);
                            nonInternationalComponent.setVisible(false);
                        } else {
                            nonInternationalComponent.setVisible(true);
                            container.setVisible(false);
                        }
                        target.add(checkbox, container, nonInternationalComponent, tablePanel);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        switchVisibility(target);
                    }
                };
        checkbox.add(checkboxSubmitter);
        checkBoxContainer.add(checkbox);
        container.setVisible(i18nVisible);
        container.add(
                geoServerAjaxFormLink =
                        new GeoServerAjaxFormLink("addNew") {

                            private static final long serialVersionUID = -4136656891019857299L;

                            @Override
                            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                                provider.getItems()
                                        .add(new GrowableStringModel.InternationalStringEntry());
                                tablePanel.modelChanged();
                                target.add(tablePanel);
                            }
                        });
        provider = new InternationalEntriesProvider();
        GeoServerTablePanel<GrowableStringModel.InternationalStringEntry> tablePanel =
                new GeoServerTablePanel<GrowableStringModel.InternationalStringEntry>(
                        "tablePanel", provider) {
                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<GrowableStringModel.InternationalStringEntry> itemModel,
                            GeoServerDataProvider.Property<
                                            GrowableStringModel.InternationalStringEntry>
                                    property) {
                        if (property.getName().equals("locale")) {
                            Fragment localeFragment =
                                    new Fragment(
                                            id, "selectFragment", InternationalStringPanel.this);
                            FormComponentFeedbackBorder localeBorder =
                                    new FormComponentFeedbackBorder("border");
                            localeFragment.add(localeBorder);
                            LocalesDropdown locales =
                                    new LocalesDropdown(
                                            "select", new PropertyModel<>(itemModel, "locale"));
                            locales.setLabel(
                                    new ParamResourceModel(
                                            "th.locale", InternationalStringPanel.this));
                            localeBorder.add(locales);
                            locales.add(new LocaleValidator());
                            locales.setNullValid(true);
                            return localeFragment;
                        } else if (property.getName().equals("text")) {
                            AbstractTextComponent<String> field =
                                    getTextComponent("txt", new PropertyModel<>(itemModel, "text"));
                            String fragmentId;
                            if (field instanceof TextArea) fragmentId = "txtAreaFragment";
                            else if (field instanceof TextField) fragmentId = "txtFragment";
                            else
                                throw new RuntimeException(
                                        "The text component has to be either a TextField or a TextArea");

                            Fragment textFragment =
                                    new Fragment(id, fragmentId, InternationalStringPanel.this);
                            FormComponentFeedbackBorder textBorder =
                                    new FormComponentFeedbackBorder("border");
                            textFragment.add(textBorder);
                            field.setLabel(
                                    new ParamResourceModel(
                                            "th.text", InternationalStringPanel.this));
                            textBorder.add(field);
                            field.setRequired(true);
                            return textFragment;
                        } else if ("remove".equals(property.getName())) {
                            Fragment removeFragment =
                                    new Fragment(
                                            id, "removeFragment", InternationalStringPanel.this);
                            GeoServerAjaxFormLink removeLink =
                                    new GeoServerAjaxFormLink("remove") {

                                        @Override
                                        protected void onClick(
                                                AjaxRequestTarget target, Form form) {
                                            GrowableStringModel.InternationalStringEntry entry =
                                                    itemModel.getObject();
                                            provider.getItems().remove(entry);
                                            InternationalStringPanel.this.modelChanged();
                                            target.add(InternationalStringPanel.this);
                                        }
                                    };
                            removeFragment.add(removeLink);
                            return removeFragment;
                        }
                        return null;
                    }

                    @Override
                    public void processInputs() {
                        this.visitChildren(
                                FormComponent.class,
                                (component, visit) -> {
                                    ((FormComponent<?>) component).convertInput();
                                    ((FormComponent<?>) component).processInput();
                                });
                    }
                };
        tablePanel.setSelectable(false);
        tablePanel.setFilterable(false);
        tablePanel.setPageable(false);
        container.add(this.tablePanel = tablePanel);
        add(container);
        this.tablePanel.setOutputMarkupId(true);
    }

    protected abstract C getTextComponent(String id, IModel<String> model);

    class InternationalEntriesProvider
            extends GeoServerDataProvider<GrowableStringModel.InternationalStringEntry> {

        List<GrowableStringModel.InternationalStringEntry> internationalEntries;

        public InternationalEntriesProvider() {}

        @Override
        protected List<Property<GrowableStringModel.InternationalStringEntry>> getProperties() {
            return Arrays.asList(
                    new BeanProperty<>("locale", "locale"),
                    new BeanProperty<>("text", "text"),
                    new BeanProperty<>("remove", "remove"));
        }

        @Override
        protected List<GrowableStringModel.InternationalStringEntry> getItems() {
            if (internationalEntries == null)
                internationalEntries = new ArrayList<>(growableModel.getEntries());
            return internationalEntries;
        }

        void setInternationalEntries(List<GrowableStringModel.InternationalStringEntry> entries) {
            this.internationalEntries = entries;
        }
    }

    @Override
    public void convertInput() {
        setConvertedInput(updateGrowableString());
    }

    private GrowableInternationalString updateGrowableString() {
        GrowableInternationalString growableString = new GrowableInternationalString();
        List<GrowableStringModel.InternationalStringEntry> items = provider.getItems();
        for (GrowableStringModel.InternationalStringEntry entry : items) {
            if (canAddElement(items, entry)) growableString.add(entry.getLocale(), entry.getText());
        }
        growableModel.setObject(growableString);
        return growableString;
    }

    // Since null locale is supported and the validation occurs before the
    // submission this method is used to determine
    // if an InternationalStringEntry has null locale because explicitly set or because
    // the input has not been converted. This in order to prevent the addition of entries with
    // duplicated null key that will cause an exception to be thrown.
    private boolean canAddElement(
            List<GrowableStringModel.InternationalStringEntry> items,
            GrowableStringModel.InternationalStringEntry entry) {
        // input has not been converted yet skip addition
        if (entry.getText() == null) return false;
        long count = items.stream().filter(l -> l.getLocale() == null).count();
        int lastElem = items.size() - 1;
        // the lang is null because input has not been converted yet. Return false.
        if (entry.getLocale() == null && (items.indexOf(entry) == lastElem) && count > 1)
            return false;
        return true;
    }

    @Override
    public void updateModel() {
        if (isSaveSubmit()) {
            if (nonInternationalComponent.isVisible()) {
                growableModel.setObject(null);
                provider.setInternationalEntries(new ArrayList<>());
            } else {
                nonInternationalComponent.clearInput();
                nonInternationalComponent.setConvertedInput(null);
                nonInternationalComponent.setModelObject(null);
            }
            nonInternationalComponent.modelChanged();
        }
        String errorMsg = validateNullLocale();
        if (errorMsg != null) {
            getForm().error(errorMsg);
            return;
        }
        updateGrowableString();
    }

    private boolean isSaveSubmit() {
        boolean result = false;
        IFormSubmitter submitBtn = getForm().findSubmittingButton();
        if (submitBtn != null && submitBtn instanceof Component) {
            Component submitLink = (Component) submitBtn;
            String id = submitLink.getId();
            result = id.equals("submit") || id.equals("save");
        }
        return result;
    }

    private boolean i18nVisible() {
        List<GrowableStringModel.InternationalStringEntry> entries = growableModel.getEntries();
        return !entries.isEmpty() && !(entries.size() == 1 && entries.get(0).getLocale() == null);
    }

    private class LocaleValidator implements IValidator<Locale> {
        @Override
        public void validate(IValidatable<Locale> iValidatable) {
            Locale locale = iValidatable.getValue();
            List<GrowableStringModel.InternationalStringEntry> items = provider.getItems();
            // null locale validation occurs before saving. Here we cannot tell if the model object
            // is null because
            // null is the input or because wicket did not convert input
            long count =
                    items.stream()
                            .filter(i -> i.getLocale() != null && i.getLocale().equals(locale))
                            .count();
            boolean exisits =
                    count >= 1
                            && (iValidatable.getModel().getObject() == null
                                    || !iValidatable.getModel().getObject().equals(locale));
            if (locale != null && (exisits || count > 2)) {

                ValidationError error = new ValidationError();
                error.setMessage(duplicateLocaleMessage(locale));
                iValidatable.error(error);
            }
        }
    }

    private String duplicateLocaleMessage(Locale locale) {
        String lang =
                locale != null && locale.toLanguageTag() != null ? locale.toLanguageTag() : "empty";
        String message =
                new StringResourceModel("InternationalStringPanel.duplicatedLocale", this)
                                .getString()
                        + " "
                        + lang;
        return message;
    }

    private String validateNullLocale() {
        List<GrowableStringModel.InternationalStringEntry> entries = provider.getItems();
        long count = entries.stream().filter(i -> i != null && i.getLocale() == null).count();
        if (count > 1) {
            return duplicateLocaleMessage(null);
        }
        return null;
    }
}
