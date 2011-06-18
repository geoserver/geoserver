package org.geoserver.geosearch.web;

import org.apache.wicket.Component;
import org.geoserver.web.GeoServerHomePageContentProvider;

public class SiteMapLinkHomePageProvider implements GeoServerHomePageContentProvider {

    /**
     * @see org.geoserver.web.GeoServerHomePageContentProvider#getPageBodyComponent(java.lang.String)
     */
    public Component getPageBodyComponent(final String id) {
        return new SitemapPanel(id);
    }

}
