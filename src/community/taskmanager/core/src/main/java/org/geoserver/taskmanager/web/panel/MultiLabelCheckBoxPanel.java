/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class MultiLabelCheckBoxPanel extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(MultiLabelCheckBoxPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = 3765410954951956161L;

    public MultiLabelCheckBoxPanel(
            String id,
            String labelContent,
            String checkboxContent,
            IModel<Boolean> selectionModel,
            boolean selectionVisible) {
        super(id);
        Form<?> form = new Form<>("form");
        add(form);
        form.add(new MultiLineLabel("multilabel", labelContent).setEscapeModelStrings(false));
        form.add(new CheckBox("checkbox", selectionModel).setVisible(selectionVisible));
        form.add(new Label("checkboxLabel", checkboxContent).setVisible(selectionVisible));
    }
}
