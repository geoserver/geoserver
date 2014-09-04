/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.geoserver.web.GeoServerSecuredPage;

public class ReportPage extends GeoServerSecuredPage {

    public ReportPage() {
        
        add(new BookmarkablePageLink("owsSummary", OWSSummaryPage.class));
    }
}
