/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layergroup.LayerListPanel;
import org.geoserver.wms.eo.EoLayerType;
import org.geoserver.wms.eo.web.EoLayerGroupEntryPanel.LayerGroupEntryProvider;

public class EoLayerListPanel extends LayerListPanel {
    private static final long serialVersionUID = -7650810117220868467L;

    public EoLayerListPanel(
            String contentId,
            final EoLayerType layerType,
            final LayerGroupEntryProvider entryProvider) {
        super(
                contentId,
                new LayerListProvider() {

                    @Override
                    protected List<LayerInfo> getItems() {
                        List<LayerInfo> layers =
                                GeoServerApplication.get().getCatalog().getLayers();

                        // collect the layers we already have, no dupes in the EO group
                        Set<String> existingLayerIds = new HashSet<String>();
                        for (EoLayerGroupEntry entry : entryProvider.getItems()) {
                            existingLayerIds.add(entry.layerId);
                        }

                        List<LayerInfo> results = new ArrayList<LayerInfo>();
                        for (LayerInfo layer : layers) {
                            if (layerType == EoLayerType.COVERAGE_OUTLINE) {
                                // outlines are only vector
                                if (!(layer.getResource() instanceof FeatureTypeInfo)) {
                                    continue;
                                }
                            } else {
                                // we can only add raster layers for the other types
                                if (!(layer.getResource() instanceof CoverageInfo)) {
                                    continue;
                                }
                            }

                            // avoid dupes
                            if (existingLayerIds.contains(layer.getId())) {
                                continue;
                            }

                            // we can only add layers having a time dimension
                            if (layer.getResource()
                                            .getMetadata()
                                            .get(ResourceInfo.TIME, DimensionInfo.class)
                                    == null) {
                                continue;
                            }

                            results.add(layer);
                        }

                        return results;
                    }
                });
    }
}
