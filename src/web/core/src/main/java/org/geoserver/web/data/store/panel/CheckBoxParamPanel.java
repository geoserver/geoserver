/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A simple label + checkbox panel
 * 
 * @author Gabriel Roldan
 */
public class CheckBoxParamPanel extends Panel {

    private static final long serialVersionUID = -8587266542399491587L;

    public CheckBoxParamPanel(final String id, final IModel model, final IModel paramLabelModel) {
        super(id, model);
        Label label = new Label("paramName", paramLabelModel);
        CheckBox checkBox = new CheckBox("paramValue", model);
        add(label);
        add(checkBox);
    }
}
