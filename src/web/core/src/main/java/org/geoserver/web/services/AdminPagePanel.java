/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
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
 *
 */
public class AdminPagePanel extends Panel {

    public AdminPagePanel(String id, IModel<?> model) {
        super(id, model);
    }

}
