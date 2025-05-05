/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * Wicket behavior that patches the wicket-dnd extension to a fix an unsafe-eval CSP violation in AJAX event handling.
 * This behavior must be added to the Wicket component after adding the wicketdnd.DragSource and wicketdnd.DropTarget
 * behaviors in order to ensure that the patch JavaScript is executed after the wicket-dnd JavaScript.
 */
public class GeoServerDNDBehavior extends Behavior {

    private static final long serialVersionUID = -68544546136230017L;

    public static final JavaScriptResourceReference DND_JS =
            new JavaScriptResourceReference(GeoServerDNDBehavior.class, "gs-wicket-dnd.js");

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(DND_JS));
    }
}
