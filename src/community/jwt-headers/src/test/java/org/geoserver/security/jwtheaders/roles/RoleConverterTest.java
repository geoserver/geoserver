/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.junit.Assert;
import org.junit.Test;

public class RoleConverterTest {

    /**
     * tests parsing of the roleConverter string
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These check
     * for empty maps
     */
    @Test
    public void testParseNull() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setRoleConverterString(null);
        Map<String, String> map = config.getRoleConverterAsMap();
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
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These
     * checks simple (correct) inputs
     */
    @Test
    public void testParseSimple() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setRoleConverterString("a=b");
        Map<String, String> map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals("b", map.get("a"));

        config.setRoleConverterString("a=b;c=d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals("b", map.get("a"));
        Assert.assertTrue(map.containsKey("c"));
        Assert.assertEquals("d", map.get("c"));
    }

    /**
     * tests parsing of the roleConverter string
     * format:externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2" These
     * checks bad inputs
     */
    @Test
    public void testParseBad() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        // bad format
        config.setRoleConverterString("a=b;c=;d");
        Map<String, String> map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals("b", map.get("a"));

        // bad chars
        config.setRoleConverterString("a= b** ;c=**;d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals("b", map.get("a"));

        // removes html tags
        config.setRoleConverterString("a= <script> ;c=**;d");
        map = config.getRoleConverterAsMap();
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertEquals("script", map.get("a"));
    }

    /** Tests simple conversion, with setOnlyExternalListedRoles(false); */
    @Test
    public void testConversionAllExternals() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setRoleConverterString("a=b;c=d");
        config.setOnlyExternalListedRoles(false);

        RoleConverter roleConverter = new RoleConverter(config);
        List<String> externalRoles = Arrays.asList("a", "c");
        List<GeoServerRole> internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(2, internalRoles.size());
        Assert.assertEquals("b", internalRoles.get(0).getAuthority());
        Assert.assertEquals("d", internalRoles.get(1).getAuthority());

        // dddddd = non-existant internal roles (but thats okay -
        // config.setOnlyExternalListedRoles(false);)
        externalRoles = Arrays.asList("ddddddd", "c");
        internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(2, internalRoles.size());

        Assert.assertEquals("ddddddd", internalRoles.get(0).getAuthority());
        Assert.assertEquals("d", internalRoles.get(1).getAuthority());
    }

    /** Tests simple conversion, with setOnlyExternalListedRoles(true); */
    @Test
    public void testConversionOnlyMapped() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setRoleConverterString("a=b;c=d");
        config.setOnlyExternalListedRoles(true);

        RoleConverter roleConverter = new RoleConverter(config);
        List<String> externalRoles = Arrays.asList("ddddddd", "c");
        List<GeoServerRole> internalRoles = roleConverter.convert(externalRoles);
        Assert.assertEquals(1, internalRoles.size());

        Assert.assertEquals("d", internalRoles.get(0).getAuthority());
    }
}
