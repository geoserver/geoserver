/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.Component;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.ConfigPersister;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geoserver.web.spring.security.GeoServerSession;

/** Adds the banner to the welcome page, for full administrators only, while the module is enabled. */
public class HomePageContentProvider implements GeoServerHomePageContentProvider {

    /**
     * Below the security warnings, which have order 0 and are listed from the highest order down. Not an
     * {@code @Override}, because GeoServer 2.x has no {@code getOrder()}, so the same source compiles there.
     */
    @SuppressWarnings({"MissingOverride", "PMD.MissingOverride"})
    public int getOrder() {
        return -1;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        boolean enabled =
                GeoServerExtensions.bean(ConfigPersister.class).getConfig().isEnabled();
        return enabled && isFullAdministrator() ? new BannerPanel(id) : null;
    }

    /** @return whether the viewer holds the full {@code ADMIN_ROLE}, which is also true when security is disabled */
    static boolean isFullAdministrator() {
        GeoServerSession session = GeoServerSession.get();
        return session.getAuthentication() != null && session.isAdmin();
    }
}
