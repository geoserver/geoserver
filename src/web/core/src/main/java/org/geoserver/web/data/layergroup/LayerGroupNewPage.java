/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Allows creation of a new layer group
 */
public class LayerGroupNewPage extends AbstractLayerGroupPage {
    
    public LayerGroupNewPage() {
        initUI(getCatalog().getFactory().createLayerGroup());
    }

    @Override
    protected void initUI(LayerGroupInfo layerGroup) {
        super.initUI(layerGroup);

        if (!isAuthenticatedAsAdmin()) {
            //initialize the workspace drop down
            DropDownChoice<WorkspaceInfo> wsChoice = 
                    (DropDownChoice<WorkspaceInfo>) get("form:workspace");
    
            //default to first available workspace
            List<WorkspaceInfo> ws = getCatalog().getWorkspaces(); 
            if (!ws.isEmpty()) {
                wsChoice.setModelObject(ws.get(0));
            }
        }
    }

    @Override
    protected void onSubmit() {
        LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();

        Catalog catalog = getCatalog();
        catalog.add(lg);

        lg = catalog.getLayerGroup(lg.getId());
        doReturn();
    }

}
