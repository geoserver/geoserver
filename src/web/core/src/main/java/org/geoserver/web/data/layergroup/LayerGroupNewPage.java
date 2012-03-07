/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;

/**
 * Allows creation of a new layer group
 */
public class LayerGroupNewPage extends AbstractLayerGroupPage {
    
    public LayerGroupNewPage() {
        initUI(getCatalog().getFactory().createLayerGroup());
    }

    @Override
    protected void onSubmit() {
        LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();

        Catalog catalog = getCatalog();
        catalog.add(lg);

        lg = catalog.getLayerGroup(lg.getId());
        setResponsePage(LayerGroupPage.class);
    }

}
