/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.geoserver.web.data.store.panel.ParamPanel;

@SuppressWarnings("serial")
public class RadioGroupParamPanel<T extends Serializable> extends Panel implements ParamPanel<T> {

    private RadioGroup<T> group;

    public RadioGroupParamPanel(String id, IModel<String> label, IModel<T> model, List<T> choices) {
        this(id, label, model, choices, opt -> null);
    }

    public RadioGroupParamPanel(
            String id,
            IModel<String> label,
            IModel<T> model,
            List<T> choices,
            SerializableFunction<T, IModel<String>> choiceLabels) {
        super(id, model);

        group = new RadioGroup<>("group", model);
        group.add(new DynamicRadioChoices<>("choices", choices, choiceLabels));
        add(new Label("paramName", label));
        add(group);
    }

    @Override
    public RadioGroup<T> getFormComponent() {
        return group;
    }

    /** ListView to dynamically generate the radios */
    private static class DynamicRadioChoices<I> extends ListView<I> {

        private SerializableFunction<I, IModel<String>> choiceLabels;

        DynamicRadioChoices(String id, List<I> choices, SerializableFunction<I, IModel<String>> choiceLabels) {
            super(id, choices);
            this.choiceLabels = choiceLabels;
        }

        @Override
        protected void populateItem(ListItem<I> item) {
            // Add a Radio component to the group, using the item's model object as the
            // value
            item.add(new Radio<>("paramValue", item.getModel()));
            // Add a Label for the radio button
            IModel<String> labelModel = labelModel(item.getModelObject());
            item.add(new Label("label", labelModel));
        }

        private IModel<String> labelModel(I modelObject) {
            IModel<String> labelModel = choiceLabels.apply(modelObject);
            if (labelModel == null) {
                labelModel = new Model<>(String.valueOf(modelObject));
            }
            return labelModel;
        }
    }
}
