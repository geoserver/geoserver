/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.util.Collection;
import java.util.Iterator;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class AutoCompleteTextFieldPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public AutoCompleteTextFieldPanel(String id, IModel<String> model, Collection<String> orig) {
        super(id, model);

        add(
                new AutoCompleteTextField<String>("textfield", model) {

                    private static final long serialVersionUID = 3534191360864988131L;

                    @Override
                    protected Iterator<String> getChoices(String input) {
                        return orig.stream().filter(s -> s.startsWith(input)).iterator();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public AutoCompleteTextField<String> getTextField() {
        return (AutoCompleteTextField<String>) get("textfield");
    }
}
