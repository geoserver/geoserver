/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.resource.Paths;
import org.geoserver.template.editor.constants.GeoServerConstants;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geotools.feature.NameImpl;
import org.geotools.util.SuppressFBWarnings;

public class LayerTemplateEditorPage extends AbstractTemplateEditorPage {
    private String workspaceName;

    private String storeName = "";

    private String layerName = "";

    public LayerTemplateEditorPage() {
        super();
    }

    public LayerTemplateEditorPage(PageParameters parameters) {
        super(parameters);
        this.resourceType = "Layer";
    }

    @Override
    @SuppressFBWarnings("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR")
    protected void initComponents() {
        LayerInfo layer = getLayer(layerName, workspaceName);

        ResourceInfo resource = null;
        if (layer != null) {
            this.resourceType = layer.getType().toString().toLowerCase() + " layer";
            resource = layer.getResource();
        }

        RepeatingView attributesList = new RepeatingView("attributesList");
        if (resource instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) resource;
            Iterator<AttributeTypeInfo> it;
            try {
                it = featureTypeInfo.attributes().iterator();
                while (it.hasNext()) {
                    AttributeTypeInfo info = it.next();
                    attributesList.add(new Label(attributesList.newChildId(), info.getName()));
                }
            } catch (IOException e) {
                LOGGER.warning("Error retrieving layer attributes \n" + e);
            }

        } else if (resource instanceof CoverageInfo) {
            CoverageInfo coverageInfo = (CoverageInfo) resource;
            Iterator<CoverageDimensionInfo> it = coverageInfo.getDimensions().iterator();
            while (it.hasNext()) {
                CoverageDimensionInfo info = it.next();
                attributesList.add(new Label(attributesList.newChildId(), info.getName()));
            }
        }

        add(attributesList);

        super.initComponents();
    }

    protected void init(PageParameters parameters) {
        workspaceName = parameters.get(ResourceConfigurationPage.WORKSPACE).toString();
        storeName = parameters.get(DataAccessEditPage.STORE_NAME).toString();
        layerName = parameters.get(ResourceConfigurationPage.NAME).toString();
    }

    @Override
    @SuppressFBWarnings("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR")
    public List<String> getResourcePaths(PageParameters parameters) {
        if ((workspaceName == null)) {
            this.init(parameters);
        }
        List<String> pathList = new ArrayList<>();
        pathList.add(
                Paths.path(GeoServerConstants.WORKSPACES, workspaceName, storeName, layerName));
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES, workspaceName, storeName));
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES, workspaceName));
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES));
        pathList.add(Paths.path("templates"));
        return pathList;
    }

    @Override
    @SuppressFBWarnings("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR")
    protected String buildResourceFullName() {
        return workspaceName + ":" + layerName;
    }

    private LayerInfo getLayer(String layerName, String workspaceName) {
        LOGGER.info("[getResource] layername=" + layerName + " , workspacename=" + workspaceName);
        LayerInfo layer;
        if (workspaceName != null) {
            NamespaceInfo ns = getCatalog().getNamespaceByPrefix(workspaceName);
            if (ns == null) {
                // unlikely to happen, requires someone making modifications on the workspaces
                // with a layer page open in another tab/window
                throw new RuntimeException("Could not find workspace " + workspaceName);
            }
            String nsURI = ns.getURI();
            layer = getCatalog().getLayerByName(new NameImpl(nsURI, layerName));
        } else {
            layer = getCatalog().getLayerByName(layerName);
        }

        LOGGER.info(
                "successfully loaded layer " + layerName + ", type " + layer.getType().toString());
        return layer;
    }
}
