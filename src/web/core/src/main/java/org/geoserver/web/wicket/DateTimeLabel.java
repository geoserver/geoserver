/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geotools.util.logging.Logging;

/**
 * @author ImranR
 *     <p>A Generic Panel to render Time stamp inside Data Provider (Table)
 */
public class DateTimeLabel extends Panel {

    static final Logger LOGGER = Logging.getLogger(DateTimeLabel.class);

    /** serialVersionUID */
    private static final long serialVersionUID = -665729388275555894L;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public DateTimeLabel(String id, IModel<?> model) {
        super(id, model);

        String formattedDateString = "";
        String formattedTimeString = "";
        // null check
        if (model.getObject() != null) {
            Object val = model.getObject();
            // type check
            if (val instanceof Date) {
                Date date = (Date) val;
                formattedDateString = dateFormat.format(date);
                formattedTimeString = timeFormat.format(date);
            } else
                LOGGER.severe(
                        "expected instance of java.util.Date as Model object in DateTimeLabel");
        }
        Label dateLabel = new Label("dateTimelabel", formattedDateString);
        dateLabel.add(new AttributeModifier("title", formattedTimeString));
        add(dateLabel);
    }
}
