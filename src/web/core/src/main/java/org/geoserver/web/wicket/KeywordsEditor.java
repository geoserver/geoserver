/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Form component to edit a List<String> that makes up the keywords field of
 * various catalog objects.
 */
@SuppressWarnings("serial")
public class KeywordsEditor extends FormComponentPanel {

    ListMultipleChoice choices;
    TextField newKeyword;

    /**
     * Creates a new keywords editor. 
     * @param id
     * @param keywords The module should return a non null collection of strings.
     */
    public KeywordsEditor(String id, final IModel keywords) {
        super(id, keywords);

        choices = new ListMultipleChoice("keywords", new Model(), new ArrayList((List) keywords.getObject()));
        choices.setOutputMarkupId(true);
        add(choices);
        add(removeKeywordsButton());
        newKeyword = new TextField("newKeyword", new Model());
        newKeyword.setOutputMarkupId(true);
        add(newKeyword);
        add(addKeywordsButton());
    }

    private AjaxButton addKeywordsButton() {
        AjaxButton button = new AjaxButton("addKeyword") {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                List choiceList = choices.getChoices();
                choiceList.add(newKeyword.getInput());
                choices.setChoices(choiceList);
                newKeyword.setModelObject(null);
                newKeyword.modelChanged();
                target.addComponent(newKeyword);
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
                    String selected = (String) it.next();
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
