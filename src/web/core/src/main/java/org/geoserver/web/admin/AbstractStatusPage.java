/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.system.status.SystemStatusMonitorPanel;

public abstract class AbstractStatusPage extends ServerAdminPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -6228795354577370186L;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    public AbstractStatusPage() {
        initUI();
    }

    protected void initUI() {

        List<ITab> tabs = new ArrayList<ITab>();

        PanelCachingTab statusTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<String>("Status")) {
                            private static final long serialVersionUID = 9062803783143908814L;

                            public Panel getPanel(String id) {
                                return new StatusPanel(id, AbstractStatusPage.this);
                            }
                        });
        PanelCachingTab moduleStatusTab =
                new PanelCachingTab(
                        new AbstractTab(new Model<String>("Modules")) {
                            private static final long serialVersionUID = -5301288750339244612L;

                            public Panel getPanel(String id) {
                                return new ModuleStatusPanel(id, AbstractStatusPage.this);
                            }
                        });
        PanelCachingTab systemStatusTab =
                new PanelCachingTab(
                        new AbstractTab(new StringResourceModel("MonitoringPanel.title")) {
                            private static final long serialVersionUID = -5301288750339244612L;

                            public Panel getPanel(String id) {
                                return new SystemStatusMonitorPanel(id);
                            }
                        });

        tabs.add(statusTab);
        tabs.add(moduleStatusTab);
        tabs.add(systemStatusTab);

        // extension point for adding extra tabs that will be ordered using the extension priority
        GeoServerExtensions.extensions(StatusPage.TabDefinition.class)
                .forEach(
                        tabDefinition -> {
                            // create the new extra panel using the tab definition title
                            String title =
                                    new ResourceModel(tabDefinition.getTitleKey()).getObject();
                            PanelCachingTab tab =
                                    new PanelCachingTab(
                                            new AbstractTab(new Model<>(title)) {
                                                private static final long serialVersionUID =
                                                        -5301288750339244612L;
                                                // create the extra tab panel passing down the
                                                // container id
                                                public Panel getPanel(String panelId) {
                                                    return tabDefinition.createPanel(
                                                            panelId, AbstractStatusPage.this);
                                                }
                                            });
                            tabs.add(tab);
                        });
        AjaxTabbedPanel tabbedPanel = new AjaxTabbedPanel<>("tabs", tabs);
        tabbedPanel
                .get("panel")
                .add(
                        new Behavior() {

                            @Override
                            public boolean getStatelessHint(Component component) {
                                // this will force canCallListenerInterfaceAfterExpiry to be false
                                // when a pending Ajax request
                                // is processed for expired tabs, we can't predict the Ajax events
                                // that will be used
                                return false;
                            }
                        });
        add(tabbedPanel);
    }
    // Make sure child tabs can see this
    @Override
    protected boolean isAuthenticatedAsAdmin() {
        return super.isAuthenticatedAsAdmin();
    }

    @Override
    protected Catalog getCatalog() {
        return super.getCatalog();
    }

    @Override
    protected GeoServerApplication getGeoServerApplication() {
        return super.getGeoServerApplication();
    }

    @Override
    protected GeoServer getGeoServer() {
        return super.getGeoServerApplication().getGeoServer();
    }

    /**
     * Extensions that implement this interface will be able to contribute a new tabs to GeoServer
     * status page, interface {@link org.geoserver.platform.ExtensionPriority} should be used to
     * define the tab priority.
     */
    public interface TabDefinition {

        // title of the tab
        String getTitleKey();

        // content of the tab, the created panel should use the provided id
        Panel createPanel(String panelId, Page containerPage);
    }
}
