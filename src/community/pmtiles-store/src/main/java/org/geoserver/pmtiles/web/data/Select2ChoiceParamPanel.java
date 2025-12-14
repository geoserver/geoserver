/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.springframework.util.StringUtils;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.StringTextChoiceProvider;

/**
 * A {@link Select2Choice}-based {@link ParamPanel} that allows entering a value that's not in the list of options by
 * setting {@link #allowCustomValues(boolean) allowCustomValues(true)} qe
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class Select2ChoiceParamPanel<T extends Serializable> extends Panel implements ParamPanel<T> {

    private Select2Choice<T> choice;

    public Select2ChoiceParamPanel(String id, IModel<String> labelModel, IModel<T> model, ChoiceProvider<T> provider) {
        super(id, model);
        choice = new Select2Choice<>("paramValue", model, provider);
        choice.setOutputMarkupId(true);
        choice.setMarkupId(select2UniqueIdentifier());

        // for allowClear placeholder needs to be non null. setRequired will reset it if necessary
        choice.getSettings().setPlaceholder("");
        choice.getSettings().setAllowClear(true);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(new Label("paramName", labelModel));
        add(feedback);
    }

    /**
     * Select2 javascript code needs a unique HTML id for each component. This method generates a unique id for the
     * component using the component's markup id and a random UUID.
     */
    private String select2UniqueIdentifier() {
        return "paramValue" + UUID.randomUUID().toString().replaceAll("_", "");
    }

    public static Select2ChoiceParamPanel<String> ofStrings(
            String id, IModel<String> label, IModel<String> model, List<String> options) {

        return ofStrings(id, label, model, options, SerializableFunction.identity());
    }

    public static Select2ChoiceParamPanel<String> ofStrings(
            String id,
            IModel<String> label,
            IModel<String> model,
            List<String> options,
            SerializableFunction<String, String> displayValueConverter) {

        ChoiceProvider<String> provider = new StringProvider(options, displayValueConverter);
        return new Select2ChoiceParamPanel<>(id, label, model, provider);
    }

    public static <V extends Serializable & Comparable<V>> Select2ChoiceParamPanel<V> of(
            String id,
            IModel<String> label,
            IModel<V> model,
            List<V> options,
            SerializableFunction<V, String> displayValueConverter) {

        ChoiceProvider<V> provider = new Select2ChoiceProvider<>(options, displayValueConverter);
        return new Select2ChoiceParamPanel<>(id, label, model, provider);
    }

    @Override
    public Select2Choice<T> getFormComponent() {
        return choice;
    }

    public Select2ChoiceParamPanel<T> allowCustomValues(boolean allowCustomValues) {
        choice.getSettings().setTags(allowCustomValues);
        return this;
    }

    public Select2ChoiceParamPanel<T> setPlaceHolder(T placeholder) {
        choice.getSettings().setPlaceholder(placeholder);
        return this;
    }

    public Select2ChoiceParamPanel<T> setProvider(ChoiceProvider<T> provider) {
        choice.setProvider(provider);
        return this;
    }

    public Select2ChoiceParamPanel<T> setRequired(boolean required) {
        choice.setRequired(required);
        choice.getSettings().setAllowClear(!required);
        return this;
    }

    public static class Select2ChoiceProvider<V extends Serializable & Comparable<V>> extends ChoiceProvider<V> {

        private List<V> options;
        private List<String> displayNames;
        private SerializableFunction<V, String> displayValueConverter;

        public Select2ChoiceProvider(List<V> options, SerializableFunction<V, String> displayValueConverter) {
            this.displayValueConverter = Objects.requireNonNull(displayValueConverter);
            this.options = new ArrayList<>(options);
            this.displayNames = options.stream().map(displayValueConverter).toList();
        }

        @Override
        public String getDisplayValue(V object) {
            return displayValueConverter.apply(object);
        }

        @Override
        public String getIdValue(V object) {
            return displayValueConverter.apply(object);
        }

        @Override
        public Collection<V> toChoices(Collection<String> ids) {
            return ids.stream()
                    .map(displayNames::indexOf)
                    .filter(i -> i > -1)
                    .map(options::get)
                    .toList();
        }

        @Override
        public void query(String term, int page, Response<V> response) {
            Predicate<? super String> predicate;
            if (StringUtils.hasText(term)) {
                final String ucTerm = term.toUpperCase();
                predicate = opt -> opt.toUpperCase().contains(ucTerm);
            } else {
                predicate = opt -> true;
            }
            displayNames.stream()
                    .filter(predicate)
                    .map(displayNames::indexOf)
                    .filter(index -> index > -1)
                    .map(options::get)
                    .forEach(response::add);
            response.setHasMore(false);
        }
    }

    public static class StringProvider extends StringTextChoiceProvider implements Serializable {

        private List<String> options;
        private SerializableFunction<String, String> displayValueConverter;

        public StringProvider(List<String> options, SerializableFunction<String, String> displayValueConverter) {
            this.displayValueConverter = Objects.requireNonNull(displayValueConverter);
            this.options = new ArrayList<>(options);
        }

        @Override
        public String getDisplayValue(String choice) {
            return displayValueConverter.apply(choice);
        }

        @Override
        public Collection<String> toChoices(Collection<String> ids) {
            return ids;
        }

        @Override
        public void query(String term, int page, Response<String> response) {
            Predicate<? super String> predicate;
            if (StringUtils.hasText(term)) {
                final String ucTerm = term.toUpperCase();
                predicate = opt -> opt.toUpperCase().contains(ucTerm);
            } else {
                predicate = opt -> true;
            }
            options.stream().filter(Objects::nonNull).filter(predicate).forEach(response::add);
            response.setHasMore(false);
        }
    }
}
