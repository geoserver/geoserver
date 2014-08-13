/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.GeoServerApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple detachable model listing all the available workspaces
 */
@SuppressWarnings("serial")
public class FeatureTypesModel extends LoadableDetachableModel {
    @Override
    protected Object load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<FeatureTypeInfo> layers = new ArrayList<FeatureTypeInfo>(catalog.getFeatureTypes());
        Collections.sort(layers, new FeatureTypeComparator());
        return layers;
    }

    class FeatureTypeComparator implements Comparator<FeatureTypeInfo> {

        public int compare(FeatureTypeInfo w1, FeatureTypeInfo w2) {
            return w1.getName().compareToIgnoreCase(w2.getName());
        }

    }
}
