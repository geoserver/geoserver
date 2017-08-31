package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SystemInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.SystemPanelInfo;


public abstract class AbstractStatusPage extends ServerAdminPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -6228795354577370186L;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    protected FeedbackPanel feedbackPanel;
    
    public AbstractStatusPage() {
        this.feedbackPanel = super.feedbackPanel;
        initUI();
    }
    
    protected void initUI() {

        List<ITab> tabs = new ArrayList<ITab>();

        PanelCachingTab statusTab = new PanelCachingTab(
                new AbstractTab(new Model<String>("Status")) {
                    private static final long serialVersionUID = 9062803783143908814L;

                    public Panel getPanel(String id) {
                        return new StatusPanel(id, AbstractStatusPage.this);
                    }
                });
        tabs.add(statusTab);

        PanelCachingTab moduleStatusTab = new PanelCachingTab(
                new AbstractTab(new Model<String>("Modules")) {
                    private static final long serialVersionUID = -5301288750339244612L;
                    public Panel getPanel(String id) {
                        return new ModuleStatusPanel(id, AbstractStatusPage.this);
                    }
                });
        tabs.add(moduleStatusTab);

        // get the list of SystemStatus extensions
        List<SystemPanelInfo<SystemInfo>> systemStatuss = GeoServerExtensions
                .extensions(SystemPanelInfo.class).stream().collect(Collectors.toList());
        Collections.sort(systemStatuss, (p1, p2) -> p1.getOrder().compareTo(p2.getOrder()));
        systemStatuss.forEach(item -> {
            PanelCachingTab systemStatusTab = new PanelCachingTab(
                    new AbstractTab(new StringResourceModel(item.getTitleKey(), (Component) null, null)) {
                        private static final long serialVersionUID = 8062803711143338819L;

                        public Panel getPanel(String id) {
                            Panel panel;
                            try {
                                panel = item.getComponentClass().getConstructor(String.class)
                                        .newInstance(id);
                            } catch (Exception e) {
                                throw new WicketRuntimeException(
                                        "Failed to add pluggable system info configuration panels",
                                        e);
                            }
                            return panel;
                        }
                    });
            tabs.add(systemStatusTab);
        });

        add(new AjaxTabbedPanel<ITab>("tabs", tabs));
    }
    //Make sure child tabs can see this
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

}
