/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;

/**
 * Defines LayerGroup visibility policy, used by AdvertisedCatalog.
 *
 * @author Davide Savazzi - GeoSolutions
 */
public interface LayerGroupVisibilityPolicy {

    /** @return true if LayerGroup must be hidden, false otherwise */
    boolean hideLayerGroup(LayerGroupInfo group, List<PublishedInfo> filteredLayers);

    /** Never hide a LayerGroup */
    public static final LayerGroupVisibilityPolicy HIDE_NEVER =
            new LayerGroupVisibilityPolicy() {
                @Override
                public boolean hideLayerGroup(
                        LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
                    return false;
                }
            };

    /** Hide a LayerGroup if it doesn't contain Layers or if its Layers are all hidden */
    public static final LayerGroupVisibilityPolicy HIDE_EMPTY =
            new LayerGroupVisibilityPolicy() {
                @Override
                public boolean hideLayerGroup(
                        LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
                    return filteredLayers.size() == 0;
                }
            };

    /** Hide a LayerGroup if its Layers are all hidden */
    public static final LayerGroupVisibilityPolicy HIDE_IF_ALL_HIDDEN =
            new LayerGroupVisibilityPolicy() {
                @Override
                public boolean hideLayerGroup(
                        LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
                    return filteredLayers.size() == 0 && group.getLayers().size() > 0;
                }
            };
}
