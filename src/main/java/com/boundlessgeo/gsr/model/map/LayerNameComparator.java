/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.map;

import java.util.Comparator;

import org.geoserver.catalog.LayerInfo;

/**
 * Sort on {@link LayerInfo#getName()}
 */
public final class LayerNameComparator implements Comparator<LayerInfo> {
    public final static LayerNameComparator INSTANCE = new LayerNameComparator();
    private LayerNameComparator() {
    }

    public int compare(LayerInfo a, LayerInfo b) {
        return a.getName().compareTo(b.getName());
    }
}
