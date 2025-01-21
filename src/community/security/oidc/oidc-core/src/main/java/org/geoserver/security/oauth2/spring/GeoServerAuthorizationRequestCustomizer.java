/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;
import org.springframework.util.Assert;

/**
 * Adapts {@link OAuth2AuthorizationRequest}s to specific needs. Currently:
 *
 * <ul>
 *   <li>"response_mode" special case
 *   <li>PKCE
 * </ul>
 *
 * @author awaterme
 */
public class GeoServerAuthorizationRequestCustomizer implements Consumer<OAuth2AuthorizationRequest.Builder> {

    private static final Logger LOGGER = Logging.getLogger(GeoServerAuthorizationRequestCustomizer.class);

    private GeoServerOAuth2LoginFilterConfig config;

    /** @param pConfig */
    public GeoServerAuthorizationRequestCustomizer(GeoServerOAuth2LoginFilterConfig pConfig) {
        super();
        config = pConfig;
        Assert.notNull(pConfig, "configuration must not be null");
    }

    @Override
    public void accept(Builder pBuilder) {
        Consumer<Map<String, Object>> lCustomizer = attr -> {
            Object lRegId = attr.get(REGISTRATION_ID);
            boolean lIsOidc = REG_ID_OIDC.equals(lRegId);
            boolean lIsOidcUsePKCE = config.isOidcUsePKCE();

            // Google, GitHub and Azure support PKCE, OIDC depends on configuration
            if (!lIsOidc || (lIsOidc && lIsOidcUsePKCE)) {
                applyPKCE(pBuilder);
            }

            // ResponseMode: only for OIDC
            String lResponseMode = config.getOidcResponseMode();
            boolean lIsOidcRespMode = lResponseMode != null && !lResponseMode.isBlank();
            if (lIsOidc && lIsOidcRespMode) {
                applyResponseModeParam(pBuilder);
            }
        };
        pBuilder.attributes(lCustomizer);
    }

    /** @param pBuilder */
    private void applyPKCE(Builder pBuilder) {
        Consumer<Builder> lConsumer = OAuth2AuthorizationRequestCustomizers.withPkce();
        lConsumer.accept(pBuilder);
    }

    private void applyResponseModeParam(Builder pBuilder) {
        String lResponseMode = config.getOidcResponseMode();
        String lMode = lResponseMode.trim();
        LOGGER.fine("Adding 'response_mode' parameter to authorize request: '" + lMode + "'.");
        pBuilder.additionalParameters(m -> m.put("response_mode", lMode));
    }
}
