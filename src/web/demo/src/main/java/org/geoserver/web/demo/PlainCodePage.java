/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.wicket.GSModalWindow;

/**
 * Shows the text in a <pre> section
 */
public class PlainCodePage extends WebPage {
    String code;

    public PlainCodePage(
            final GSModalWindow container, final GSModalWindow responseWindow, String initialXml) {
        this.code = initialXml;

        add(new Label("code", new PropertyModel<>(this, "code")));
    }
}
