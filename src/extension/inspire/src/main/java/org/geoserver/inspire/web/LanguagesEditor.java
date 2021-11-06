/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class LanguagesEditor extends FormComponentPanel<String> {
    ListMultipleChoice<String> languages;
    DropDownChoice<String> langChoice;

    public LanguagesEditor(String id, IModel<String> model) {
        super(id, model);
        List<String> langList;
        String strLanguages = model.getObject();
        if (strLanguages != null) {
            langList = Arrays.asList(strLanguages.split(","));
        } else {
            langList = new ArrayList<>();
        }
        languages = new ListMultipleChoice<>("languages", new Model<ArrayList<String>>(), langList);

        languages.setOutputMarkupId(true);
        add(languages);

        langChoice = new LanguageDropDownChoice("selectLanguage", new Model<>());
        langChoice.setChoiceRenderer(
                new ChoiceRenderer<String>() {
                    @Override
                    public String getIdValue(String object, int index) {
                        return object;
                    }
                });
        langChoice.setOutputMarkupId(true);
        add(langChoice);
        add(addButton());
        add(removeButton());
    }

    private AjaxButton addButton() {
        AjaxButton button =
                new AjaxButton("addLanguage") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        String value = langChoice.getInput();
                        langChoice.setModelObject(null);
                        langChoice.modelChanged();

                        @SuppressWarnings("unchecked")
                        List<String> languagesList = (List<String>) languages.getChoices();
                        List<String> newList = new ArrayList<>(languagesList);
                        newList.removeIf(l -> l.contains(value) || value.contains(l));
                        newList.add(value);
                        languages.setChoices(newList);
                        languages.modelChanged();
                        target.add(langChoice, languages);
                    }
                };
        button.setDefaultFormProcessing(false);
        return button;
    }

    private AjaxButton removeButton() {
        AjaxButton button =
                new AjaxButton("removeLanguages") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        Collection<String> selection = languages.getModelObject();
                        List<String> languagesList = new ArrayList<>(languages.getChoices());
                        for (String selected : selection) {
                            languagesList.remove(selected);
                        }
                        languages.setChoices(languagesList);
                        languages.modelChanged();
                        target.add(languages);
                    }
                };
        return button;
    }

    @Override
    public void convertInput() {
        super.convertInput();
        @SuppressWarnings("unchecked")
        Collection<String> list = (Collection<String>) languages.getChoices();
        StringBuffer sb = new StringBuffer("");
        int i = 0;
        for (String l : list) {
            sb.append(l);
            if (i != list.size() - 1) {
                sb.append(",");
            }
        }
        super.setConvertedInput(sb.toString());
    }
}
