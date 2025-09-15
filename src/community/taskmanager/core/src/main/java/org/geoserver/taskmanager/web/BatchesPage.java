/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.io.Serial;
import org.geoserver.taskmanager.web.panel.BatchesPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;

public class BatchesPage extends GeoServerSecuredPage {

    @Serial
    private static final long serialVersionUID = 2273966783474224452L;

    @Override
    public void onInitialize() {
        super.onInitialize();
        add(new BatchesPanel("batchesPanel"));
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
