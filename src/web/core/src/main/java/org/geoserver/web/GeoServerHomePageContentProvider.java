/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.Component;
import org.geoserver.platform.GeoServerExtensions;

/**
 * {@link GeoServerHomePage} extension point allowing to contribute a Wicket component to the
 * central body of the home page.
 *
 * <p>Instances of this type are to be looked up by the {@code GeoServerHomePage} by means of the
 * {@link GeoServerExtensions} mechanism and appended to the home page's central body in no
 * predefined order.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePageLinkProvider
 */
public interface GeoServerHomePageContentProvider {

    /**
     * Returns a component to be added to the home page central body.
     *
     * <p>Note {@code null} is a valid return value, so that implementations may opt not to
     * contribute extra content to the home page under a circumstance of their choice. For example,
     * if the contributed content is not meant to be visible to anonymous users, etc.
     *
     * @param id the id of the returned component
     * @return a component suitable to be contained by the home page central body, or {@code null}
     *     if no extra content is provided.
     */
    public Component getPageBodyComponent(final String id);
}
