/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.data.layergroup.LayerGroupProviderFilter;
import org.geoserver.web.data.store.panel.ParamPanel;


/**
 * A label + layer group dropdown form panel
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
@SuppressWarnings("serial")
public class EoLayerGroupPanel extends Panel implements ParamPanel {

    protected DropDownChoice<LayerGroupInfo> choice;
    
    
    public EoLayerGroupPanel(final String id, final IModel<LayerGroupInfo> layerGroupModel,
            final IModel<String> paramLabelModel, final boolean required,
            LayerGroupProviderFilter filter) {
        // make the value of the combo field the model of this panel, for easy
        // value retriaval
        super(id, layerGroupModel);

        // the label
        String requiredMark = required ? " *" : ""; 
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the drop down field, with a decorator for validations
        choice = new DropDownChoice<LayerGroupInfo>("paramValue", layerGroupModel, 
                new LayerGroupsModel(filter), new LayerGroupInfoChoiceRenderer());
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
    public FormComponent<LayerGroupInfo> getFormComponent() {
        return choice;
    }
}