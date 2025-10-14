/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent;
import org.geoserver.web.LoginFormInfo;
import org.springframework.context.event.EventListener;

/**
 * Enables login buttons for OAuth2 dynamically, depending on the respective providers enablement state. Required since
 * a single filter instance supports multiple providers, so the regular enablement of login buttons based on the
 * presence of a filter is not sufficient.
 */
public class OAuth2LoginButtonManager {

    private List<LoginFormInfo> loginFormInfos = new ArrayList<>();

    public OAuth2LoginButtonManager() {
        super();
    }

    @EventListener
    public void enablementChanged(OAuth2LoginButtonEnablementEvent pEvent) {
        String lRegId = pEvent.getRegistrationId().toLowerCase();
        for (LoginFormInfo lInfo : loginFormInfos) {
            if (lInfo.getId() != null && lInfo.getId().toLowerCase().contains(lRegId)) {
                lInfo.setEnabled(pEvent.isEnable());
            }
        }
    }

    /** @param pInfos the infos to set */
    public void setLoginFormInfos(List<LoginFormInfo> pInfos) {
        loginFormInfos = pInfos;
    }
}
