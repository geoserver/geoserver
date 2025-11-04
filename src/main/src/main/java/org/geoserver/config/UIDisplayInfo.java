/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Info;

/** GeoServer UIDisplay information. */
public interface UIDisplayInfo extends Info {

    enum LoggedInUserDisplayMode {
        USERNAME,
        PREFERRED_USERNAME,
        FIRST_NAME_LAST_NAME,
        FALLBACK;
    }

    enum EmailDisplayMode {
        HIDDEN,
        DOMAIN_ONLY,
        MASKED,
        FULL;

        public boolean allowsReveal() {
            return this == DOMAIN_ONLY || this == MASKED;
        }
    }

    @Override
    String getId();

    LoggedInUserDisplayMode getLoggedInUserDisplayMode();

    void setLoggedInUserDisplayMode(LoggedInUserDisplayMode loggedInUserDisplayMode);

    boolean getShowProfileColumnsInUserList();

    void setShowProfileColumnsInUserList(boolean showProfileColumnsInUserList);

    EmailDisplayMode getEmailDisplayMode();

    void setEmailDisplayMode(EmailDisplayMode emailDisplayMode);

    boolean getRevealEmailAtClick();

    void setRevealEmailAtClick(boolean revealEmailAtClick);
}
