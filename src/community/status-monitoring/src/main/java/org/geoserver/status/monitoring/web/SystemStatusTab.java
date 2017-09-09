/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.web;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.admin.AbstractStatusPage;

/**
 * Define the system monitoring tab that will appear on GeoServer status page.
 */
public final class SystemStatusTab implements AbstractStatusPage.TabDefinition {

    @Override
    public String getTitleKey() {
        return "MonitoringPanel.title";
    }

    @Override
    public Panel createPanel(String panelId, Page containerPage) {
        return new SystemStatusMonitorPanel(panelId);
    }
}
