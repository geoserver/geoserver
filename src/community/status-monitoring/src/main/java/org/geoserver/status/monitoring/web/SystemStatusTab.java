/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.web;

import java.io.Serializable;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.admin.AbstractStatusPage;

/** Define the system monitoring tab that will appear on GeoServer status page. */
public final class SystemStatusTab implements AbstractStatusPage.TabDefinition, Serializable {

    private static final long serialVersionUID = 1095048632943920726L;

    @Override
    public String getTitleKey() {
        return "MonitoringPanel.title";
    }

    @Override
    public Panel createPanel(String panelId, Page containerPage) {
        return new SystemStatusMonitorPanel(panelId);
    }
}
