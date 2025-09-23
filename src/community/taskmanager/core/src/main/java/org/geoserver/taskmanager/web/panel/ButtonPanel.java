/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.io.Serial;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

// TODO WICKET8 - Verify this page works OK
public class ButtonPanel extends Panel {

    @Serial
    private static final long serialVersionUID = -1829729746678003578L;

    public ButtonPanel(String id, IModel<String> model) {
        super(id);

        add(new AjaxButton("button", model) {
            @Serial
            private static final long serialVersionUID = 3516037457693268460L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                ButtonPanel.this.onSubmit(target);
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

    public void onSubmit(AjaxRequestTarget target) {}
}
