/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.AccessTypeDTO;
import org.geofence.core.services.dto.CatalogModeDTO;
import org.geofence.core.services.dto.GrantTypeDTO;
import org.geofence.core.services.dto.LayerAttributeDTO;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geofence.web.rest.api.interfaces.params.RESTRuleFilter;
import org.geofence.web.rest.api.model.RESTAccessInfo;
import org.geofence.web.rest.api.model.RESTLayerAttribute;
import org.geofence.web.rest.api.model.RESTShortRule;
import org.geofence.web.rest.api.model.enums.RESTAccessType;
import org.geofence.web.rest.api.model.enums.RESTCatalogMode;
import org.geofence.web.rest.api.model.enums.RESTGrantType;
import org.junit.Test;

/** Unit tests for the DTO<->REST conversion logic (no network involved). */
public class RestRuleReaderServiceTest {

    @Test
    public void testToRestFilterAny() {
        RuleFilter filter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
        RESTRuleFilter query = RestRuleReaderService.toRestFilter(filter);

        assertNull(query.userName);
        assertNull(query.userDefault);
        assertNull(query.instanceId);
        assertNull(query.instanceName);
        assertNull(query.instanceDefault);
    }

    @Test
    public void testToRestFilterDefaultByDefault() {
        RuleFilter filter = new RuleFilter();
        RESTRuleFilter query = RestRuleReaderService.toRestFilter(filter);

        assertNull(query.userName);
        assertTrue(query.userDefault);
        assertNull(query.instanceId);
        assertNull(query.instanceName);
        assertTrue(query.instanceDefault);
    }

    @Test
    public void testToRestFilterNameValue() {
        RuleFilter filter = new RuleFilter();
        filter.setUser("bob");
        filter.setWorkspace("topp");
        filter.setLayer("states");

        RESTRuleFilter query = RestRuleReaderService.toRestFilter(filter);

        assertEquals("bob", query.userName);
        assertTrue(query.userDefault);
        assertEquals("topp", query.workspace);
        assertEquals("states", query.layer);
    }

    @Test
    public void testToRestFilterInstanceById() {
        RuleFilter filter = new RuleFilter();
        filter.setInstance(42L);

        RESTRuleFilter query = RestRuleReaderService.toRestFilter(filter);

        assertEquals(Long.valueOf(42L), query.instanceId);
        assertNull(query.instanceName);
    }

    @Test
    public void testToAccessInfo() {
        RESTAccessInfo in = new RESTAccessInfo();
        in.setGrant(RESTGrantType.LIMIT);
        in.setAdminRights(true);
        in.setAreaWkt("wkt-area");
        in.setClipAreaWkt("wkt-clip");
        in.setCatalogMode(RESTCatalogMode.HIDE);
        in.setDefaultStyle("style1");
        in.setCqlFilterRead("read=1");
        in.setCqlFilterWrite("write=1");
        in.setAllowedStyles(Set.of("s1", "s2"));
        in.setAttributes(Set.of(new RESTLayerAttribute("attr1", "java.lang.String", RESTAccessType.READONLY)));

        AccessInfo out = RestRuleReaderService.toAccessInfo(in);

        assertEquals(GrantTypeDTO.LIMIT, out.getGrant());
        assertTrue(out.getAdminRights());
        assertEquals("wkt-area", out.getAreaWkt());
        assertEquals("wkt-clip", out.getClipAreaWkt());
        assertEquals(CatalogModeDTO.HIDE, out.getCatalogMode());
        assertEquals("style1", out.getDefaultStyle());
        assertEquals("read=1", out.getCqlFilterRead());
        assertEquals("write=1", out.getCqlFilterWrite());
        assertEquals(Set.of("s1", "s2"), out.getAllowedStyles());
        assertEquals(1, out.getAttributes().size());
        LayerAttributeDTO outAttr = out.getAttributes().iterator().next();
        assertEquals("attr1", outAttr.getName());
        assertEquals("java.lang.String", outAttr.getDatatype());
        assertEquals(AccessTypeDTO.READONLY, outAttr.getAccess());
    }

    @Test
    public void testToAccessInfoWithNullAttributes() {
        RESTAccessInfo in = new RESTAccessInfo();
        in.setGrant(RESTGrantType.DENY);

        AccessInfo out = RestRuleReaderService.toAccessInfo(in);

        assertEquals(GrantTypeDTO.DENY, out.getGrant());
        assertFalse(out.getAdminRights());
        assertNull(out.getAttributes());
    }

    @Test
    public void testToShortRule() {
        RESTShortRule in = new RESTShortRule();
        in.setId(1L);
        in.setPriority(10L);
        in.setUserName("bob");
        in.setRoleName("admins");
        in.setInstanceId(5L);
        in.setInstanceName("inst1");
        in.setAddressRange("10.0.0.0/8");
        in.setValidAfter("2026-01-01");
        in.setValidBefore("2026-12-31");
        in.setService("WMS");
        in.setRequest("GetMap");
        in.setSubfield("sub1");
        in.setWorkspace("topp");
        in.setLayer("states");
        in.setAccess(RESTGrantType.ALLOW);

        ShortRule out = RestRuleReaderService.toShortRule(in);

        assertEquals(Long.valueOf(1L), out.getId());
        assertEquals(10L, out.getPriority());
        assertEquals("bob", out.getUserName());
        assertEquals("admins", out.getRoleName());
        assertEquals(Long.valueOf(5L), out.getInstanceId());
        assertEquals("inst1", out.getInstanceName());
        assertEquals("10.0.0.0/8", out.getAddressRange());
        assertEquals("2026-01-01", out.getValidAfter());
        assertEquals("2026-12-31", out.getValidBefore());
        assertEquals("WMS", out.getService());
        assertEquals("GetMap", out.getRequest());
        assertEquals("sub1", out.getSubfield());
        assertEquals("topp", out.getWorkspace());
        assertEquals("states", out.getLayer());
        assertEquals(GrantTypeDTO.ALLOW, out.getAccess());
    }
}
