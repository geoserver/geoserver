/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode.kombu;

import java.util.ArrayList;
import java.util.List;

public class KombuLayerGroupInfo extends KombuPublishedInfo {

    String mode;

    String rootLayer;

    String rootLayerStyle;

    List<KombuLayerSimpleInfo> layers = new ArrayList<KombuLayerSimpleInfo>(0);

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRootLayer() {
        return rootLayer;
    }

    public void setRootLayer(String rootLayer) {
        this.rootLayer = rootLayer;
    }

    public String getRootLayerStyle() {
        return rootLayerStyle;
    }

    public void setRootLayerStyle(String rootLayerStyle) {
        this.rootLayerStyle = rootLayerStyle;
    }

    public List<KombuLayerSimpleInfo> getLayers() {
        return layers;
    }

    public void setLayers(List<KombuLayerSimpleInfo> layers) {
        this.layers = layers;
    }

    public void addLayer(KombuLayerSimpleInfo layer) {
        this.layers.add(layer);
    }
}
