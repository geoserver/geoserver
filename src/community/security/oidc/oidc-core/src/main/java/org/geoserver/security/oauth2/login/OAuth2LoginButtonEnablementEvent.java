/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.io.Serial;
import org.springframework.context.ApplicationEvent;

/**
 * Event signals a changed OAuth2/OIDC provider activation. Fired on configuration changes, triggers visibility updates
 * for login buttons.
 *
 * @author awaterme
 */
public class OAuth2LoginButtonEnablementEvent extends ApplicationEvent {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 513879448251262654L;

    public static final OAuth2LoginButtonEnablementEvent enableButtonEvent(Object pSource, String pId) {
        return new OAuth2LoginButtonEnablementEvent(pSource, true, pId);
    }

    public static final OAuth2LoginButtonEnablementEvent disableButtonEvent(Object pSource, String pId) {
        return new OAuth2LoginButtonEnablementEvent(pSource, false, pId);
    }

    private boolean enable;
    private String registrationId;

    /**
     * @param pSource
     * @param pEnable
     * @param pRegistrationId
     */
    public OAuth2LoginButtonEnablementEvent(Object pSource, boolean pEnable, String pRegistrationId) {
        super(pSource);
        enable = pEnable;
        registrationId = pRegistrationId;
    }

    /** @return the enable */
    public boolean isEnable() {
        return enable;
    }

    /** @return the registrationId */
    public String getRegistrationId() {
        return registrationId;
    }
}
