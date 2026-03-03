/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serial;
import java.util.Date;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.DateField;

public class DateTimeFieldPanel extends Panel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        //if the panel-specific CSS file contains actual css then have the browser load the css 
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = -1829729746678003578L;

    public DateTimeFieldPanel(String id, IModel<Date> model, boolean time) {
        super(id, model);
        DateField dateTimeField = new DateField("dateTimeField", model, time);
        dateTimeField.setModel(model);
        add(dateTimeField);
    }
}
