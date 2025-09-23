/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serial;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class CheckBoxPanel extends Panel {

    @Serial
    private static final long serialVersionUID = -1829729746678003578L;

    public CheckBoxPanel(String id, IModel<Boolean> model) {

        super(id, model);

        add(new CheckBox("checkBox", model));
    }
}
