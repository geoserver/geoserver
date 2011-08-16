/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
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
