/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.platform.GeoServerExtensions;

@SuppressWarnings("serial")
public class GeoServerWicketServlet extends WicketServlet {

    @Override
    protected WicketFilter newWicketFilter() {
        WicketFilter filter =
                new WicketFilter(GeoServerExtensions.bean(GeoServerApplication.class));
        filter.setFilterPath("/web");
        return filter;
    }
}
