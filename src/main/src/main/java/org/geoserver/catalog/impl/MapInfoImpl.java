/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;

public class MapInfoImpl implements MapInfo {

    String id;
    String name;
    boolean enabled;

    List<LayerInfo> layers;

    public MapInfoImpl() {
        layers = new ArrayList<LayerInfo>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<LayerInfo> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerInfo> layers) {
        this.layers = layers;
    }

    public void accept(CatalogVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(name)
                .append(']')
                .toString();
    }
}
