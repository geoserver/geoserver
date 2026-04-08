/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import java.util.Collection;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * {@link Jwt} converter considering GeoServer basic role sources for authorization.
 *
 * <p>Used for the "Resource Server" use case. Implementation is unfinished, because a different GS extension supports
 * this case already. Filter is not offered in UI. This code is never executed.
 *
 * @author awaterme
 */
public class GeoServerJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    private GeoServerRoleResolvers.ResolverContext roleResolverContext;

    public GeoServerJwtAuthenticationConverter(GeoServerRoleResolvers.ResolverContext pCtx) {
        super();
        roleResolverContext = pCtx;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt pSource) {
        JwtAuthenticationToken lToken = (JwtAuthenticationToken) delegate.convert(pSource);
        if (lToken == null) {
            return null;
        }
        String lPrincipal = lToken.getName();

        GeoServerRoleResolvers.RoleResolver lResolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> lRoles = lResolver.convert(new ResolverParam(lPrincipal, null, roleResolverContext));

        JwtAuthenticationToken lNew = new JwtAuthenticationToken(pSource, lRoles, lPrincipal);
        return lNew;
    }

    public void setPrincipalClaimName(String pName) {
        delegate.setPrincipalClaimName(pName);
    }
}
