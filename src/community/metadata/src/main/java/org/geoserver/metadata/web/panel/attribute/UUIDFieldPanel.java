/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.UUID;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class UUIDFieldPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public UUIDFieldPanel(String id, IModel<String> model) {
        super(id, model);

        TextField<String> textfield = new TextField<String>("textfield", model);
        textfield.setOutputMarkupId(true);
        add(textfield);

        add(
                new AjaxLink<Object>("generateUUID") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        UUID uuid = UUID.randomUUID();
                        model.setObject(uuid.toString());
                        target.add(textfield);
                    }
                });
    }
}
