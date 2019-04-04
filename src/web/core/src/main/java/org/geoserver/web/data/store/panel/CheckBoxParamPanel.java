/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A simple label + checkbox panel
 *
 * @author Gabriel Roldan
 * @todo: extend {@link FormComponentPanel} instead
 */
public class CheckBoxParamPanel extends Panel implements ParamPanel {

    private static final long serialVersionUID = -8587266542399491587L;

    private CheckBox checkBox;

    public CheckBoxParamPanel(final String id, final IModel model, final IModel paramLabelModel) {
        super(id, model);
        Label label = new Label("paramName", paramLabelModel);
        checkBox = new CheckBox("paramValue", model);
        add(label);
        add(checkBox);
    }

    public FormComponent<Boolean> getFormComponent() {
        return checkBox;
    }
}
