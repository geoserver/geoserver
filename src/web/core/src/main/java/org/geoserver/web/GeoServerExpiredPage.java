/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.link.Link;

/** Displays a message suggesting the user to login or to elevate his privileges */
public class GeoServerExpiredPage extends GeoServerBasePage {
    public GeoServerExpiredPage() {
        add(
                new Link("homeLink") {
                    public void onClick() {
                        setResponsePage(GeoServerHomePage.class);
                    }
                });
    }
}
