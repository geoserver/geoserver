/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.Comparator;

import org.geoserver.catalog.StoreInfo;

/**
 * Sorts stores by their name
 */
public class StoreNameComparator implements Comparator<StoreInfo> {
    public StoreNameComparator() {
    }

    public int compare(StoreInfo o1, StoreInfo o2) {
        return o1.getName().compareTo(o2.getName());
    }
    
}