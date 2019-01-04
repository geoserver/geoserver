/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

/**
 * Holding XML Constants for Element and AttributeNames
 *
 * @author christian
 */
public class XMLConstants {

    public static final String NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String NSP_XMLSCHEMA = "xsi";

    public static final String FILE_UR = "users.xml";
    public static final String FILE_RR = "roles.xml";
    public static final String FILE_UR_SCHEMA = "users.xsd";
    public static final String FILE_RR_SCHEMA = "roles.xsd";

    /** Namespace Prefix for User Registry */
    public static final String NS_UR = "http://www.geoserver.org/security/users";

    public static final String NSP_UR = "gsu";
    public static final String SCHEMA_UR = NS_UR + " " + FILE_UR_SCHEMA;
    public static final String VERSION_UR_1_0 = "1.0";
    public static final String E_PROPERTY_UR = "property";
    public static final String A_PROPERTY_NAME_UR = "name";

    public static final String E_USERREGISTRY_UR = "userRegistry";
    public static final String A_VERSION_UR = "version";

    public static final String E_USERS_UR = "users";
    public static final String E_GROUPS_UR = "groups";
    public static final String E_USER_UR = "user";
    public static final String A_USER_NAME_UR = "name";
    public static final String A_USER_PASSWORD_UR = "password";
    public static final String A_USER_ENABLED_UR = "enabled";

    public static final String E_GROUP_UR = "group";
    public static final String A_GROUP_NAME_UR = "name";
    public static final String A_GROUP_ENABLED_UR = "enabled";
    public static final String E_MEMBER_UR = "member";
    public static final String A_MEMBER_NAME_UR = "username";

    /** Namespace Prefix for Role Registry */
    public static final String NS_RR = "http://www.geoserver.org/security/roles";

    public static final String NSP_RR = "gsr";
    public static final String SCHEMA_RR = NS_RR + " " + FILE_RR_SCHEMA;
    public static final String E_ROLEREGISTRY_RR = "roleRegistry";
    public static final String VERSION_RR_1_0 = "1.0";
    public static final String A_VERSION_RR = "version";
    public static final String E_PROPERTY_RR = "property";
    public static final String A_PROPERTY_NAME_RR = "name";

    public static final String E_ROLELIST_RR = "roleList";
    public static final String E_ROLE_RR = "role";
    public static final String A_ROLEID_RR = "id";
    public static final String A_PARENTID_RR = "parentID";

    public static final String E_USERLIST_RR = "userList";
    public static final String E_USERROLES_RR = "userRoles";
    public static final String A_USERNAME_RR = "username";
    public static final String E_ROLEREF_RR = "roleRef";
    public static final String A_ROLEREFID_RR = "roleID";

    public static final String E_GROUPLIST_RR = "groupList";
    public static final String E_GROUPROLES_RR = "groupRoles";
    public static final String A_GROUPNAME_RR = "groupname";
}
