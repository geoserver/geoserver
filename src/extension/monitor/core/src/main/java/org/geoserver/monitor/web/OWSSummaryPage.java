/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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

    private static final long serialVersionUID = -8012730662519508306L;

    public OWSSummaryPage() {
        List<AbstractTab> tabs = new ArrayList<AbstractTab>();
        tabs.add(
                new AbstractTab(new ResourceModel("overview")) {
                    private static final long serialVersionUID = 1037158550051655148L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new OWSOverviewPanel(panelId, getMonitor(), null);
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("wfs")) {
                    private static final long serialVersionUID = -3085421260326720801L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new OWSDetailsPanel(panelId, getMonitor(), "WFS");
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("wms")) {
                    private static final long serialVersionUID = -6494862041051243036L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new OWSDetailsPanel(panelId, getMonitor(), "WMS");
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("wcs")) {
                    private static final long serialVersionUID = 2330074592986120520L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new OWSDetailsPanel(panelId, getMonitor(), "WCS");
                    }
                });
        add(new TabbedPanel<AbstractTab>("charts", tabs));
    }
}
