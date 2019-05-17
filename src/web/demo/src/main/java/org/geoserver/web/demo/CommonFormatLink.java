/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.geoserver.web.ToolLinkExternalInfo;

public abstract class CommonFormatLink extends ToolLinkExternalInfo
        implements Comparable<CommonFormatLink> {

    private final String componentId = "theLink";
    private int order = 1000;

    public CommonFormatLink() {
        super();
    }

    public abstract ExternalLink getFormatLink(
            PreviewLayer layer, boolean wmsVisible, boolean wfsVisible);

    public void setComponentClass(Class<ExternalLink> componentClass) {}

    public String getComponentId() {
        return componentId;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public int compareTo(CommonFormatLink other) {
        return getOrder() - other.getOrder();
    }
}
