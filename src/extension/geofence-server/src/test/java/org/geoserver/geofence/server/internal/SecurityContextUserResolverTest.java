package org.geoserver.geofence.server.internal;

import java.util.Collection;
import java.util.Collections;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUserResolverTest {

    @Test
    public void existsUser() {
        // Given
        SecurityContextUserResolver securityContextUserResolver = new SecurityContextUserResolver();

        // When and Then
        Assert.assertThrows(
                IllegalStateException.class,
                () -> securityContextUserResolver.existsUser("some-user"));
    }

    @Test
    public void existsRole() {
        // Given
        SecurityContextUserResolver securityContextUserResolver = new SecurityContextUserResolver();

        // When and Then
        Assert.assertThrows(
                IllegalStateException.class,
                () -> securityContextUserResolver.existsRole("some-role"));
    }

    @Test
    public void getRolesEmpty() {
        // Given
        SecurityContextUserResolver securityContextUserResolver = new SecurityContextUserResolver();
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        SecurityContextHolder.setContext(securityContext);

        // When
        securityContextUserResolver.getRoles("some-user");

        // Then
        Mockito.verify(authentication, Mockito.atLeastOnce()).getAuthorities();
    }

    @Test
    public void getRoles() {
        // Given
        SecurityContextUserResolver securityContextUserResolver = new SecurityContextUserResolver();
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.<Collection<? extends GrantedAuthority>>when(authentication.getAuthorities())
                .thenReturn(Collections.singletonList(GeoServerRole.AUTHENTICATED_ROLE));
        SecurityContextHolder.setContext(securityContext);

        // When
        securityContextUserResolver.getRoles("some-user");

        // Then
        Mockito.verify(authentication, Mockito.atLeastOnce()).getAuthorities();
    }
}
