/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;

/** Form component to edit a List<String> that makes up the keywords field of various catalog objects. */
// TODO WICKET8 - Verify this page works OK
public class KeywordsEditor extends FormComponentPanel<List<KeywordInfo>> {

    @Serial
    private static final long serialVersionUID = 1L;

    ListMultipleChoice<KeywordInfo> choices;
    TextField<String> newKeyword;
    TextField<String> vocabTextField;
    DropDownChoice<String> langChoice;

    /**
     * Creates a new keywords editor.
     *
     * @param keywords The module should return a non null collection of strings.
     */
    public KeywordsEditor(String id, final IModel<List<KeywordInfo>> keywords) {
        super(id, keywords);

        choices = new ListMultipleChoice<>(
                "keywords", new Model<>(), new ArrayList<>(keywords.getObject()), new ChoiceRenderer<>() {
                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(KeywordInfo kw) {
                        StringBuffer sb = new StringBuffer(kw.getValue());
                        if (kw.getLanguage() != null) {
                            sb.append(" (").append(kw.getLanguage()).append(")");
                        }
                        if (kw.getVocabulary() != null) {
                            sb.append(" [").append(kw.getVocabulary()).append("]");
                        }
                        return sb.toString();
                    }
                });
        choices.setOutputMarkupId(true);
        add(choices);
        add(removeKeywordsButton());
        newKeyword = new TextField<>("newKeyword", new Model<>());
        newKeyword.setOutputMarkupId(true);
        add(newKeyword);

        langChoice = new DropDownChoice<>(
                "lang", new Model<>(), Arrays.asList(Locale.getISOLanguages()), new ChoiceRenderer<>() {
                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getDisplayValue(String object) {
                        return new Locale(object).getDisplayLanguage();
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        return object;
                    }
                });

        langChoice.setNullValid(true);
        langChoice.setOutputMarkupId(true);
        add(langChoice);

        vocabTextField = new TextField<>("vocab", new Model<>());
        vocabTextField.setOutputMarkupId(true);

        add(vocabTextField);

        add(addKeywordsButton());
    }

    private AjaxButton addKeywordsButton() {
        AjaxButton button = new AjaxButton("addKeyword") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                String value = newKeyword.getInput();
                String lang = langChoice.getInput();
                String vocab = vocabTextField.getInput();

                KeywordInfo keyword = new Keyword(value);
                if (lang != null && !"".equals(lang.trim())) {
                    keyword.setLanguage(lang);
                }
                if (vocab != null && !"".equals(vocab.trim())) {
                    keyword.setVocabulary(vocab);
                }

                @SuppressWarnings("unchecked")
                List<KeywordInfo> choiceList = (List<KeywordInfo>) choices.getChoices();
                choiceList.add(keyword);
                choices.setChoices(choiceList);

                langChoice.setModelObject(null);
                langChoice.modelChanged();

                vocabTextField.setModelObject(null);
                vocabTextField.modelChanged();

                target.add(newKeyword);
                target.add(langChoice);
                target.add(vocabTextField);
                target.add(choices);
            }
        };
        button.setDefaultFormProcessing(false);
        return button;
    }

    private AjaxButton removeKeywordsButton() {
        AjaxButton button = new AjaxButton("removeKeywords") {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                Collection<KeywordInfo> selection = choices.getModelObject();
                @SuppressWarnings("unchecked")
                List<KeywordInfo> keywords = (List<KeywordInfo>) choices.getChoices();
                for (KeywordInfo selected : selection) {
                    keywords.remove(selected);
                }
                choices.setChoices(keywords);
                choices.modelChanged();
                target.add(choices);
            }
        };
        // button.setDefaultFormProcessing(false);
        return button;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        updateFields();
    }

    private void updateFields() {
        choices.setChoices(getModel());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        setConvertedInput((List<KeywordInfo>) choices.getChoices());
    }
}
