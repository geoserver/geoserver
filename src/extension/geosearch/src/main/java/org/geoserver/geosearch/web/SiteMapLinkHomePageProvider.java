/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
