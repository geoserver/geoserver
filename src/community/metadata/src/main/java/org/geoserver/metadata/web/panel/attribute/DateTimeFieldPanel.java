/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.Date;
import org.apache.wicket.extensions.yui.calendar.DateField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class DateTimeFieldPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public DateTimeFieldPanel(String id, IModel<Date> model, boolean time) {
        super(id, model);
        DateTimeField dateTimeField =
                time
                        ? new DateTimeField("dateTimeField", model)
                        : new DateField("dateTimeField", model);
        dateTimeField.setModel(model);
        add(dateTimeField);
    }
}
