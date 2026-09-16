/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips.web;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.admin.StatusPage;

/** Contributes the FIPS tab to the server status page. */
public class FipsStatusTabDefinition implements StatusPage.TabDefinition {

    @Override
    public String getTitleKey() {
        return "FipsStatusPanel.title";
    }

    @Override
    public Panel createPanel(String panelId, Page containerPage) {
        return new FipsStatusPanel(panelId);
    }
}
