/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerTablePanel;
import static org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class SummaryTable extends GeoServerTablePanel<Summary> {
    public SummaryTable(String id, SummaryProvider summary) {
        super(id, summary);
    }

    @Override 
    protected Component getComponentForProperty(String id, IModel value, Property<Summary> property) {
        String text = property.getPropertyValue((Summary)value.getObject()).toString();
        return new Label(id, text);
    }
}
