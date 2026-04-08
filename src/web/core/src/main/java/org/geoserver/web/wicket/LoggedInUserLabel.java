/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Optional;
import java.util.Properties;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.config.impl.UserDetailsDisplaySettingsInfoImpl;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.spring.security.GeoServerSession;
import org.springframework.security.core.Authentication;

/**
 * A customised {@link Label} component for displaying the currently logged-in user’s name in the GeoServer Web UI.
 *
 * <p>The {@code LoggedInUserLabel} respects the current GeoServer user details display settings (as configured in
 * {@link UserDetailsDisplaySettingsInfo}) to determine how the user’s name is shown. The visualisation of the name is
 * controlled by {@link UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode}, and can be one of:
 *
 * <ul>
 *   <li>{@code USERNAME} – the GeoServer username is displayed
 *   <li>{@code PREFERRED_USERNAME} – the value of the {@code preferred_username} property is displayed
 *   <li>{@code FULL_NAME} – the concatenation of the {@code first_name} and {@code last_name} properties is displayed
 *   <li>{@code FALLBACK} – tries {@code FULL_NAME} first, then falls back to {@code PREFERRED_USERNAME}, and finally to
 *       {@code USERNAME}
 * </ul>
 *
 * <p>The component safely handles cases where the user properties are missing or null, and will always display the
 * username as fallback if necessary.
 */
public class LoggedInUserLabel extends Label {

    public LoggedInUserLabel(String id) {
        super(
                id,
                Model.of(Optional.ofNullable(resolveLoggedInUserDisplayMode(loggedInUserDisplayMode()))
                        .orElse(GeoServerSession.get().getUsername())));
    }

    private static UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode loggedInUserDisplayMode() {
        UserDetailsDisplaySettingsInfo userDetailsDisplaySettingsInfo = Optional.ofNullable(
                        GeoServerApplication.get().getGeoServer().getGlobal().getUserDetailsDisplaySettings())
                .orElse(new UserDetailsDisplaySettingsInfoImpl());
        return userDetailsDisplaySettingsInfo.getLoggedInUserDisplayMode();
    }

    private static String resolveLoggedInUserDisplayMode(
            UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode loggedInUserDisplayMode) {

        GeoServerSession geoServerSession = GeoServerSession.get();
        String username = geoServerSession.getUsername();

        Authentication auth = geoServerSession.getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof GeoServerUser)) {
            return username;
        }

        GeoServerUser user = (GeoServerUser) auth.getPrincipal();
        Properties userProperties = user.getProperties();

        return switch (loggedInUserDisplayMode) {
            case PREFERRED_USERNAME -> userProperties.getProperty(UserProfilePropertyNames.PREFERRED_USERNAME);
            case FULL_NAME -> joinFirstAndLastNames(userProperties);
            case FALLBACK ->
                Optional.ofNullable(joinFirstAndLastNames(userProperties))
                        .orElse(userProperties.getProperty(UserProfilePropertyNames.PREFERRED_USERNAME));
            case USERNAME -> username;
        };
    }

    private static String joinFirstAndLastNames(Properties userProperties) {
        String firstName = userProperties.getProperty(UserProfilePropertyNames.FIRST_NAME, "");
        String lastName = userProperties.getProperty(UserProfilePropertyNames.LAST_NAME, "");
        String join = String.join(" ", firstName, lastName).trim();
        return join.isEmpty() ? null : join;
    }
}
