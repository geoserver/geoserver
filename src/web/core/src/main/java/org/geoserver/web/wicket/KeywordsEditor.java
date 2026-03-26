/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.web.InternationalStringPanel;
import org.geoserver.web.data.resource.LocalesDropdown;

/**
 * Shows and allows editing of the  {@link KeywordInfo} defining {@cdode } List<String>} keywords field of various catalog objects.
 */
public class KeywordsEditor extends Panel {

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

    /**
     * Repeating list view, providing a row of controls to edit each keyword individually.
     */
    private ListView<KeywordInfo> keywordsView;
    /**
     * Label displayed when no keywords are listed.
     */
    private Label noKeywords;
    /**
     * Table listing keywords.
     */
    private WebMarkupContainer table;

    /**
     * Creates a new keywords editor.
     *
     * @param keywordModel The model should return a non null List of KeywordInfo Strings.
     */
    public KeywordsEditor(String id, final IModel<List<KeywordInfo>> keywordModel) {
        super(id, keywordModel);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the keywords table
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);;

        // add new keyword button
        GeoServerAjaxFormLink addKeyword = new GeoServerAjaxFormLink("addKeyword") {
            @Serial
            private static final long serialVersionUID = -4136656891019857299L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                KeywordInfo keyword = new Keyword("");

                List<KeywordInfo> keywordList = keywordModel.getObject();
                keywordList.add(keyword);

                keywordModel.setObject(keywordList);
                keywordsView.modelChanged();

                updateVisibility();
                target.add(container);
            }
        };
        container.add(addKeyword);

        // the list view of keywords
        keywordsView = new ListView<>("keywords", keywordModel) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<KeywordInfo> item) {

                // odd/even style
                item.add(AttributeModifier.replace("class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                FormComponentFeedbackBorder keywordBorder = new FormComponentFeedbackBorder("keywordBorder");
                item.add(keywordBorder);

                // keyword info
                TextField<String> keywordValue = new TextField<>("keyword", new PropertyModel<>(item.getModel(), "value"));
                keywordValue.add(new KeywordValueValidator());
                keywordValue.setRequired(true);
                keywordBorder.add(keywordValue);

//                FormComponentFeedbackBorder languageBorder = new FormComponentFeedbackBorder("languageBorder");
//                item.add(languageBorder);

                LocalesDropdown language =
                        new LocalesDropdown("language", new PropertyModel<>(item.getModel(), "language"));

                ChoiceRenderer<Locale> languageTagRenderer = new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(Locale object) {
                        String languageTag = object.toLanguageTag();
                        return languageTag;
                    }
                    @Override
                    public String getIdValue(Locale object, int index) {
                        return object.toLanguageTag();
                    }
                };
                language.setChoiceRenderer(languageTagRenderer);
                language.setNullValid(true);

                keywordBorder.add(language);
                // languageBorder.add(new InternationalStringPanel.LocaleValidator());

                FormComponentFeedbackBorder vocabularyBorder = new FormComponentFeedbackBorder("vocabularyBorder");
                item.add(vocabularyBorder);

                TextField<String> vocabulary = new TextField<>("vocabulary", new PropertyModel<>(item.getModel(), "vocabulary"));
                vocabulary.add(new VocabularyValidator());
                vocabularyBorder.add(vocabulary);

                // remove link
                GeoServerAjaxFormLink removeKeyword = new GeoServerAjaxFormLink("removeKeyword") {
                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        addStateChange();
                        item.modelChanging();

                        // remove item
                        List<KeywordInfo> keywordList = getList();
                        keywordList.remove(item.getIndex());
                        keywordModel.setObject(keywordList);

                        keywordsView.modelChanged();

                        // invalid listview
                        // keywordsView.removeAll();

                        // keywordList.remove(keywordInfo);
                        // keywordsView.setModelObject(keywordList);
                        updateVisibility();
                        target.add(container);
                    }
                };
                ContextImage image = new ContextImage("image", "img/icons/silk/delete.png");
                removeKeyword.add(image);
                item.add(removeKeyword);
            }
        };
        keywordsView.setOutputMarkupId(true);
        // this is necessary to avoid loosing item contents on edit/validation checks
        keywordsView.setReuseItems(true);
        table.add(keywordsView);

        // the noKeywords  label
        noKeywords = new Label("noKeywords",
                new ResourceModel("noKeywords"));
        container.add(noKeywords);
        updateVisibility();
    }

    /**
     * Keywords must not be empty and are unable to contain {@code \\} character.
     */
    public static class KeywordValueValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            String keyword = validatable.getValue();;
            if (keyword != null) {
                Matcher valueMatcher = KeywordInfo.isValidPattern.matcher(keyword);
                if (!valueMatcher.matches()) {
                    ValidationError invalidKeyword = new ValidationError("invalidKeyword")
                            .addKey("invalidKeyword")
                            .setVariable("keyword",keyword);
                    validatable.error(invalidKeyword);
                }
            }
            else {
                ValidationError nullKeywordValue = new ValidationError("nullKeywordValue")
                        .addKey("nullKeywordValue");
                validatable.error(nullKeywordValue);
            }
        }
    };

    /**
     * Vocabulary is optional, but are unable to contain {@code \\} character.
     */
    public static class VocabularyValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            String vocabulary = validatable.getValue();;
            if (vocabulary != null) {
                Matcher vocabMatcher = KeywordInfo.isValidPattern.matcher(vocabulary);
                if (!vocabMatcher.matches()) {
                    ValidationError invalidVocabulary = new ValidationError("invalidVocabulary")
                            .addKey("invalidVocabulary")
                            .setVariable("vocabulary",vocabulary);
                    validatable.error(invalidVocabulary);
                }
            }
        }
    };

    private void updateVisibility() {
        List<KeywordInfo> keywordList = (List<KeywordInfo>) getDefaultModelObject();
        boolean hasKeywords = keywordList != null && !keywordList.isEmpty();

        // table.setVisible(hasKeywords);
        noKeywords.setVisible(!hasKeywords);
    }

}
