/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.data.store.panel.ParamPanel;

/**
 * A DataStore parameter panel that presents a simple dropdown choice. Created to avoid use of
 * DropDownChoiceParamPanel since it contains a Select2DropDownChoice which have a bug when multiple
 * dropdowns are rendered on same panel.
 */
public class SimpleDropDownChoiceParamPanel extends Panel implements ParamPanel {

    private static final long serialVersionUID = 1L;

    private DropDownChoice<Serializable> choice;

    /**
     * @param id panel id
     * @param paramValue model for the component's value
     * @param paramLabelModel model for the parameter name label
     * @param options drop down choices
     * @param required true if a value is required, false otherwise
     */
    public SimpleDropDownChoiceParamPanel(
            final String id,
            final IModel<Serializable> paramValue,
            final IModel<String> paramLabelModel,
            final List<? extends Serializable> options,
            final boolean required) {

        super(id, paramValue);

        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        choice = new DropDownChoice<>("paramValue", paramValue, options);
        choice.setRequired(required);
        if (!required) {
            choice.setNullValid(true);
        }

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
    }

    @Override
    public DropDownChoice<Serializable> getFormComponent() {
        return choice;
    }
}
