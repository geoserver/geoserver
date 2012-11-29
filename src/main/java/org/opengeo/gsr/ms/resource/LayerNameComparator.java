package org.opengeo.gsr.ms.resource;

import java.util.Comparator;

import org.geoserver.catalog.LayerInfo;

final class LayerNameComparator implements Comparator<LayerInfo> {
    public final static LayerNameComparator INSTANCE = new LayerNameComparator();
    private LayerNameComparator() {
    }
    
    @Override
    public int compare(LayerInfo a, LayerInfo b) {
        return a.getName().compareTo(b.getName());
    }
}