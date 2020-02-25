/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Base class for admin panel extensions.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminPagePanel extends Panel {

    public AdminPagePanel(String id, IModel<?> model) {
        super(id, model);
    }

    public void onMainFormSubmit() {}
}
