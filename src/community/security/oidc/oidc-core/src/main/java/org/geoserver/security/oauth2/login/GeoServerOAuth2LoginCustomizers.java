/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.util.function.Consumer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * Defines interfaces for optional customizers, which might be used to tweak the OIDC login in a specific way.
 *
 * @author awaterme
 */
public class GeoServerOAuth2LoginCustomizers {

    public interface HttpSecurityCustomizer extends Consumer<HttpSecurity> {}

    public interface ClientRegistrationCustomizer extends Consumer<ClientRegistration> {}

    public interface FilterBuilderCustomizer extends Consumer<GeoServerOAuth2LoginAuthenticationFilterBuilder> {}
}
