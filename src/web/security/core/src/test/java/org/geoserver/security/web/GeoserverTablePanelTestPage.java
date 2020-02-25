/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.io.Serializable;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Helper class to test {@link GeoServerTablePanel} Since the concrete page gets a random id, the id
 * is available using {@link #getComoponentId()} for building wicket path expressions
 *
 * <p>The panel will be placed into a form named "form"
 */
public class GeoserverTablePanelTestPage extends WebPage {

    public static final String TABLE = "table";
    public static final String FORM = "form";

    private String componentId;

    public String getComponentId() {
        return componentId;
    }

    public String getWicketPath() {
        return FORM + ":" + getComponentId();
    }

    public GeoserverTablePanelTestPage(ComponentBuilder builder) {
        Form<Serializable> form = new Form<Serializable>(FORM);
        Component c = builder.buildComponent(TABLE);
        componentId = c.getId();
        form.add(c);
        add(form);
    }
}
