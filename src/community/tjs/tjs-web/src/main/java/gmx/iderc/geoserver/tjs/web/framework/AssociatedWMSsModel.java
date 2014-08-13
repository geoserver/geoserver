/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wms.WMS;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple detachable model listing all the available workspaces
 */
@SuppressWarnings("serial")
public class AssociatedWMSsModel extends LoadableDetachableModel {

    @Override
    protected Object load() {
//        Catalog catalog = GeoServerApplication.get().getCatalog();
//        ApplicationContext context = GeoServerApplication.get().getApplicationContext();
        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        try {
            WMS localWms = WMS.get();
            layers.addAll(localWms.getLayers());
            Collections.sort(layers, new LayerComparator());
        } catch (NoSuchBeanDefinitionException ex) {

        }
        return layers;
    }

    class LayerComparator implements Comparator<LayerInfo> {

        public int compare(LayerInfo w1, LayerInfo w2) {
            return w1.getName().compareToIgnoreCase(w2.getName());
        }

    }
}
