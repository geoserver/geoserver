/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder;
import org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent;
import org.geoserver.web.LoginFormInfo;
import org.springframework.context.event.EventListener;

/**
 * Enables login buttons for OAuth2 dynamically, depending on the respective providers enablement state. Tracks
 * enablement per filter instance so that multiple filter instances can coexist: a button stays enabled as long as at
 * least one filter instance enables that provider type. The login path is updated to point to the scoped registration
 * ID of the most recently enabled filter for that provider type.
 */
public class OAuth2LoginButtonManager {

    private List<LoginFormInfo> loginFormInfos = new ArrayList<>();

    /**
     * Tracks which scoped registration IDs have enabled each base provider type. Key: lowercase base registration ID
     * (e.g. {@code "google"}), value: set of scoped registration IDs that have this type enabled (e.g.
     * {@code ["myfilter__google"]}).
     */
    private final Map<String, Set<String>> enabledScopedIds = new HashMap<>();

    public OAuth2LoginButtonManager() {
        super();
    }

    @EventListener
    public void enablementChanged(OAuth2LoginButtonEnablementEvent pEvent) {
        String lBaseRegId = pEvent.getRegistrationId().toLowerCase();
        String lScopedRegId = pEvent.getScopedRegistrationId();

        Set<String> lScoped = enabledScopedIds.computeIfAbsent(lBaseRegId, k -> new HashSet<>());
        if (pEvent.isEnable()) {
            lScoped.add(lScopedRegId);
        } else {
            lScoped.remove(lScopedRegId);
        }

        boolean lAnyEnabled = !lScoped.isEmpty();
        // Determine the effective scoped ID for the login path: on enable, use the event's scoped
        // ID; on disable with remaining enabled filters, pick an arbitrary surviving one.
        String lEffectiveScopedId = pEvent.isEnable()
                ? lScopedRegId
                : (lAnyEnabled ? lScoped.iterator().next() : null);

        for (LoginFormInfo lInfo : loginFormInfos) {
            if (lInfo.getId() != null && lInfo.getId().toLowerCase().contains(lBaseRegId)) {
                lInfo.setEnabled(lAnyEnabled);
                if (lAnyEnabled && lEffectiveScopedId != null) {
                    lInfo.setLoginPath("/"
                            + GeoServerOAuth2LoginAuthenticationFilterBuilder.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
                            + "/" + lEffectiveScopedId);
                }
            }
        }
    }

    /** @param pInfos the infos to set */
    public void setLoginFormInfos(List<LoginFormInfo> pInfos) {
        loginFormInfos = pInfos;
    }
}
