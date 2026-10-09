/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.junit.Test;

/**
 * Verifies that {@link KeycloakRoleServiceConfig} round-trips through the same XStream mechanism the generic
 * role-service REST controller relies on to (de)serialize polymorphic {@link SecurityRoleServiceConfig} payloads.
 */
public class KeycloakRoleServiceConfigXStreamTest {

    private XStreamPersister newPersister() {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.security.**", "org.geoserver.security.config.**"});
        new KeycloakSecurityProvider().configure(xp);
        return xp;
    }

    @Test
    public void testRoundTripThroughAlias() throws Exception {
        KeycloakRoleServiceConfig original = new KeycloakRoleServiceConfig();
        original.setName("myKeycloakRoles");
        original.setClassName(KeycloakRoleService.class.getName());
        original.setServerURL("https://keycloak.example.com");
        original.setRealm("myrealm");
        original.setClientID("geoserver");
        original.setClientSecret("s3cr3t");
        original.setAdminRoleName("ADMIN");
        original.setGroupAdminRoleName("GROUP_ADMIN");

        XStreamPersister xp = newPersister();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xp.save(original, out);
        String xml = out.toString(StandardCharsets.UTF_8);

        // the alias registered in KeycloakSecurityProvider.configure() is what lets the generic
        // RoleServiceController resolve the concrete config class from the request payload
        assertTrue(xml.contains("<keycloakRoleService>"));

        SecurityRoleServiceConfig loaded = xp.load(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), SecurityRoleServiceConfig.class);

        assertTrue(loaded instanceof KeycloakRoleServiceConfig);
        KeycloakRoleServiceConfig copy = (KeycloakRoleServiceConfig) loaded;
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getClassName(), copy.getClassName());
        assertEquals(original.getServerURL(), copy.getServerURL());
        assertEquals(original.getRealm(), copy.getRealm());
        assertEquals(original.getClientID(), copy.getClientID());
        assertEquals(original.getClientSecret(), copy.getClientSecret());
        assertEquals(original.getAdminRoleName(), copy.getAdminRoleName());
        assertEquals(original.getGroupAdminRoleName(), copy.getGroupAdminRoleName());
    }
}
