/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@Category(SystemTest.class)
public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    @Test
    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("admin", "geoserver", List.of(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));
    }

    /**
     * checkForDefaultAdminPassword() probes the default admin credentials to decide whether to show the "change your
     * password" warning. That probe must never be visible to the rest of the application as a real login:
     * {@link org.geoserver.web.GeoServerApplication} (and potentially other listeners, e.g. audit logging) reacts to
     * any {@link AuthenticationSuccessEvent} as if the user had just logged in interactively, which would incorrectly
     * fire for every admin page view while the default password is still active.
     */
    @Test
    public void testCheckForDefaultAdminPasswordDoesNotPublishAuthenticationEvent() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        List<AuthenticationSuccessEvent> captured = new ArrayList<>();
        ApplicationListener<AuthenticationSuccessEvent> listener = captured::add;
        applicationContext.addApplicationListener(listener);
        try {
            // sanity check: the probe itself must still correctly report the default password as
            // unchanged, otherwise this test would trivially pass for the wrong reason
            assertTrue(secMgr.checkForDefaultAdminPassword());

            assertTrue(
                    "checkForDefaultAdminPassword() must not publish a real AuthenticationSuccessEvent - doing so "
                            + "is indistinguishable from an actual admin login to any listener in the application",
                    captured.isEmpty());
        } finally {
            applicationContext.removeApplicationListener(listener);
        }
    }

    /**
     * The failure path leaks just as badly as the success path: before the fix, probing with the default credentials
     * against an account whose password has actually been changed would still authenticate against the shared,
     * event-publishing {@code providerMgr}, publishing a real {@link AuthenticationFailureBadCredentialsEvent} on every
     * admin landing-page view. {@link BruteForceListener} listens for exactly that event type to drive its
     * login-delay/lockout tracking - so, without this fix, an admin who did the right thing and changed the default
     * password could have every landing-page view recorded as a failed login attempt against their own account. The
     * [GEOS-12083] {@code withThrottlingDisabled()} wrapper only ever masked this for {@link BruteForceListener}
     * specifically (it checks a thread-local before acting on the event) - it never stopped the event from being
     * published, so any other listener was still fooled.
     */
    @Test
    public void testCheckForDefaultAdminPasswordFailurePathDoesNotLeakEvent() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        List<AuthenticationProvider> originalProviders = new ArrayList<>(secMgr.getProviders());

        List<AbstractAuthenticationEvent> captured = new ArrayList<>();
        ApplicationListener<AbstractAuthenticationEvent> listener = captured::add;
        applicationContext.addApplicationListener(listener);
        try {
            secMgr.setProviders(List.of(new AlwaysFailAuthenticationProvider()));

            assertFalse(
                    "sanity check: the probe must correctly report a changed password as such",
                    secMgr.checkForDefaultAdminPassword());

            assertTrue(
                    "a failed probe must not leak any AbstractAuthenticationEvent (success or failure) - this is "
                            + "what BruteForceListener listens for to drive login delays/lockouts",
                    captured.isEmpty());
        } finally {
            applicationContext.removeApplicationListener(listener);
            secMgr.setProviders(originalProviders);
        }
    }

    /**
     * Guards against a plausible future "optimization": caching the private probe {@code ProviderManager} as a field
     * instead of building it fresh on every call, to avoid the small per-call allocation. If that's ever done without
     * invalidating the cache on {@code reload()}/{@code setProviders()}, the probe would silently keep authenticating
     * against a stale provider list.
     */
    @Test
    public void testCheckForDefaultAdminPasswordReflectsLiveProviderListNotACachedCopy() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        List<AuthenticationProvider> originalProviders = new ArrayList<>(secMgr.getProviders());

        try {
            // the real provider list currently authenticates admin/geoserver successfully; swapping
            // in a provider that always fails must change the probe's answer on the very next call
            secMgr.setProviders(List.of(new AlwaysFailAuthenticationProvider()));
            assertFalse(secMgr.checkForDefaultAdminPassword());

            // swapping back to a provider that always succeeds must flip the answer again
            secMgr.setProviders(List.of(new AlwaysSucceedAuthenticationProvider()));
            assertTrue(secMgr.checkForDefaultAdminPassword());
        } finally {
            secMgr.setProviders(originalProviders);
        }
    }

    /** Always throws {@link BadCredentialsException}, regardless of the credentials offered. */
    private static final class AlwaysFailAuthenticationProvider implements AuthenticationProvider {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            throw new BadCredentialsException("stub: always fails");
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }

    /** Always authenticates successfully, regardless of the credentials offered. */
    private static final class AlwaysSucceedAuthenticationProvider implements AuthenticationProvider {
        @Override
        public Authentication authenticate(Authentication authentication) {
            UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(), authentication.getCredentials(), List.of());
            return result;
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }

    @Test
    public void testMasterPasswordForMigration() throws Exception {

        // simulate no user.properties file
        GeoServerSecurityManager secMgr = getSecurityManager();
        char[] generatedPW = secMgr.extractMasterPasswordForMigration(null);
        assertEquals(8, generatedPW.length);

        Properties props = new Properties();
        String adminUser = "user1";
        String noAdminUser = "user2";

        // check all users with default password
        String defaultMasterePassword = String.valueOf(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        props.put(GeoServerUser.ADMIN_USERNAME, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(adminUser, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(noAdminUser, defaultMasterePassword + ",ROLE_WFS");

        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword" + ",ROLE_WFS");
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // password to short  for adminuser
        props.put(adminUser, "abc" + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // valid password for user having admin role

        String validPassword = "validPassword";
        props.put(adminUser, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));

        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));

        // assert configuration reload works properly
        secMgr.reload();
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        // GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain =
                config.getFilterChain().getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }

    @Test
    public void testReloadClearsCaches() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // populate caches by loading default services
        secMgr.loadRoleService("default");
        secMgr.loadUserGroupService("default");
        secMgr.loadPasswordValidator("default");

        // verify caches are populated (precondition)
        assertFalse("roleServices cache should not be empty", secMgr.roleServices.isEmpty());
        assertFalse("userGroupServices cache should not be empty", secMgr.userGroupServices.isEmpty());
        assertFalse("passwordValidators cache should not be empty", secMgr.passwordValidators.isEmpty());

        // capture cached instances by identity
        GeoServerRoleService oldRoleService = secMgr.roleServices.get("default");
        GeoServerUserGroupService oldUgService = secMgr.userGroupServices.get("default");
        PasswordValidator oldPwValidator = secMgr.passwordValidators.get("default");

        assertNotNull(oldRoleService);
        assertNotNull(oldUgService);
        assertNotNull(oldPwValidator);

        // reload calls init() which calls clearCaches(), then re-initializes
        secMgr.reload();

        // after reload, loading services again must produce fresh instances
        secMgr.loadRoleService("default");
        secMgr.loadUserGroupService("default");
        secMgr.loadPasswordValidator("default");

        // check the cache maps directly to avoid wrapper interference from the public load methods
        assertNotSame(
                "roleService should be a fresh instance after reload",
                oldRoleService,
                secMgr.roleServices.get("default"));
        assertNotSame(
                "userGroupService should be a fresh instance after reload",
                oldUgService,
                secMgr.userGroupServices.get("default"));
        assertNotSame(
                "passwordValidator should be a fresh instance after reload",
                oldPwValidator,
                secMgr.passwordValidators.get("default"));
    }

    @Test
    public void testGeoServerEnvParametrization() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        String oldRoleServiceName = config.getRoleServiceName();

        try {
            if (GeoServerEnvironment.allowEnvParametrization()) {
                System.setProperty("TEST_SYS_PROPERTY", oldRoleServiceName);

                config.setRoleServiceName("${TEST_SYS_PROPERTY}");
                secMgr.saveSecurityConfig(config);

                SecurityManagerConfig config1 = secMgr.loadSecurityConfig();
                assertEquals(config1.getRoleServiceName(), oldRoleServiceName);
            }
        } finally {
            config.setRoleServiceName(oldRoleServiceName);
            secMgr.saveSecurityConfig(config);
            System.clearProperty("TEST_SYS_PROPERTY");
        }
    }
}
