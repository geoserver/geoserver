/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2;

import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.AccessToken;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.oauth2.services.OpenIdConnectTokenServices;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/** OpenID Connect authentication filter. */
public class OpenIdConnectAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {

    static final String ID_TOKEN_VALUE = "OpenIdConnect-IdTokenValue";

    public OpenIdConnectAuthenticationFilter(
            SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
        // reconfigure the token services
        if (tokenServices instanceof OpenIdConnectTokenServices
                && config instanceof OpenIdConnectFilterConfig) {
            ((OpenIdConnectTokenServices) tokenServices)
                    .setConfiguration((OpenIdConnectFilterConfig) config);
        }
        // reconfigure the configuration, allow building a useful rest template
        if (oauth2SecurityConfiguration instanceof OpenIdConnectSecurityConfiguration
                && config instanceof OpenIdConnectFilterConfig) {
            OpenIdConnectSecurityConfiguration sc =
                    (OpenIdConnectSecurityConfiguration) oauth2SecurityConfiguration;
            OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) config;
            sc.setConfiguration(idConfig);
        }
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal)
            throws IOException {
        RoleSource rs = getRoleSource();
        if (AccessToken.equals(rs)) {
            return getRolesFromToken(
                    (String) request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE));
        } else if (IdToken.equals(rs)) {
            return getRolesFromToken((String) request.getAttribute(ID_TOKEN_VALUE));
        }

        return super.getRoles(request, principal);
    }

    private Collection<GeoServerRole> getRolesFromToken(String token) throws IOException {
        if (token == null) {
            LOGGER.warning("Token not found, cannot perform role extraction");
            return new ArrayList<>();
        }
        Jwt decoded = JwtHelper.decode(token);
        String claims = decoded.getClaims();
        JSONObject json = (JSONObject) JSONSerializer.toJSON(claims);
        String rolesAttribute =
                ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
        Object o = json.get(rolesAttribute);
        List<GeoServerRole> result = new ArrayList<>();
        if (o instanceof String) {
            result.add(new GeoServerRole((String) o));
        } else if (o instanceof List) {
            ((List) o).stream().forEach(v -> result.add(new GeoServerRole((String) v)));
        } else if (o != null) {
            LOGGER.log(
                    Level.WARNING,
                    "Was expecting to find a list of strings or a single value in "
                            + rolesAttribute
                            + ", but it was something else: "
                            + o);
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Did not find "
                            + rolesAttribute
                            + "in the token, returning an empty role list");
        }

        if (!result.isEmpty()) enrichWithRoleCalculator(result);

        return result;
    }

    private void enrichWithRoleCalculator(List<GeoServerRole> roles) throws IOException {
        RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
        calc.addInheritedRoles(roles);
        calc.addMappedSystemRoles(roles);
    }
}
