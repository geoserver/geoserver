/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Unit tests for {@link GeoServerOAuth2ClientRegistrationRegistry}.
 *
 * <p>These cover the invariants the rest of the OIDC plugin relies on for the shared-registry fix to the Spring 7
 * multi-filter HTTP 500 bug:
 *
 * <ul>
 *   <li>contributions are keyed by filter name and atomically replaceable;
 *   <li>cross-filter lookup of scoped registration IDs always succeeds while the contribution is live;
 *   <li>removal / retain semantics drop entries cleanly so a deleted filter's old client credentials cannot resolve.
 * </ul>
 */
public class GeoServerOAuth2ClientRegistrationRegistryTest {

    private GeoServerOAuth2ClientRegistrationRegistry sut;

    @Before
    public void setUp() {
        sut = new GeoServerOAuth2ClientRegistrationRegistry();
    }

    private static ClientRegistration registrationFor(String scopedRegId) {
        // Build a minimally-valid ClientRegistration. The fields we care about for the registry contract are the
        // registrationId (used as the key) and clientId (used to verify equality in some tests). Everything else is
        // a placeholder.
        return ClientRegistration.withRegistrationId(scopedRegId)
                .clientId("client-" + scopedRegId)
                .clientSecret("secret-" + scopedRegId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.com/cb/" + scopedRegId)
                .authorizationUri("https://idp.example.com/auth")
                .tokenUri("https://idp.example.com/token")
                .build();
    }

    @Test
    public void registryStartsEmpty() {
        assertEquals(0, sut.size());
        assertEquals(0, sut.filterCount());
        assertNull(sut.findByRegistrationId("anything"));
        assertNull(sut.findByRegistrationId(null));
    }

    @Test
    public void replaceFilterRegistrations_addsAndLookupSucceeds() {
        ClientRegistration cr = registrationFor("keycloak-prod__oidc");
        sut.replaceFilterRegistrations("keycloak-prod", List.of(cr));

        assertEquals(1, sut.size());
        assertEquals(1, sut.filterCount());
        ClientRegistration found = sut.findByRegistrationId("keycloak-prod__oidc");
        assertNotNull(found);
        assertEquals("client-keycloak-prod__oidc", found.getClientId());
    }

    @Test
    public void replaceFilterRegistrations_isAtomicForTheSameFilter() {
        // Initial contribution: one OIDC registration.
        sut.replaceFilterRegistrations("entra", List.of(registrationFor("entra__oidc")));
        assertEquals(1, sut.size());

        // Refresh: same filter now contributes a different provider. The old OIDC registration must be removed,
        // the new Microsoft one inserted, and the per-filter count must remain 1.
        sut.replaceFilterRegistrations("entra", List.of(registrationFor("entra__microsoft")));
        assertEquals(1, sut.size());
        assertEquals(1, sut.filterCount());
        assertNull("old OIDC registration must have been purged", sut.findByRegistrationId("entra__oidc"));
        assertNotNull("new Microsoft registration must be visible", sut.findByRegistrationId("entra__microsoft"));
    }

    @Test
    public void multipleFilters_independentRegistrationsAllLookupable() {
        // The core multi-filter invariant: every contributing filter's registration must be resolvable, regardless
        // of who registered it first or last. Without this, Spring's OAuth2 redirect filter inside one filter's
        // sub-chain throws InvalidClientRegistrationIdException when asked about a sibling's scoped id (the very bug
        // the registry exists to fix).
        sut.replaceFilterRegistrations("keycloak-prod", List.of(registrationFor("keycloak-prod__oidc")));
        sut.replaceFilterRegistrations("auth0-staging", List.of(registrationFor("auth0-staging__oidc")));
        sut.replaceFilterRegistrations("entra-tenant", List.of(registrationFor("entra-tenant__microsoft")));

        assertEquals(3, sut.size());
        assertEquals(3, sut.filterCount());
        assertNotNull(sut.findByRegistrationId("keycloak-prod__oidc"));
        assertNotNull(sut.findByRegistrationId("auth0-staging__oidc"));
        assertNotNull(sut.findByRegistrationId("entra-tenant__microsoft"));
    }

    @Test
    public void removeFilterRegistrations_dropsAllOfThatFiltersEntries() {
        sut.replaceFilterRegistrations(
                "multi-provider",
                Arrays.asList(registrationFor("multi-provider__oidc"), registrationFor("multi-provider__microsoft")));
        sut.replaceFilterRegistrations("survivor", List.of(registrationFor("survivor__oidc")));
        assertEquals(3, sut.size());

        sut.removeFilterRegistrations("multi-provider");

        assertEquals(1, sut.size());
        assertEquals(1, sut.filterCount());
        assertNull(sut.findByRegistrationId("multi-provider__oidc"));
        assertNull(sut.findByRegistrationId("multi-provider__microsoft"));
        assertNotNull("unrelated filter must survive removal of a sibling", sut.findByRegistrationId("survivor__oidc"));
    }

    @Test
    public void replaceWithNullOrEmpty_isEquivalentToRemove() {
        sut.replaceFilterRegistrations("filter", List.of(registrationFor("filter__oidc")));
        assertEquals(1, sut.size());

        sut.replaceFilterRegistrations("filter", null);
        assertEquals("null collection must purge the filter's entries", 0, sut.size());

        sut.replaceFilterRegistrations("filter", List.of(registrationFor("filter__oidc")));
        sut.replaceFilterRegistrations("filter", List.of());
        assertEquals("empty collection must purge the filter's entries", 0, sut.size());
    }

    @Test
    public void retainFilters_dropsContributionsNotInTheRetainSet() {
        sut.replaceFilterRegistrations("keep-a", List.of(registrationFor("keep-a__oidc")));
        sut.replaceFilterRegistrations("keep-b", List.of(registrationFor("keep-b__oidc")));
        sut.replaceFilterRegistrations("drop-c", List.of(registrationFor("drop-c__oidc")));
        sut.replaceFilterRegistrations("drop-d", List.of(registrationFor("drop-d__oidc")));
        assertEquals(4, sut.size());

        sut.retainFilters(new HashSet<>(Arrays.asList("keep-a", "keep-b")));

        assertEquals(2, sut.size());
        assertNotNull(sut.findByRegistrationId("keep-a__oidc"));
        assertNotNull(sut.findByRegistrationId("keep-b__oidc"));
        assertNull(sut.findByRegistrationId("drop-c__oidc"));
        assertNull(sut.findByRegistrationId("drop-d__oidc"));
    }

    @Test
    public void iterator_returnsSnapshotOfCurrentRegistrations() {
        sut.replaceFilterRegistrations("a", List.of(registrationFor("a__oidc")));
        sut.replaceFilterRegistrations("b", List.of(registrationFor("b__oidc")));

        Iterator<ClientRegistration> it = sut.iterator();
        int count = 0;
        while (it.hasNext()) {
            ClientRegistration cr = it.next();
            assertTrue(
                    "unexpected registration in iteration: " + cr.getRegistrationId(),
                    "a__oidc".equals(cr.getRegistrationId()) || "b__oidc".equals(cr.getRegistrationId()));
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void nullFilterName_isIgnored() {
        // Defensive: a null filter name during a corrupted save should not blow up the registry. Existing entries
        // must be preserved.
        sut.replaceFilterRegistrations("real", List.of(registrationFor("real__oidc")));
        sut.replaceFilterRegistrations(null, List.of(registrationFor("ghost__oidc")));

        assertEquals(1, sut.size());
        assertNotNull(sut.findByRegistrationId("real__oidc"));
        assertNull(sut.findByRegistrationId("ghost__oidc"));
    }
}
