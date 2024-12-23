/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class RoleConverterTest {

    /**
     * tests parsing of the roleConverter string
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These check for empty maps
     */
    @Test
    public void testParseNull() {
        JwtConfiguration config = new JwtConfiguration();

        config.setRoleConverterString(null);
        Map<String, List<String>> map = config.getRoleConverterAsMap();
        Assert.assertEquals(0, map.size());

        config.setRoleConverterString("");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(0, map.size());

        config.setRoleConverterString("adadadfafdasdf");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(0, map.size());

        config.setRoleConverterString("adadadfafdasdf=");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(0, map.size());
    }

    /**
     * tests parsing of the roleConverter string
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These checks simple (correct)
     * inputs
     */
    @Test
    public void testParseSimple() {
        JwtConfiguration config = new JwtConfiguration();

        config.setRoleConverterString("a=b");
        Map<String, List<String>> map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals(Arrays.asList("b"), map.get("a"));

        config.setRoleConverterString("a=b;c=d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals(Arrays.asList("b"), map.get("a"));
        Assert.assertTrue(map.containsKey("c"));
        Assert.assertEquals(Arrays.asList("d"), map.get("c"));
    }

    /**
     * tests parsing of the roleConverter string
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These checks bad inputs
     */
    @Test
    public void testParseBad() {
        JwtConfiguration config = new JwtConfiguration();

        // bad format
        config.setRoleConverterString("a=b;c=;d");
        Map<String, List<String>> map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals(Arrays.asList("b"), map.get("a"));

        // bad chars
        config.setRoleConverterString("a= b** ;c=**;d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals(Arrays.asList("b"), map.get("a"));

        // removes html tags
        config.setRoleConverterString("a= <script> ;c=**;d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals(Arrays.asList("script"), map.get("a"));
    }

    /** Tests simple conversion, with setOnlyExternalListedRoles(false); */
    @Test
    public void testConversionAllExternals() {
        JwtConfiguration config = new JwtConfiguration();
        config.setRoleConverterString("a=b;c=d");
        config.setOnlyExternalListedRoles(false);

        RoleConverter roleConverter = new RoleConverter(config);
        List<String> externalRoles = Arrays.asList("a", "c");
        List<String> internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(2, internalRoles.size());
        Assert.assertEquals("b", internalRoles.get(0));
        Assert.assertEquals("d", internalRoles.get(1));

        // dddddd = non-existant internal roles (but thats okay -
        // config.setOnlyExternalListedRoles(false);)
        externalRoles = Arrays.asList("ddddddd", "c");
        internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(2, internalRoles.size());

        Assert.assertEquals("ddddddd", internalRoles.get(0));
        Assert.assertEquals("d", internalRoles.get(1));
    }

    /** Tests simple conversion, with setOnlyExternalListedRoles(true); */
    @Test
    public void testConversionOnlyMapped() {
        JwtConfiguration config = new JwtConfiguration();
        config.setRoleConverterString("a=b;c=d");
        config.setOnlyExternalListedRoles(true);

        RoleConverter roleConverter = new RoleConverter(config);
        List<String> externalRoles = Arrays.asList("ddddddd", "c");
        List<String> internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(1, internalRoles.size());

        Assert.assertEquals("d", internalRoles.get(0));
    }

    /**
     * Test for creating multiple roles. purpose - make sure that a user can get multiple GS roles from a single OIDC
     * role.
     */
    @Test
    public void testMultipleRoles() {
        JwtConfiguration config = new JwtConfiguration();
        config.setRoleConverterString("a=ROLE_ADMINISTRATOR;a=ADMIN");
        config.setOnlyExternalListedRoles(true);

        RoleConverter roleConverter = new RoleConverter(config);
        List<String> externalRoles = Arrays.asList("a");
        List<String> internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(2, internalRoles.size());

        Assert.assertEquals("ROLE_ADMINISTRATOR", internalRoles.get(0));
        Assert.assertEquals("ADMIN", internalRoles.get(1));
    }
}
