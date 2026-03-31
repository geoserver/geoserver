/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LDAPBaseSecurityServiceTest {

    String userName1 =
            "cn=Adm-AD-Blasby,ou=Administration,ou=Production,ou=Accounts,ou=ApplicationServerManagement,dc=ad,dc=geocat,dc=net";

    /** if groupName2UserName is null/blank, #groupName2UserName shouldn't change it */
    @Test
    public void testGroupName2UserName_blank() {
        LDAPBaseSecurityService service = new LDAPBaseSecurityService() {};

        service.groupMember2UserName = null;
        String uname = service.groupName2UserName(userName1);

        // null groupMember2UserName, no change
        assertEquals(uname, userName1);

        service.groupMember2UserName = "";
        uname = service.groupName2UserName(userName1);

        // blanks groupMember2UserName, no change
        assertEquals(uname, userName1);
    }

    /** this should extract the `cn=` from the username */
    @Test
    public void testGroupName2UserName_cn_goodcase() {
        LDAPBaseSecurityService service = new LDAPBaseSecurityService() {};

        service.groupMember2UserName = "cn";
        String uname = service.groupName2UserName(userName1);

        // should extract
        assertEquals("Adm-AD-Blasby", uname);
    }

    /** this should extract the `cn=` from the username, even though its the wrong case */
    @Test
    public void testGroupName2UserName_cn_wrongcase() {
        LDAPBaseSecurityService service = new LDAPBaseSecurityService() {};

        service.groupMember2UserName = "CN";
        String uname = service.groupName2UserName(userName1);

        // should extract
        assertEquals("Adm-AD-Blasby", uname);
    }

    /** `XYZ` - this isn't part of the username, so shouldn't modify it */
    @Test
    public void testGroupName2UserName_xyz() {
        LDAPBaseSecurityService service = new LDAPBaseSecurityService() {};

        service.groupMember2UserName = "XYZ";
        String uname = service.groupName2UserName(userName1);

        // shouldn't change
        assertEquals(userName1, uname);
    }
}
