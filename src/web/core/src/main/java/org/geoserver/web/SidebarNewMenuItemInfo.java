package org.geoserver.web;

import org.apache.wicket.Page;

@SuppressWarnings("serial")
public class SidebarNewMenuItemInfo extends ComponentInfo<Page> implements Comparable<SidebarNewMenuItemInfo> {

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
