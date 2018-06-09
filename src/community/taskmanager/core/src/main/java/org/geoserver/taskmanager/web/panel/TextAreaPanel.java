/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class TextAreaPanel extends Panel {

    private static final long serialVersionUID = -1821529746678003578L;

    public TextAreaPanel(String id, IModel<String> model) {
        super(id, model);

        add(new TextArea<>("textarea", model));
    }

    @SuppressWarnings("unchecked")
    public TextArea<String> getTextArea() {
        return (TextArea<String>) get("textarea");
    }
}
