/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListView;

/**
 * {@link GeoServerHomePage} extension point allowing to contribute a Wicket component listing links
 * to the GetCapabilities or similar service description document for the various services provided
 * by GeoServer.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 * @see GeoServerHomePageContentProvider
 */
public interface CapabilitiesHomePageLinkProvider {

    /**
     * Returns a component to be added as a child of the home page {@link ListView} that contains
     * the list of GetCapabilities links.
     *
     * @param id the id of the returned component
     * @return a component suitable to be contained by the home page list of getcapabilities links
     */
    public Component getCapabilitiesComponent(final String id);
}
