package org.geoserver.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geoserver.security.GeoServerRoleConverter;
import org.springframework.security.core.GrantedAuthority;


public class GeoServerRoleConverterImplTest extends TestCase {

    private GeoServerRoleConverter converter;

    @Override
    protected void setUp() throws Exception {
        converter = new GeoServerRoleConverterImpl();
    }

    public void testConverter() {
        GeoServerRole r1 = new GeoServerRole("r1");
        r1.getProperties().setProperty("r1_p1", "r1_v1");
        r1.getProperties().setProperty("r1_p2", "r1_v2");
        GeoServerRole r2 = new GeoServerRole("r2");
        r2.getProperties().setProperty("r2_p1", "r2_v1");
        GeoServerRole r3 = new GeoServerRole("r3");
        
        GeoServerRole r =converter.convertRoleFromString(converter.convertRoleToString(r1),"testuser");
        
        assertEquals("r1", r.getAuthority());
        assertEquals(2, r.getProperties().size());
        assertEquals("r1_v1" ,r.getProperties().get("r1_p1"));
        assertEquals("r1_v2" ,r.getProperties().get("r1_p2"));
        assertEquals("testuser",r.getUserName());
        
        List<GeoServerRole> list = new ArrayList<GeoServerRole>();
        
        list.add(r1);
        list.add(r2);
        list.add(r3);
        
        Collection<GeoServerRole> resColl = 
                converter.convertRolesFromString(converter.convertRolesToString(list),null);
        
        assertEquals(3,resColl.size());
        for (GrantedAuthority auth : resColl) {
            r = (GeoServerRole) auth;
            assertNull(r.getUserName());
            if ("r3".equals(r.getAuthority())) continue;
            if ("r2".equals(r.getAuthority())) {
                assertEquals(1, r.getProperties().size());
                assertEquals("r2_v1" ,r.getProperties().get("r2_p1"));                
                continue;                
            }
            if ("r1".equals(r.getAuthority())) {
                assertEquals(2, r.getProperties().size());
                assertEquals("r1_v1" ,r.getProperties().get("r1_p1"));
                assertEquals("r1_v2" ,r.getProperties().get("r1_p2"));
                continue;                
            }
            Assert.fail("Unexpected role: "+ r.getAuthority());
        }
        
        assertNull(converter.convertRoleFromString("  ", null));
        assertEquals(0,converter.convertRolesFromString("  ", null).size());
        
        resColl.clear();
        assertEquals(0,converter.convertRolesToString(resColl).length());
    }
    
    
    
}
