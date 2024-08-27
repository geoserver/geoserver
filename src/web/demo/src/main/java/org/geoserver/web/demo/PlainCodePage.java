/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.wicket.GSModalDialog;
import org.geoserver.web.wicket.GSModalWindow;

/**
 * Shows the text in a <pre> section
 */
public class PlainCodePage extends Panel {
    String code;

    public WebComponent getContent() {
        return content;
    }

    WebComponent content;

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
    }

    public PlainCodePage(
            final GSModalWindow container, final GSModalDialog responseWindow, String initialXml) {
        super(responseWindow.getContentId());
        this.code = initialXml;

        content = new Label("code", new PropertyModel(this, "code"));
        add(content);
    }
}
