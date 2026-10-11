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
import org.springframework.security.core.Authentication;

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
        String username = eligibleUsername();
        if (username == null) {
            return null;
        }
        return new UpdatesCheckerAsyncPanel(id, username);
    }

    /** @return the real banner for a status, or an invisible placeholder if there is nothing to show. */
    static Component build(String id, String username, UpdatesCheckerStatus status) {
        if (status.getNotifications().isEmpty()) {
            return invisible(id);
        }
        return new UpdatesCheckerWarningPanel(id, status.getNotifications(), username);
    }

    /**
     * @return the current viewer's username if they hold the full {@code ADMIN_ROLE}, else {@code null}. Note that this
     *     also passes when security is disabled for the request.
     */
    static String eligibleUsername() {
        GeoServerSession session = GeoServerSession.get();
        Authentication authentication = session.getAuthentication();
        if (authentication == null || !session.isAdmin()) {
            return null;
        }
        return authentication.getName();
    }

    static Component invisible(String id) {
        WebMarkupContainer placeholder = new WebMarkupContainer(id);
        placeholder.setVisible(false);
        return placeholder;
    }
}
