/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class RuleFilterBuilderTest {

    @Test
    public void testFilterByUser() {
        RuleFilterBuilder ruleFilterBuilder = new RuleFilterBuilder(getGeofenceConfiguration());
        ruleFilterBuilder.withUser(getAuthentication());
        ruleFilterBuilder.withRequest(getRequest());
        ruleFilterBuilder.withLayer("states");
        ruleFilterBuilder.withWorkspace("topp");
        RuleFilter filter = ruleFilterBuilder.build();
        // filter by user is default and in role field we have ANY filter type
        assertEquals(filter.getRole().getType(), RuleFilter.SpecialFilterType.ANY.getRelatedType());
        assertEquals(filter.getUser().getText(), getAuthentication().getName());
        assertEquals(filter.getService().getText(), "WMS");
        assertEquals(filter.getRequest().getText(), "GETMAP");
        assertEquals(filter.getLayer().getText(), "states");
        assertEquals(filter.getWorkspace().getText(), "topp");
    }

    @Test
    public void testFilterByRole() {
        GeoFenceConfiguration configuration = getGeofenceConfiguration();
        configuration.setUseRolesToFilter(true);
        configuration.setAcceptedRoles("ROLE_ONE");
        RuleFilterBuilder ruleFilterBuilder = new RuleFilterBuilder(configuration);
        ruleFilterBuilder.withUser(getAuthentication());
        ruleFilterBuilder.withRequest(getRequest());
        ruleFilterBuilder.withLayer("states");
        ruleFilterBuilder.withWorkspace("topp");
        RuleFilter filter = ruleFilterBuilder.build();
        assertEquals(filter.getRole().getText(), "ROLE_ONE");
        assertEquals(filter.getUser().getText(), getAuthentication().getName());
        assertEquals(filter.getService().getText(), "WMS");
        assertEquals(filter.getRequest().getText(), "GETMAP");
        assertEquals(filter.getLayer().getText(), "states");
        assertEquals(filter.getWorkspace().getText(), "topp");
    }

    @Test
    public void testDefaults() {
        RuleFilterBuilder ruleFilterBuilder = new RuleFilterBuilder(getGeofenceConfiguration());
        ruleFilterBuilder.withUser(getAuthentication());
        ruleFilterBuilder.withRequest(getRequest());
        RuleFilter filter = ruleFilterBuilder.build();
        assertEquals(filter.getRole().getType(), RuleFilter.SpecialFilterType.ANY.getRelatedType());
        assertEquals(filter.getUser().getText(), getAuthentication().getName());
        assertEquals(filter.getService().getText(), "WMS");
        assertEquals(filter.getRequest().getText(), "GETMAP");
        // no value provided should be set to default null
        assertNull(filter.getLayer().getText());
        assertNull(filter.getWorkspace().getText());
        // with type namevalue
        assertEquals(filter.getLayer().getType(), RuleFilter.FilterType.NAMEVALUE);
        assertEquals(filter.getWorkspace().getType(), RuleFilter.FilterType.NAMEVALUE);
    }

    private Request getRequest() {
        Request request = new Request();
        request.setService("WMS");
        request.setRequest("GETMAP");
        return request;
    }

    private GeoFenceConfiguration getGeofenceConfiguration() {
        GeoFenceConfiguration configuration = new GeoFenceConfiguration();
        configuration.setDefaultUserGroupServiceName("deafault");
        configuration.setInstanceName("geoserver");
        return configuration;
    }

    private Authentication getAuthentication() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "username",
                        "password",
                        Arrays.asList(
                                new SimpleGrantedAuthority("ROLE_ONE"),
                                new SimpleGrantedAuthority("ROLE_TWO")));
        return authentication;
    }
}
