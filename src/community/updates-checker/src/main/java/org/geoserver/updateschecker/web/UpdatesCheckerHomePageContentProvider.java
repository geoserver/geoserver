/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.Component;
import org.geoserver.web.GeoServerHomePageContentProvider;

/**
 * Contributes the updates-checker warning banner to the GeoServer landing page.
 *
 * <p>Rendered as a real panel (styled with GeoServer's {@code feedbackPanel}/{@code feedbackPanelWARNING} classes)
 * rather than through {@code Session.warn()}, because the warning needs clickable "remind me tomorrow"/"dismiss"
 * actions - Wicket's built-in FeedbackPanel escapes message text, so links can't be embedded in it.
 */
public class UpdatesCheckerHomePageContentProvider implements GeoServerHomePageContentProvider {

    /** Below the security warnings, which have order 0 and are listed from the highest order down. */
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return UpdatesCheckerBannerFactory.createIfVisible(id);
    }
}
