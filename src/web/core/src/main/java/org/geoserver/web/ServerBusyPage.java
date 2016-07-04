/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Displays a message suggesting the user to login or to elevate his privileges
 */
public class ServerBusyPage extends GeoServerBasePage {

    public ServerBusyPage() {
        IModel model = new ResourceModel( "ServerBusyPage.serverBusyMessage" );
        add(new Label("serverBusyMessage", model));
    }
}
