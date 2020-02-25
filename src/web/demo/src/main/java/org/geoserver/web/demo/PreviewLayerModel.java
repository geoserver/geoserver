/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.web.GeoServerApplication;

/**
 * A detachable model for the {@link PreviewLayer}
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class PreviewLayerModel extends LoadableDetachableModel {
    String id;
    boolean group;

    public PreviewLayerModel(PreviewLayer pl) {
        super(pl);
        id = pl.layerInfo != null ? pl.layerInfo.getId() : pl.groupInfo.getId();
        group = pl.groupInfo != null;
    }

    @Override
    protected Object load() {
        if (group) {
            return new PreviewLayer(GeoServerApplication.get().getCatalog().getLayerGroup(id));
        } else {
            return new PreviewLayer(GeoServerApplication.get().getCatalog().getLayer(id));
        }
    }
}
