/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;

/**
 * A simple bean to represent a sidebar category.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public class Category implements Comparable<Category>, Serializable {
    /** The sort key to determine the order of the categories in the menu. */
    private int order;

    /** The key to look up the internationalized name for this category. */
    private String namekey;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getNameKey() {
        return namekey;
    }

    public void setNameKey(String namekey) {
        this.namekey = namekey;
    }

    public int compareTo(Category other) {
        return getOrder() - other.getOrder();
    }
}
