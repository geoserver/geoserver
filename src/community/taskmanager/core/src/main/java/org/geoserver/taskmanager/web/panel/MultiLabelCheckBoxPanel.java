/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class MultiLabelCheckBoxPanel extends Panel {

    private static final long serialVersionUID = 3765410954951956161L;

    public MultiLabelCheckBoxPanel(
            String id,
            String labelContent,
            String checkboxContent,
            IModel<Boolean> selectionModel) {
        super(id);
        Form<?> form = new Form<>("form");
        add(form);
        form.add(new MultiLineLabel("multilabel", labelContent).setEscapeModelStrings(false));
        form.add(new CheckBox("checkbox", selectionModel));
        form.add(new Label("checkboxLabel", checkboxContent));
    }
}
