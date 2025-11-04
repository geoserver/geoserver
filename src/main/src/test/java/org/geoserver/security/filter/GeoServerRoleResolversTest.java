package org.geoserver.security.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerRoleConverterImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;

public class GeoServerRoleResolversTest {

    public static final String PRINCIPAL_NAME = "test_user";
    public static final String ROLE_HEADER_ATTRIBUTE = "roleHeader";

    private HttpServletRequest request;
    private GeoServerSecurityManager securityManager;
    private GeoServerRoleService roleService;
    private GeoServerUserGroupService userGroupService;
    private GeoServerRoleConverter roleConverter;
    private List<GeoServerRole> roles;
    private TreeSet<GeoServerRole> sortedRoles;

    @Before
    public void setUp() {
        request = mock();
        securityManager = mock();
        roleService = mock();
        userGroupService = mock();
        roleConverter = new GeoServerRoleConverterImpl();
        roles = List.of(new GeoServerRole("role1"), new GeoServerRole("role2"), new GeoServerRole("role3"));
        sortedRoles = new TreeSet<>();
        sortedRoles.addAll(roles);
    }

    @Test
    public void testResolveActiveRoleService() throws IOException {
        when(securityManager.getActiveRoleService()).thenReturn(roleService);
        when(roleService.getRolesForUser(PRINCIPAL_NAME)).thenReturn(sortedRoles);
        RoleSource roleSource = PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.RoleService;
        final GeoServerRoleResolvers.DefaultResolverContext context = new GeoServerRoleResolvers.DefaultResolverContext(
                securityManager, null, null, null, roleConverter, roleSource);
        GeoServerRoleResolvers.RoleResolver resolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> actualRoles =
                resolver.convert(new GeoServerRoleResolvers.ResolverParam(PRINCIPAL_NAME, request, context));
        assertEquals(sortedRoles, actualRoles);
    }

    @Test
    public void testResolveRoleService() throws IOException {
        final String roleServiceName = "roleService";
        when(securityManager.loadRoleService(roleServiceName)).thenReturn(roleService);
        when(roleService.getRolesForUser(PRINCIPAL_NAME)).thenReturn(sortedRoles);
        RoleSource roleSource = PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.RoleService;
        final GeoServerRoleResolvers.DefaultResolverContext context = new GeoServerRoleResolvers.DefaultResolverContext(
                securityManager, roleServiceName, null, null, roleConverter, roleSource);
        GeoServerRoleResolvers.RoleResolver resolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> actualRoles =
                resolver.convert(new GeoServerRoleResolvers.ResolverParam(PRINCIPAL_NAME, request, context));
        assertEquals(sortedRoles, actualRoles);
    }

    @Test
    public void testResolveUserGroupService() throws IOException {
        final String userGroupServiceName = "userGroupService";
        UserDetails details = mock();
        when(securityManager.loadUserGroupService(userGroupServiceName)).thenReturn(userGroupService);
        when(userGroupService.loadUserByUsername(PRINCIPAL_NAME)).thenReturn(details);
        doReturn(roles).when(details).getAuthorities();
        RoleSource roleSource =
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.UserGroupService;
        final GeoServerRoleResolvers.DefaultResolverContext context = new GeoServerRoleResolvers.DefaultResolverContext(
                securityManager, null, userGroupServiceName, null, roleConverter, roleSource);
        GeoServerRoleResolvers.RoleResolver resolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> actualRoles =
                resolver.convert(new GeoServerRoleResolvers.ResolverParam(PRINCIPAL_NAME, request, context));
        assertEquals(roles, actualRoles);
    }

    @Test
    public void testResolveHeaderAttribute() {
        when(request.getHeader(ROLE_HEADER_ATTRIBUTE)).thenReturn("role1;role2;role3");
        RoleSource roleSource = PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header;
        final GeoServerRoleResolvers.DefaultResolverContext context = new GeoServerRoleResolvers.DefaultResolverContext(
                securityManager, null, null, ROLE_HEADER_ATTRIBUTE, roleConverter, roleSource);
        GeoServerRoleResolvers.RoleResolver resolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> actualRoles =
                resolver.convert(new GeoServerRoleResolvers.ResolverParam("test_user", request, context));
        assertEquals(roles, actualRoles);
    }
}
