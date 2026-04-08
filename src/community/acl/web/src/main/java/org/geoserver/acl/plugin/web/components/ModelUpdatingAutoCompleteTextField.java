/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import java.util.Iterator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.geoserver.acl.plugin.web.support.SerializableFunction;

/**
 * {@link AutoCompleteTextField} that dynamically updates the model value by calling
 * {@code getModel().setObject(getConvertedInput())}
 */
@SuppressWarnings("serial")
public class ModelUpdatingAutoCompleteTextField<T> extends AutoCompleteTextField<T> {

    private SerializableFunction<String, Iterator<T>> choiceResolver;

    public ModelUpdatingAutoCompleteTextField(
            String id, IModel<T> model, SerializableFunction<String, Iterator<T>> choiceResolver) {
        this(id, model, choiceResolver, defaultSettings());
    }

    protected static AutoCompleteSettings defaultSettings() {
        return new AutoCompleteSettings()
                .setMaxHeightInPx(240)
                .setShowListOnEmptyInput(true)
                .setAdjustInputWidth(false)
                .setIgnoreBordersWhenPositioning(false);
    }

    public ModelUpdatingAutoCompleteTextField(
            String id,
            IModel<T> model,
            SerializableFunction<String, Iterator<T>> choiceResolver,
            AutoCompleteSettings settings) {

        this(id, model, choiceResolver, StringAutoCompleteRenderer.instance(), settings);
    }

    public ModelUpdatingAutoCompleteTextField(
            String id,
            IModel<T> model,
            SerializableFunction<String, Iterator<T>> choiceResolver,
            IAutoCompleteRenderer<T> renderer,
            AutoCompleteSettings settings) {
        super(id, model, null /* type */, renderer, settings);
        this.choiceResolver = choiceResolver;
        add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                T convertedInput = getConvertedInput();
                getModel().setObject(convertedInput);
            }
        });
    }

    @Override
    protected Iterator<T> getChoices(String input) {
        return choiceResolver.apply(input);
    }
}
