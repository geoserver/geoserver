/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.taskmanager.web.panel.bulk.BulkImportPanel;
import org.geoserver.taskmanager.web.panel.bulk.BulkInitPanel;
import org.geoserver.taskmanager.web.panel.bulk.BulkRunPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;

public class BulkOperationsPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -3476820703264158330L;

    @Override
    public void onInitialize() {
        super.onInitialize();

        Form<Object> form = new Form<Object>("form");
        List<ITab> tabs = new ArrayList<>();
        tabs.add(
                new AbstractTab(new ResourceModel("bulkRun")) {
                    private static final long serialVersionUID = 4375160438369461475L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new BulkRunPanel(panelId);
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("bulkImport")) {
                    private static final long serialVersionUID = 4375160438369461475L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new BulkImportPanel(panelId);
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("bulkInitialize")) {
                    private static final long serialVersionUID = 4375160438369461475L;

                    @Override
                    public Panel getPanel(String panelId) {
                        return new BulkInitPanel(panelId);
                    }
                });
        form.add(new TabbedPanel<ITab>("tabs", tabs));

        add(form);
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.ADMIN;
    }
}
