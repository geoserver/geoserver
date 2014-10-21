/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.sf.cglib.core.Local;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;

/**
 * Form component to edit a List<String> that makes up the keywords field of
 * various catalog objects.
 */
@SuppressWarnings("serial")
public class KeywordsEditor extends FormComponentPanel {

    ListMultipleChoice choices;
    TextField newKeyword;
    TextField<String> vocabTextField;
    DropDownChoice<String> langChoice;
    
    /**
     * Creates a new keywords editor. 
     * @param id
     * @param keywords The module should return a non null collection of strings.
     */
    public KeywordsEditor(String id, final IModel keywords) {
        super(id, keywords);

        choices = new ListMultipleChoice("keywords", new Model(), 
            new ArrayList((List) keywords.getObject()), new ChoiceRenderer<Keyword>() {
                @Override
                public Object getDisplayValue(Keyword kw) {
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
        newKeyword = new TextField("newKeyword", new Model());
        newKeyword.setOutputMarkupId(true);
        add(newKeyword);

        langChoice = new DropDownChoice<String>("lang", new Model(), 
            Arrays.asList(Locale.getISOLanguages()), new ChoiceRenderer<String>() {
            @Override
            public Object getDisplayValue(String object) {
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

        vocabTextField = new TextField<String>("vocab", new Model());
        vocabTextField.setOutputMarkupId(true);
            
        add(vocabTextField);

        add(addKeywordsButton());
    }

    private AjaxButton addKeywordsButton() {
        AjaxButton button = new AjaxButton("addKeyword") {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
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
                
                List choiceList = choices.getChoices();
                choiceList.add(keyword);
                choices.setChoices(choiceList);
                
                langChoice.setModelObject(null);
                langChoice.modelChanged();

                vocabTextField.setModelObject(null);
                vocabTextField.modelChanged();

                target.addComponent(newKeyword);
                target.addComponent(langChoice);
                target.addComponent(vocabTextField);
                target.addComponent(choices);
            }
        };
        button.setDefaultFormProcessing(false);
        return button;
    }

    private AjaxButton removeKeywordsButton() {
        AjaxButton button = new AjaxButton("removeKeywords") {
            
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                List selection = (List) choices.getModelObject();
                List keywords = choices.getChoices();
                for (Iterator it = selection.iterator(); it.hasNext();) {
                    KeywordInfo selected = (KeywordInfo) it.next();
                    keywords.remove(selected);
                }
                choices.setChoices(keywords);
                choices.modelChanged();
                target.addComponent(choices);
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
    
    @Override
    protected void convertInput() {
        setConvertedInput(choices.getChoices());
    }
}
