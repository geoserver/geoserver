/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import org.geoserver.taskmanager.web.ConfigurationPage;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Niels Charlier
 *
 */
@Component
public class LayerEditAction implements Action {
    
    private static final long serialVersionUID = 6978608806982184868L;
    
    private final static String NAME = "LayerEdit";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String execute(ConfigurationPage onPage, String value) {
        String[] prefixname = value.split(":", 2);
        onPage.setResponsePage(
                new ResourceConfigurationPage(prefixname.length > 1 ? prefixname[0] : null,
                        prefixname[prefixname.length - 1]).setReturnPage(onPage));
        return value;
    }

    @Override
    public boolean accept(String value) {
        if (value == null || "".equals(value)) {
            return false;
        } else {
            return GeoServerApplication.get().getCatalog().getLayerByName(value) != null;
        }
    }

}
