/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class OWSSummaryPage extends MonitorBasePage {

    public OWSSummaryPage() {
        List tabs = new ArrayList();
        tabs.add(new AbstractTab(new ResourceModel("overview")) {
            @Override
            public Panel getPanel(String panelId) {
                return new OWSOverviewPanel(panelId, getMonitor(), null);
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("wfs")) {
            @Override
            public Panel getPanel(String panelId) {
                return new OWSDetailsPanel(panelId, getMonitor(), "WFS");
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("wms")) {
            @Override
            public Panel getPanel(String panelId) {
                return new OWSDetailsPanel(panelId, getMonitor(), "WMS");
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("wcs")) {
            @Override
            public Panel getPanel(String panelId) {
                return new OWSDetailsPanel(panelId, getMonitor(), "WCS");
            }
        });
        add(new TabbedPanel("charts", tabs));
        
    }
}
