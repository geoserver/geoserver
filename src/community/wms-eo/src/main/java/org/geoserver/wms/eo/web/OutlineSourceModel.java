/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;

/**
 * A model that returns the layer group entries backed by an image mosaic, suitable for creating a
 * vector outline layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OutlineSourceModel implements IModel<List<? extends EoLayerGroupEntry>> {
    private static final long serialVersionUID = -5194537044901789111L;

    static final String IMAGE_MOSAIC_FORMAT_NAME = new ImageMosaicFormat().getName();

    private List<EoLayerGroupEntry> items;

    public OutlineSourceModel(List<EoLayerGroupEntry> items) {
        this.items = items;
    }

    @Override
    public void detach() {
        // nothing to do I believe?

    }

    @Override
    public List<? extends EoLayerGroupEntry> getObject() {
        List<EoLayerGroupEntry> result = new ArrayList<EoLayerGroupEntry>();

        // search for entries backed by a image mosaic
        for (EoLayerGroupEntry entry : items) {
            PublishedInfo pi = entry.getLayer();
            if (pi instanceof LayerInfo) {
                LayerInfo li = (LayerInfo) pi;
                if (li.getResource() instanceof CoverageInfo) {
                    CoverageStoreInfo store = (CoverageStoreInfo) li.getResource().getStore();
                    if (IMAGE_MOSAIC_FORMAT_NAME.equals(store.getType())) {
                        result.add(entry);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void setObject(List<? extends EoLayerGroupEntry> object) {
        throw new UnsupportedOperationException(
                "The list of image mosaic entries cannot be modified");
    }
}
