/** */
package org.geoserver.web.wicket;

import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Styles;
import org.geotools.util.logging.Logging;

/** @author hp */
public class StyleFormatLabel extends Panel {

    static final Logger LOGGER = Logging.getLogger(StyleFormatLabel.class);

    /** serialVersionUID */
    private static final long serialVersionUID = -665729388275555894L;

    public StyleFormatLabel(String id, IModel<?> model) {

        super(id, model);
        String formatDisplayName = "";

        if (model.getObject() != null) {
            String format = (String) model.getObject();
            formatDisplayName = Styles.handler(format).getName();
        }
        Label formatLabel = new Label("styleFormatLabel", formatDisplayName);
        formatLabel.add(new AttributeModifier("title", formatDisplayName));
        add(formatLabel);
    }
}
