/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Optional;
import java.util.Properties;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.config.UIDisplayInfo;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.spring.security.GeoServerSession;
import org.springframework.security.core.Authentication;

/**
 * A customised {@link Label} component for displaying the currently logged-in user’s name in the GeoServer Web UI.
 *
 * <p>The {@code LoggedInUserLabel} respects the current GeoServer UI display settings (as configured in
 * {@link org.geoserver.config.UIDisplayInfo}) to determine how the user’s name is shown. The visualisation of the name
 * is controlled by {@link UIDisplayInfo.LoggedInUserDisplayMode}, and can be one of:
 *
 * <ul>
 *   <li>{@code USERNAME} – the GeoServer username is displayed
 *   <li>{@code PREFERRED_USERNAME} – the value of the {@code preferred_username} property is displayed
 *   <li>{@code FIRST_NAME_LAST_NAME} – the concatenation of the {@code first_name} and {@code last_name} properties is
 *       displayed
 *   <li>{@code FALLBACK} – tries {@code FIRST_NAME_LAST_NAME} first, then falls back to {@code PREFERRED_USERNAME}, and
 *       finally to {@code USERNAME}
 * </ul>
 *
 * <p>The component safely handles cases where the user properties are missing or null, and will always display the
 * username as fallback if necessary.
 */
public class LoggedInUserLabel extends Label {

    public LoggedInUserLabel(String id) {
        super(id, new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return Optional.ofNullable(resolveLoggedInUserDisplayMode(loggedInUserDisplayMode()))
                        .orElse(GeoServerSession.get().getUsername());
            }
        });
    }

    private static UIDisplayInfo.LoggedInUserDisplayMode loggedInUserDisplayMode() {
        return GeoServerApplication.get()
                .getGeoServer()
                .getGlobal()
                .getUiDisplay()
                .getLoggedInUserDisplayMode();
    }

    private static String resolveLoggedInUserDisplayMode(
            UIDisplayInfo.LoggedInUserDisplayMode loggedInUserDisplayMode) {

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
            case FIRST_NAME_LAST_NAME -> joinFirstAndLastNames(userProperties);
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
