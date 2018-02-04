/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.Settings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter to quickly migrate a Wicket {@link org.apache.wicket.markup.html.form.DropDownChoice}
 * to an auto-completable {@link Select2Choice}
 */
public class Select2DropDownChoice<T> extends Select2Choice<T> {

    public static final int MAX_WIDTH = 100;
    private final IModel<List<T>> choicesModel;
    private final IChoiceRenderer<T> renderer;

    /**
     * Utility class used to steal the internationalized "choose one" string from Wicket
     */
    static class DropDownChoiceEmptySelectProvider extends DropDownChoice {

        public DropDownChoiceEmptySelectProvider(String id) {
            super(id);
        }

        @Override
        public String getNullKeyDisplayValue() {
            return super.getNullKeyDisplayValue();
        }
    }
    
    public Select2DropDownChoice(String id, IModel<T> model, IModel<List<T>> choices, IChoiceRenderer<T> renderer) {
        super(id, model, new DropDownChoiceProvider<>(choices, renderer));
        this.choicesModel = choices;
        this.renderer = renderer;
        Settings settings = getSettings();
        settings.setCloseOnSelect(true);
        String placeHolder = getPlaceholderFromWicket();
        settings.setPlaceholder(placeHolder);
        // attempt to auto-size the drop down, few chars are one em large, this formula seems to work (empirically)
        // still giving the dropdown more width than strictly required
        int maxChoiceLength = getMaxChoiceLength(choices, renderer) * 2 / 3;
        int length = Math.max(placeHolder.length() + 2, Math.min(MAX_WIDTH, maxChoiceLength + 2));
        settings.setWidth(length + "em");
        settings.setDropdownAutoWidth(true);
    }

    private int getMaxChoiceLength(IModel<List<T>> choices, IChoiceRenderer<T> renderer) {
        Optional<Integer> max = choices.getObject().stream().map(choice -> {
            Object display = renderer.getDisplayValue(choice);
            if (display instanceof CharSequence) {
                return ((CharSequence) display).length();
            } else {
                return 0;
            }
        }).max(Integer::compare);
        
        return max.orElse(0);
    }

    private String getPlaceholderFromWicket() {
        return new DropDownChoiceEmptySelectProvider("fooBar").getNullKeyDisplayValue();
    }

    public IModel<List<T>> getChoicesModel() {
        return choicesModel;
    }

    public IChoiceRenderer<T> getChoiceRenderer() {
        return renderer;
    }

    @Override
    public boolean isInputNullable() {
        return false;
    }
    
    

    @Override
    public String getModelValue()
    {
        final T object = getModelObject();
        if (object != null)
        {
            int index = getChoices().indexOf(object);
            return getChoiceRenderer().getIdValue(object, index);
        }
        else
        {
            return "";
        }
    }

    /**
     * @return The collection of object that this choice has
     */
    public List<T> getChoices()
    {
        List<T> choices = (this.choicesModel != null) ? this.choicesModel.getObject() : null;
        if (choices == null)
        {
            throw new NullPointerException(
                    "List of choices is null - Was the supplied 'Choices' model empty?");
        }
        return choices;
    }


    /**
     * Simple {@link ChoiceProvider} bridging the model/renderer DropDownChoice approach to Select2 components
     * @param <T>
     */
    private static class DropDownChoiceProvider<T> extends ChoiceProvider<T> {
        private final IChoiceRenderer<T> renderer;
        private final IModel<List<T>> choices;

        DropDownChoiceProvider(IModel<List<T>> choices, IChoiceRenderer<T> renderer) {
            this.renderer = renderer;
            this.choices = choices;
        }

        @Override
        public String getDisplayValue(T t) {
            return String.valueOf(renderer.getDisplayValue(t));
        }

        @Override
        public String getIdValue(T t) {
            return renderer.getIdValue(t, 0);
        }

        @Override
        public void query(String s, int i, Response<T> response) {
            choices.getObject().stream().filter(o -> {
                Object display = renderer.getDisplayValue(o);
                return s == null || (display != null && String.valueOf(display).toLowerCase().contains(s.toLowerCase()));
            }).forEach(response::add);
        }

        @Override
        public Collection<T> toChoices(Collection<String> collection) {
            List<? extends T> objects = choices.getObject();
            return objects.stream()
                    .filter(o -> collection.contains(renderer.getIdValue(o, 0)))
                    .collect(Collectors.toList());
        }
    }
}
