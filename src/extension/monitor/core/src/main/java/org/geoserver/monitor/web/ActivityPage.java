/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class ActivityPage extends MonitorBasePage {

    @Serial
    private static final long serialVersionUID = 4172665268503474405L;

    public ActivityPage() {
        List<AbstractTab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(new ResourceModel("live")) {
            @Serial
            private static final long serialVersionUID = 4764386249807182104L;

            @Override
            public Panel getPanel(String panelId) {
                return new LiveActivityPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("daily")) {
            @Serial
            private static final long serialVersionUID = 9173511149822486084L;

            @Override
            public Panel getPanel(String panelId) {
                return new DailyActivityPanel(panelId, getMonitor());
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("weekly")) {
            @Serial
            private static final long serialVersionUID = -7578737647862625538L;

            @Override
            public Panel getPanel(String panelId) {
                return new WeeklyActivityPanel(panelId, getMonitor());
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("monthly")) {
            @Serial
            private static final long serialVersionUID = -5620008935388738857L;

            @Override
            public Panel getPanel(String panelId) {
                return new MonthlyActivityPanel(panelId, getMonitor());
            }
        });
        add(new TabbedPanel<>("charts", tabs));
    }
}
