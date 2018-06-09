/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/** A label + locale dropdown form panel */
@SuppressWarnings("serial")
public class CharsetPanel extends Panel implements ParamPanel {

    private DropDownChoice<String> choice;

    public CharsetPanel(
            final String id,
            final IModel<String> charsetModel,
            final IModel<String> paramLabelModel,
            final boolean required) {
        // make the value of the combo field the model of this panel, for easy
        // value retriaval
        super(id, charsetModel);

        // the label
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the drop down field, with a decorator for validations
        final ArrayList<String> charsets =
                new ArrayList<String>(Charset.availableCharsets().keySet());
        choice = new DropDownChoice<String>("paramValue", charsetModel, charsets);
        choice.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        choice.setLabel(paramLabelModel);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
    }

    /**
     * Returns the form component used in the panel in case it is needed for related form components
     * validation
     */
    public FormComponent<String> getFormComponent() {
        return choice;
    }
}
