/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import java.io.Serial;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Container for system status refreshable values, this isolates the periodicity refreshed panel from the rest of the
 * page components, this will make the auto-refresh stop if the refreshed panel is hidden, e.g. when a new tab is
 * selected.
 */
public class SystemStatusMonitorPanel extends Panel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        //if the panel-specific CSS file contains actual css then have the browser load the css 
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = -561663546856772557L;

    public SystemStatusMonitorPanel(String id) {
        super(id);
        // adds the refreshable panel that will contain the system monitoring values
        add(new RefreshedPanel("refreshed-values"));
    }
}
