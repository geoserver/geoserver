/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.web.panel.attribute.AttributeDataProvider;
import org.geoserver.metadata.web.panel.attribute.AttributesTablePanel;
import org.geoserver.web.GeoServerApplication;

/**
 * The dynamically generated metadata input panel. All fields are added on the fly based on the yaml
 * configuration.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class MetadataPanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private final Map<String, List<Integer>> derivedAtts;

    private final ResourceInfo rInfo;

    private final String tab;

    public MetadataPanel(
            String id,
            IModel<ComplexMetadataMap> metadataModel,
            Map<String, List<Integer>> derivedAtts,
            ResourceInfo rInfo,
            String tab) {
        super(id, metadataModel);
        this.derivedAtts = derivedAtts;
        this.rInfo = rInfo;
        this.tab = tab;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();
        // the attributes panel
        AttributesTablePanel attributesPanel =
                new AttributesTablePanel(
                        "attributesPanel",
                        new AttributeDataProvider(rInfo, tab),
                        getMetadataModel(),
                        derivedAtts,
                        rInfo);

        attributesPanel.setOutputMarkupId(true);
        add(attributesPanel);
    }

    @SuppressWarnings("unchecked")
    public IModel<ComplexMetadataMap> getMetadataModel() {
        return (IModel<ComplexMetadataMap>) getDefaultModel();
    }

    public static Panel buildPanel(
            String id,
            IModel<ComplexMetadataMap> metadataModel,
            Map<String, List<Integer>> derivedAtts,
            ResourceInfo resource) {

        List<String> tabs =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class)
                        .getMetadataConfiguration()
                        .getTabs();

        if (!tabs.isEmpty()) {
            List<AbstractTab> tabPanels = new ArrayList<>();

            for (String tab : tabs) {
                tabPanels.add(
                        new AbstractTab(new Model<>(tab)) {
                            private static final long serialVersionUID = -6178140635455783732L;

                            @Override
                            public WebMarkupContainer getPanel(String panelId) {
                                return new MetadataPanel(
                                        panelId, metadataModel, derivedAtts, resource, tab);
                            }
                        });
            }

            TabbedPanel<AbstractTab> panel = new TabbedPanel<>(id, tabPanels);

            return panel;
        } else {
            return (Panel)
                    new MetadataPanel(id, metadataModel, derivedAtts, resource, null)
                            .setOutputMarkupId(true);
        }
    }
}
