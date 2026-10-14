/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.UpdatesCheckerConfigPersister;
import org.geoserver.updateschecker.UpdatesCheckerStatus;
import org.geoserver.web.spring.security.GeoServerSession;

/**
 * Builds the landing-page banner for the current viewer, who must hold the full {@code ADMIN_ROLE}: workspace and layer
 * administrators get nothing.
 */
final class UpdatesCheckerBannerFactory {

    private UpdatesCheckerBannerFactory() {}

    /**
     * @return the landing-page banner, or {@code null} if the module is disabled or the viewer is not a full
     *     administrator.
     */
    static Component createIfVisible(String id) {
        UpdatesCheckerConfigPersister persister = GeoServerExtensions.bean(UpdatesCheckerConfigPersister.class);
        if (!persister.getConfig().isEnabled()) {
            return null;
        }
        if (!isFullAdministrator()) {
            return null;
        }
        return new UpdatesCheckerAsyncPanel(id);
    }

    /** @return the real banner for a status, or an invisible placeholder if there is nothing to show. */
    static Component build(String id, UpdatesCheckerStatus status) {
        if (status.getNotifications().isEmpty()) {
            return invisible(id);
        }
        return new UpdatesCheckerWarningPanel(id, status.getNotifications());
    }

    /** @return whether the current viewer holds the full {@code ADMIN_ROLE}; also true when security is disabled. */
    static boolean isFullAdministrator() {
        GeoServerSession session = GeoServerSession.get();
        return session.getAuthentication() != null && session.isAdmin();
    }

    static Component invisible(String id) {
        WebMarkupContainer placeholder = new WebMarkupContainer(id);
        placeholder.setVisible(false);
        return placeholder;
    }
}
