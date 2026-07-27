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

    public static final OAuth2LoginButtonEnablementEvent enableButtonEvent(
            Object pSource, String pBaseRegId, String pScopedRegId) {
        return new OAuth2LoginButtonEnablementEvent(pSource, true, pBaseRegId, pScopedRegId);
    }

    public static final OAuth2LoginButtonEnablementEvent disableButtonEvent(
            Object pSource, String pBaseRegId, String pScopedRegId) {
        return new OAuth2LoginButtonEnablementEvent(pSource, false, pBaseRegId, pScopedRegId);
    }

    private boolean enable;
    private String registrationId;
    private String scopedRegistrationId;

    /**
     * @param pSource event source
     * @param pEnable whether to enable or disable the button
     * @param pRegistrationId base registration ID for matching (e.g. {@code "google"})
     * @param pScopedRegistrationId scoped registration ID for constructing login paths (e.g.
     *     {@code "myfilter__google"})
     */
    public OAuth2LoginButtonEnablementEvent(
            Object pSource, boolean pEnable, String pRegistrationId, String pScopedRegistrationId) {
        super(pSource);
        enable = pEnable;
        registrationId = pRegistrationId;
        scopedRegistrationId = pScopedRegistrationId;
    }

    /** @return the enable */
    public boolean isEnable() {
        return enable;
    }

    /** @return the base registration ID */
    public String getRegistrationId() {
        return registrationId;
    }

    /** @return the scoped registration ID (includes filter name prefix) */
    public String getScopedRegistrationId() {
        return scopedRegistrationId;
    }
}
