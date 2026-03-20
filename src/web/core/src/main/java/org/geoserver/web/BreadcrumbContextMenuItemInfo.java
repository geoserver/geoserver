package org.geoserver.web;

import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@SuppressWarnings("serial")
public class BreadcrumbContextMenuItemInfo extends ComponentInfo<Page>
        implements Comparable<BreadcrumbContextMenuItemInfo> {

    private int order = 100;
    private String targetLevel = "LAYER";
    private Category category;

    private String icon;

    public BreadcrumbContextMenuItemInfo() {}

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
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
    public int compareTo(BreadcrumbContextMenuItemInfo other) {
        return Integer.compare(this.getOrder(), other.getOrder());
    }

    public PageParameters getPageParameters(String workspaceName, String resourceName) {
        PageParameters params = new PageParameters();

        if (workspaceName != null && !workspaceName.isEmpty()) {
            params.add("workspace", workspaceName);
        }

        if (resourceName != null && !resourceName.isEmpty()) {
            params.add("layer", resourceName);
            params.add("group", resourceName);
        }
        return params;
    }
}
