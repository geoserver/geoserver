/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class ActivityPage extends MonitorBasePage {

    public ActivityPage() {
        List tabs = new ArrayList();
        tabs.add(new AbstractTab(new ResourceModel("live")) {
            @Override
            public Panel getPanel(String panelId) {
                return new LiveActivityPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("daily")) {
            @Override
            public Panel getPanel(String panelId) {
                return new DailyActivityPanel(panelId, getMonitor());
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("weekly")) {
            @Override
            public Panel getPanel(String panelId) {
                return new WeeklyActivityPanel(panelId, getMonitor());
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("monthly")) {
            @Override
            public Panel getPanel(String panelId) {
                return new MonthlyActivityPanel(panelId, getMonitor());
            }
        });
        add(new TabbedPanel("charts", tabs));
        
    }
}
