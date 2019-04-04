/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * A label + namespace dropdown form panel
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class NamespacePanel extends Panel {

    // private final DropDownChoice choice;

    private Label nsLabel;

    public NamespacePanel(
            final String componentId,
            final IModel selectedItemModel,
            final IModel paramLabelModel,
            final boolean required) {
        // make the value of the combo field the model of this panel, for easy
        // value retrieval
        super(componentId, selectedItemModel);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        nsLabel = new Label("paramValue", new PropertyModel(selectedItemModel, "URI"));
        nsLabel.setOutputMarkupId(true);
        add(nsLabel);
        /*
        // the drop down field, with a decorator for validations
        choice = new DropDownChoice("paramValue", selectedItemModel, new NamespacesModel(),
                new NamespaceChoiceRenderer());
        choice.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        choice.setLabel(paramLabelModel);
        choice.setOutputMarkupId(true);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
        */
    }

    public Component getFormComponent() {
        // return choice;
        return nsLabel;
    }
}
