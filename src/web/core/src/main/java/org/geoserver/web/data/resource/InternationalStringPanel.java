/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
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

    public InternationalStringPanel(
            String id, IModel<GrowableInternationalString> model, C nonInternationalComponent) {
        super(id, model);
        this.nonInternationalComponent = nonInternationalComponent;
        this.nonInternationalComponent.setOutputMarkupId(true);
        this.nonInternationalComponent.setOutputMarkupPlaceholderTag(true);
        initUI(new GrowableStringModel(model));
        setOutputMarkupId(true);
        setOutputMarkupId(true);
    }

    private void initUI(GrowableStringModel model) {
        this.growableModel = model;
        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        container.setOutputMarkupId(true);
        boolean i18nVisible = !growableModel.getEntries().isEmpty();
        nonInternationalComponent.setVisible(!i18nVisible);
        AjaxCheckBox checkbox =
                new AjaxCheckBox("i18nCheckBox", new Model<>(i18nVisible)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        if (getConvertedInput().booleanValue()) {
                            container.setVisible(true);
                            tablePanel.modelChanged();
                            nonInternationalComponent.setVisible(false);
                            nonInternationalComponent.setModelObject(null);
                            nonInternationalComponent.modelChanged();
                        } else {
                            nonInternationalComponent.setVisible(true);
                            growableModel.setObject(new GrowableInternationalString());
                            provider.setInternationalEntries(new ArrayList<>());
                            this.modelChanged();
                            container.setVisible(false);
                            ajaxRequestTarget.add(this, tablePanel);
                        }
                        ajaxRequestTarget.add(container, nonInternationalComponent, tablePanel);
                    }
                };
        add(checkbox);
        container.setVisible(i18nVisible);
        container.add(
                new GeoServerAjaxFormLink("addNew") {

                    private static final long serialVersionUID = -4136656891019857299L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        provider.getItems().add(new GrowableStringModel.InternationalStringEntry());
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
                            DropDownLocale locales =
                                    new DropDownLocale(
                                            "select",
                                            new PropertyModel<>(itemModel, "locale"),
                                            getLocalesList());
                            locales.setLabel(
                                    new ParamResourceModel(
                                            "th.locale", InternationalStringPanel.this));
                            localeBorder.add(locales);
                            locales.setNullValid(true);
                            locales.setRequired(true);
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

    private List<Locale> getLocalesList() {
        return Stream.of(Locale.getAvailableLocales())
                .filter(l -> l != null)
                .sorted(
                        new Comparator<Locale>() {
                            @Override
                            public int compare(Locale o1, Locale o2) {
                                return o1.toLanguageTag().compareTo(o2.toLanguageTag());
                            }
                        })
                .collect(Collectors.toList());
    }

    class DropDownLocale extends DropDownChoice<Locale> {

        public DropDownLocale(String id, IModel<Locale> model, List<? extends Locale> choices) {
            super(id, model, choices);
            ChoiceRenderer<Locale> locales =
                    new ChoiceRenderer<Locale>() {
                        @Override
                        public Object getDisplayValue(Locale object) {
                            return object.toLanguageTag();
                        }

                        @Override
                        public String getIdValue(Locale object, int index) {
                            return object.toLanguageTag();
                        }
                    };
            this.setChoiceRenderer(locales);
        }
    }

    @Override
    public void convertInput() {
        setConvertedInput(updateGrowableString());
    }

    private GrowableInternationalString updateGrowableString() {
        GrowableInternationalString growableString = new GrowableInternationalString();
        for (GrowableStringModel.InternationalStringEntry entry : provider.getItems()) {
            growableString.add(entry.getLocale(), entry.getText());
        }
        growableModel.setObject(growableString);
        return growableString;
    }

    @Override
    public void updateModel() {
        updateGrowableString();
    }
}
