/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.link.ExternalLink;

/**
 * Extension point for tool links in which the destination of the link is not a wicket page but an
 * external link.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ToolLinkExternalInfo extends ComponentInfo<ExternalLink> {

    String href;

    public ToolLinkExternalInfo() {
        super.setComponentClass(ExternalLink.class);
    }

    @Override
    public void setComponentClass(Class<ExternalLink> componentClass) {
        throw new UnsupportedOperationException();
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
