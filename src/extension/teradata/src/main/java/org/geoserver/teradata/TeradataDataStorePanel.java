/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.teradata;

import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geotools.data.teradata.TeradataDataStoreFactory;

public class TeradataDataStorePanel extends DefaultDataStoreEditPanel {

    public TeradataDataStorePanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);
    }

    @Override
    protected void applyParamDefault(ParamInfo paramInfo, StoreInfo info) {
        if (paramInfo.getName() == TeradataDataStoreFactory.APPLICATION.key) {
            info.getConnectionParameters()
                    .put(TeradataDataStoreFactory.APPLICATION.key, "GeoServer");
        } else {
            super.applyParamDefault(paramInfo, info);
        }
    }
}
