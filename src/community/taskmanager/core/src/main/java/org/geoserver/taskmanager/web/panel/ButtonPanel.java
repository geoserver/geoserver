/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class ButtonPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public ButtonPanel(String id, IModel<String> model) {
        super(id);

        add(
                new AjaxButton("button", model) {
                    private static final long serialVersionUID = 3516037457693268460L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ButtonPanel.this.onSubmit(target, form);
                    }

                    @Override
                    public boolean isEnabled() {
                        return ButtonPanel.this.isEnabled();
                    }
                });
    }

    public Button getButton() {
        return (Button) get("button");
    }

    public void onSubmit(AjaxRequestTarget target, Form<?> form) {}
}
