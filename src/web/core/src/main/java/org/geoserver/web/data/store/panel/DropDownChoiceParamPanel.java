/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.Select2DropDownChoice;

/** A DataStore parameter panel that presents a dropdown choice */
public class DropDownChoiceParamPanel extends Panel implements ParamPanel<Serializable> {

    private static final long serialVersionUID = 1L;

    private DropDownChoice<Serializable> choice;

    /**
     * @param id panel id
     * @param paramValue model for the component's value
     * @param paramLabelModel model for the parameter name label
     * @param options drop down choices
     * @param required true if a value is required, false otherwise
     */
    public DropDownChoiceParamPanel(
            final String id,
            final IModel<Serializable> paramValue,
            final IModel<String> paramLabelModel,
            final List<? extends Serializable> options,
            final boolean required) {

        super(id, paramValue);

        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        choice = new Select2DropDownChoice<>("paramValue", paramValue, options);
        choice.setOutputMarkupId(true);
        choice.setMarkupId(select2UniqueIdentifier());
        choice.setRequired(required);
        if (!required) {
            choice.setNullValid(true);
        }

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
    }

    /**
     * Select2 javascript code needs a unique HTML id for each component. This method generates a
     * unique id for the component using the component's markup id and a random UUID.
     */
    private String select2UniqueIdentifier() {
        UUID randomUUID = UUID.randomUUID();
        return "paramValue" + randomUUID.toString().replaceAll("_", "");
    }

    @Override
    public DropDownChoice<Serializable> getFormComponent() {
        return choice;
    }
}
