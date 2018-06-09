/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Allows the user to create a new EO layer group
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EoLayerGroupNewPage extends EoLayerGroupAbstractPage {

    public EoLayerGroupNewPage() {
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        lg.setMode(Mode.EO);
        initUI(lg);
    }

    @Override
    protected void initUI(LayerGroupInfo layerGroup) {
        super.initUI(layerGroup);

        if (!isAuthenticatedAsAdmin()) {
            // initialize the workspace drop down
            DropDownChoice<WorkspaceInfo> wsChoice =
                    (DropDownChoice<WorkspaceInfo>) get("form:workspace");

            // default to first available workspace
            List<WorkspaceInfo> ws = getCatalog().getWorkspaces();
            if (!ws.isEmpty()) {
                wsChoice.setModelObject(ws.get(0));
            }
        }
    }

    @Override
    protected void onSubmit(LayerGroupInfo lg) {
        // check the layer group does not exist yet
        LayerGroupInfo preExisting;
        String lgName = lg.getName();
        if (lg.getWorkspace() != null) {
            String wsName = lg.getWorkspace().getName();
            preExisting = getCatalog().getLayerGroupByName(wsName, lgName);
            if (preExisting != null) {
                error(
                        new ParamResourceModel(
                                        "layerGroupAlreadyExistsInWorkspace", this, lgName, wsName)
                                .getString());
                return;
            }
        } else {
            preExisting = getCatalog().getLayerGroupByName(lgName);
            if (preExisting != null) {
                error(new ParamResourceModel("layerGroupAlreadyExists", this, lgName).getString());
                return;
            }
        }

        Catalog catalog = getCatalog();
        catalog.add(lg);

        lg = catalog.getLayerGroup(lg.getId());
        doReturn();
    }
}
