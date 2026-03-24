/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import org.apache.wicket.Page;

public class SidebarNewMenuItemInfo extends ComponentInfo<Page> implements Comparable<SidebarNewMenuItemInfo> {

    @Serial
    private static final long serialVersionUID = 1L;

    private int order = 100;
    private Category category;
    private String icon;

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public int compareTo(SidebarNewMenuItemInfo other) {
        return Integer.compare(this.getOrder(), other.getOrder());
    }
}
