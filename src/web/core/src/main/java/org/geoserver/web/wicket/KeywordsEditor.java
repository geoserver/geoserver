/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;

/**
 * Form component allows editing of the {@link KeywordInfo} defining {@code List<KeywordInfo>} keywords field of various
 * catalog objects.
 */
public class KeywordsEditor extends FormComponentPanel<List<KeywordInfo>> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(KeywordsEditor.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    /** Repeating list view, providing a row of controls to edit each keyword individually. */
    private KeywordListView keywordsView;

    /** Label displayed when no keywords are listed. */
    private Label noKeywords;

    /** Table listing keywords. */
    private WebMarkupContainer table;

    /**
     * Creates a new keywords editor.
     *
     * @param model The model should return a non null List of KeywordInfo Strings.
     */
    public KeywordsEditor(String id, final IModel<List<KeywordInfo>> model) {
        super(id, model);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the keywords table
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);

        // IModel<List<KeywordInfo>> internalModel = new PropertyModel<>(this, "keywordList");

        // the list view of keywords
        keywordsView = new KeywordListView(model, container);
        keywordsView.setOutputMarkupId(true);
        // this is necessary to avoid loosing item contents on edit/validation checks
        keywordsView.setReuseItems(true);
        table.add(keywordsView);

        GeoServerAjaxFormLink addKeyword = keywordsView.addKeywordLink("addKeyword");
        container.add(addKeyword);

        // the noKeywords  label
        noKeywords = new Label("noKeywords", new ResourceModel("noKeywords"));
        container.add(noKeywords);
        updateVisibility();
    }

    /** Keywords must not be empty and are unable to contain {@code \\} character. */
    public static class KeywordValueValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            String keyword = validatable.getValue();

            if (keyword != null && !keyword.trim().isEmpty()) {
                Matcher valueMatcher = KeywordInfo.isValidPattern.matcher(keyword);
                if (!valueMatcher.matches()) {
                    ValidationError invalidKeyword = new ValidationError("invalidKeyword")
                            .addKey("invalidKeyword")
                            .setVariable("keyword", keyword);
                    validatable.error(invalidKeyword);
                }
            } else {
                ValidationError nullKeywordValue = new ValidationError("nullKeywordValue").addKey("nullKeywordValue");
                validatable.error(nullKeywordValue);
            }
        }
    }

    /** Vocabulary is optional, but are unable to contain {@code \\} character. */
    public static class VocabularyValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            String vocabulary = validatable.getValue();

            if (vocabulary != null) {
                Matcher vocabMatcher = KeywordInfo.isValidPattern.matcher(vocabulary);
                if (!vocabMatcher.matches()) {
                    ValidationError invalidVocabulary = new ValidationError("invalidVocabulary")
                            .addKey("invalidVocabulary")
                            .setVariable("vocabulary", vocabulary);
                    validatable.error(invalidVocabulary);
                }
            }
        }
    }

    private void updateVisibility() {
        @SuppressWarnings("unchecked")
        List<KeywordInfo> keywordList = (List<KeywordInfo>) getDefaultModelObject();
        boolean hasKeywords = keywordList != null && !keywordList.isEmpty();

        // table.setVisible(hasKeywords);
        noKeywords.setVisible(!hasKeywords);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        List<KeywordInfo> list = getModelObject();
        keywordsView.setModelObject(list);

        updateVisibility();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        processInputs();
        setConvertedInput(keywordsView.getList());
    }

    /**
     * Process submitted (non {@code null}) input on FormComponents, this has the effect of updating the model contents.
     */
    public void processInputs() {
        this.visitChildren(FormComponent.class, (component, visit) -> {
            FormComponent<?> formComponent = (FormComponent<?>) component;
            if (formComponent.getInput() != null) {
                formComponent.processInput();
            }
            visit.dontGoDeeper();
        });
    }

    /** Render langauge choice using the same appearance as InternationalStringEditor. */
    private static class LocaleChoiceRenderer extends ChoiceRenderer<String> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Object getDisplayValue(String language) {
            Locale locale = new Locale(language);

            String languageTag = locale.toLanguageTag();
            String displayName = locale.getDisplayName(locale);
            return languageTag + " - " + displayName;
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }

    /** ListView of keywords, providing a row for each keyword, along with links to add/remove keyword. */
    private class KeywordListView extends ListView<KeywordInfo> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final WebMarkupContainer container;

        public KeywordListView(IModel<? extends List<KeywordInfo>> model, WebMarkupContainer container) {
            super("keywords", model);
            this.container = container;
        }

        @Override
        protected void populateItem(final ListItem<KeywordInfo> item) {
            // odd/even style
            item.add(AttributeModifier.replace("class", item.getIndex() % 2 == 0 ? "even" : "odd"));

            FormComponentFeedbackBorder keywordBorder = new FormComponentFeedbackBorder("keywordBorder");
            item.add(keywordBorder);

            // keyword info
            TextField<String> keywordValue = new TextField<>("keyword", new PropertyModel<>(item.getModel(), "value"));
            keywordValue.add(new KeywordValueValidator());
            keywordValue.setOutputMarkupId(true);
            keywordValue.setRequired(true);
            keywordBorder.add(keywordValue);

            DropDownChoice<String> language = new DropDownChoice<>(
                    "language",
                    new PropertyModel<>(item.getModel(), "language"),
                    Arrays.asList(Locale.getISOLanguages()),
                    new LocaleChoiceRenderer());
            language.setNullValid(true);
            language.setOutputMarkupId(true);
            keywordBorder.add(language);

            FormComponentFeedbackBorder vocabularyBorder = new FormComponentFeedbackBorder("vocabularyBorder");
            item.add(vocabularyBorder);

            TextField<String> vocabulary =
                    new TextField<>("vocabulary", new PropertyModel<>(item.getModel(), "vocabulary"));
            vocabulary.add(new VocabularyValidator());
            vocabulary.setOutputMarkupId(true);
            vocabularyBorder.add(vocabulary);

            GeoServerAjaxFormLink removeKeyword = removeKeywordLink("removeKeyword", item);
            ContextImage image = new ContextImage("image", "img/icons/silk/delete.png");
            removeKeyword.add(image);
            item.add(removeKeyword);
        }

        public GeoServerAjaxFormLink addKeywordLink(final String id) {
            return new GeoServerAjaxFormLink(id) {
                @Serial
                private static final long serialVersionUID = -4136656891019857299L;

                @Override
                protected void onClick(AjaxRequestTarget target, Form<?> form) {
                    KeywordsEditor.this.processInputs();

                    KeywordInfo keyword = new Keyword("");

                    List<KeywordInfo> list = getList();
                    list.add(keyword);
                    setList(list);

                    updateVisibility();
                    target.add(container);
                }
            };
        }

        public GeoServerAjaxFormLink removeKeywordLink(final String id, final ListItem<KeywordInfo> item) {
            return new GeoServerAjaxFormLink(id) {
                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                protected void onClick(AjaxRequestTarget target, Form<?> form) {
                    KeywordsEditor.this.processInputs();

                    List<KeywordInfo> list = getList();
                    list.remove(item.getIndex());
                    setModelObject(list);

                    updateVisibility();

                    // update the enclosing container so the UI reflects the change
                    target.add(container);
                }
            };
        }
    }
}
