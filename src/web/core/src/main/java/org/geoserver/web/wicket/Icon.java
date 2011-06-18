/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * A simple {@link Image} in a panel. For when you need to add an icon in a repeater without
 * breaking yet another fragment.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@SuppressWarnings("serial")
public class Icon extends Panel {

    public Icon(String id, ResourceReference resourceReference) {
        super(id);
        add(new Image("img", resourceReference));
    }
}
