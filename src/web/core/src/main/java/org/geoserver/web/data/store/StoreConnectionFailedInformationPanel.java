/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * A content panel for a {@link GeoServerDialog} used by {@link DataAccessEditPage} and {@link
 * CoverageStoreEditPage} to ask for confirmation on saving a store that can't be connected to
 * through it's connection parameters
 *
 * @version $Id$
 */
public class StoreConnectionFailedInformationPanel extends Panel {

    private static final long serialVersionUID = -4118332716894663905L;

    public StoreConnectionFailedInformationPanel(
            final String componentId, final String storeName, final String exceptionMessage) {
        super(componentId);
        // add(new Label("title", new ResourceModel("storeConnectionFailed")));

        ParamResourceModel bodyModel = new ParamResourceModel("body", this, storeName);
        add(new Label("body", bodyModel));

        // add(new Label("exceptionTitle", new ResourceModel("exceptionTitle")));
        add(new Label("exceptionMessage", exceptionMessage));
    }
}
