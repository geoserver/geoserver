/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Shows the text in a <pre> section
 */
public class PlainCodePage extends Panel {

    public PlainCodePage(String id, String initialXml) {
        super(id);
        add(new Label("code", initialXml));
    }
}
