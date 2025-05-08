/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

/**
 * GeoServer factory for OIDC token decoding allows to replace the default Spring {@link OidcIdTokenDecoderFactory}.
 * Required to support reconfiguration through the GS admin UI. The {@link #delegate} has to be replaced to use empty
 * stale caches.
 *
 * @author awaterme
 */
public class GeoServerOidcIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    private volatile OidcIdTokenDecoderFactory delegate;

    @Override
    public JwtDecoder createDecoder(ClientRegistration pContext) {
        if (delegate == null) {
            throw new IllegalStateException("Decoder creation failed. Required configuration is missing.");
        }
        return delegate.createDecoder(pContext);
    }

    public void setGeoServerOAuth2LoginFilterConfig(GeoServerOAuth2LoginFilterConfig pConfig) {
        if (pConfig == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        OidcIdTokenDecoderFactory lFactory = new OidcIdTokenDecoderFactory();
        lFactory.setJwsAlgorithmResolver(new GeoServerJwsAlgorithmResolver(pConfig));
        lFactory.setJwtValidatorFactory(new GeoServerOidcIdTokenValidatorFactory(pConfig));
        delegate = lFactory;
    }

    /** @return the delegate */
    public OidcIdTokenDecoderFactory getDelegate() {
        return delegate;
    }
}
