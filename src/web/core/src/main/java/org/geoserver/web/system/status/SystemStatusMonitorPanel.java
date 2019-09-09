/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import org.apache.wicket.markup.html.panel.Panel;

/**
 * Container for system status refreshable values, this isolates the periodicity refreshed panel
 * from the rest of the page components, this will make the auto-refresh stop if the refreshed panel
 * is hidden, e.g. when a new tab is selected.
 */
public class SystemStatusMonitorPanel extends Panel {

    private static final long serialVersionUID = -561663546856772557L;

    public SystemStatusMonitorPanel(String id) {
        super(id);
        // adds the refreshable panel that will contain the system monitoring values
        add(new RefreshedPanel("refreshed-values"));
    }
}
