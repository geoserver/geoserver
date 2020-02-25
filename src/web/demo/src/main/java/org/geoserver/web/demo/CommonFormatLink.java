/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.geoserver.web.ToolLinkExternalInfo;

/**
 * Extension point for MapPreviewPage. Subclasses will implement getFormatLink which returns an
 * ExternalLink to a layer preview in the subclass format represented by the subclass e.g.
 * GMLFormatLink.
 *
 * @author prushforth
 */
public abstract class CommonFormatLink extends ToolLinkExternalInfo
        implements Comparable<CommonFormatLink> {

    private final String componentId = "theLink";

    private int order = 1000;

    public CommonFormatLink() {
        super();
    }

    /**
     * Returns an ExternalLink object that is used to link to the layer preview.
     *
     * @param layer the PreviewLayer object for which the preview link is returned
     */
    public abstract ExternalLink getFormatLink(PreviewLayer layer);

    @Override
    public void setComponentClass(Class<ExternalLink> componentClass) {}

    public String getComponentId() {
        return componentId;
    }

    /**
     * @param order orders the list of common formats, by default new formats will be added at the
     *     of the list.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(CommonFormatLink other) {
        return getOrder() - other.getOrder();
    }
}
