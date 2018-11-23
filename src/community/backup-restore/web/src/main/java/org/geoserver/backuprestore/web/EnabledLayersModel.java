/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.util.Iterator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerInfo;

public class EnabledLayersModel extends LoadableDetachableModel {

    IModel<List<LayerInfo>> model;

    public EnabledLayersModel(IModel siModel) {
        model = new LayersModel(siModel);
    }

    @Override
    protected Object load() {
        List<LayerInfo> layers = model.getObject();
        for (Iterator<LayerInfo> it = layers.iterator(); it.hasNext(); ) {
            if (!it.next().isEnabled()) {
                it.remove();
            }
        }
        return layers;
    }

    @Override
    public void detach() {
        super.detach();
        if (model != null) {
            model.detach();
        }
    }
}
