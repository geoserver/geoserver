/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.CoverageViewEditPage;
import org.geoserver.web.data.store.panel.ColorPickerPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;

/**
 * A configuration panel for CoverageInfo properties that related to WCS publication
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class CoverageResourceConfigurationPanel extends ResourceConfigurationPanel {

    public CoverageResourceConfigurationPanel(final String panelId, final IModel model) {
        super(panelId, model);

        final CoverageInfo coverage = (CoverageInfo) getResourceInfo();

        final Map<String, Serializable> parameters = coverage.getParameters();
        List<String> keys = new ArrayList<String>(parameters.keySet());
        Collections.sort(keys);

        final IModel paramsModel = new PropertyModel(model, "parameters");
        ListView paramsList =
                new ListView("parameters", keys) {

                    @Override
                    protected void populateItem(ListItem item) {
                        Component inputComponent =
                                getInputComponent(
                                        "parameterPanel",
                                        paramsModel,
                                        item.getDefaultModelObjectAsString());
                        item.add(inputComponent);
                    }
                };

        WebMarkupContainer coverageViewContainer =
                new WebMarkupContainer("editCoverageViewContainer");
        add(coverageViewContainer);
        final CoverageView coverageView =
                coverage.getMetadata().get(CoverageView.COVERAGE_VIEW, CoverageView.class);
        coverageViewContainer.add(
                new Link("editCoverageView") {

                    @Override
                    public void onClick() {
                        CoverageInfo coverageInfo = (CoverageInfo) model.getObject();
                        try {
                            CoverageStoreInfo store = coverageInfo.getStore();
                            WorkspaceInfo workspace = store.getWorkspace();
                            setResponsePage(
                                    new CoverageViewEditPage(
                                            workspace.getName(),
                                            store.getName(),
                                            coverageInfo.getName(),
                                            coverageInfo,
                                            ((ResourceConfigurationPage) this.getPage())));
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failure opening the Virtual Coverage edit page",
                                    e);
                            error(e.toString());
                        }
                    }
                });

        coverageViewContainer.setVisible(coverageView != null);

        // needed for form components not to loose state
        paramsList.setReuseItems(true);
        add(paramsList);

        if (keys.size() == 0) setVisible(false);
    }

    private Component getInputComponent(String id, IModel paramsModel, String keyName) {
        if (keyName.contains("Color"))
            return new ColorPickerPanel(
                    id,
                    new MapModel(paramsModel, keyName),
                    new org.apache.wicket.model.ResourceModel(keyName, keyName),
                    false);
        else
            return new TextParamPanel(
                    id,
                    new MapModel(paramsModel, keyName),
                    new org.apache.wicket.model.ResourceModel(keyName, keyName),
                    false);
    }
}
