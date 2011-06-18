package org.geoserver.monitor.web;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.geoserver.web.GeoServerSecuredPage;

public class ReportPage extends GeoServerSecuredPage {

    public ReportPage() {
        
        add(new BookmarkablePageLink("owsSummary", OWSSummaryPage.class));
    }
}
