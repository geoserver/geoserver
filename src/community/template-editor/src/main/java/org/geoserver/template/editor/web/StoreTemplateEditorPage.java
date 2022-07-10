/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.platform.resource.Paths;
import org.geoserver.template.editor.constants.GeoServerConstants;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geotools.util.SuppressFBWarnings;

public class StoreTemplateEditorPage extends AbstractTemplateEditorPage {
    private String workspaceName;

    private String storeName = "";

    public StoreTemplateEditorPage() {
        super();
    }

    public StoreTemplateEditorPage(PageParameters parameters) {
        super(parameters);
    }

    protected void init(PageParameters parameters) {
        workspaceName = parameters.get(DataAccessEditPage.WS_NAME).toString();
        storeName = parameters.get(DataAccessEditPage.STORE_NAME).toString();
        this.resourceType = "store";
    }

    @Override
    @SuppressFBWarnings("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR")
    public List<String> getResourcePaths(PageParameters parameters) {
        if ((workspaceName == null)) {
            this.init(parameters);
        }
        List<String> pathList = new ArrayList<>();
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES, workspaceName, storeName));
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES, workspaceName));
        pathList.add(Paths.path(GeoServerConstants.WORKSPACES));
        pathList.add(Paths.path("templates"));
        return pathList;
    }

    @Override
    @SuppressFBWarnings("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR")
    protected String buildResourceFullName() {
        return workspaceName + ":" + storeName;
    }
}
