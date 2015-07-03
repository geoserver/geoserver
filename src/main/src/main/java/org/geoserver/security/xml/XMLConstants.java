/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;


/**
 * Holding XML Constants for Element and AttributeNames
 * 
 * 
 * @author christian
 *
 */
public class XMLConstants {


    public final static String NS_XMLSCHEMA ="http://www.w3.org/2001/XMLSchema-instance";
    public final static String NSP_XMLSCHEMA = "xsi";
    
    
    public final static String FILE_UR = "users.xml";
    public final static String FILE_RR = "roles.xml";
    public final static String FILE_UR_SCHEMA="users.xsd";
    public final static String FILE_RR_SCHEMA="roles.xsd";
    
    /**
     * Namespace Prefix for User Registry
     */
    public final static String NS_UR="http://www.geoserver.org/security/users";
    public final static String NSP_UR="gsu";
    public final static String SCHEMA_UR=NS_UR+" "+FILE_UR_SCHEMA;
    public final static String VERSION_UR_1_0="1.0";
    public final static String E_PROPERTY_UR = "property";
    public final static String A_PROPERTY_NAME_UR = "name";


    public final static String E_USERREGISTRY_UR = "userRegistry";
    public final static String A_VERSION_UR = "version";
    
    public final static String E_USERS_UR = "users";
    public final static String E_GROUPS_UR = "groups";
    public final static String E_USER_UR = "user";
    public final static String A_USER_NAME_UR = "name";
    public final static String A_USER_PASSWORD_UR = "password";
    public final static String A_USER_ENABLED_UR = "enabled";
    
    
    public final static String E_GROUP_UR = "group";
    public final static String A_GROUP_NAME_UR ="name";
    public final static String A_GROUP_ENABLED_UR = "enabled";
    public final static String E_MEMBER_UR = "member";
    public final static String A_MEMBER_NAME_UR = "username";
    
    
    /**
     * Namespace Prefix for Role Registry
     */
    public final static String NS_RR="http://www.geoserver.org/security/roles";    
    public final static String NSP_RR="gsr";        
    public final static String SCHEMA_RR=NS_RR+" "+FILE_RR_SCHEMA;
    public final static String E_ROLEREGISTRY_RR = "roleRegistry";
    public final static String VERSION_RR_1_0="1.0";
    public final static String A_VERSION_RR = "version";
    public final static String E_PROPERTY_RR = "property";
    public final static String A_PROPERTY_NAME_RR = "name";

    
    public final static String E_ROLELIST_RR = "roleList";
    public final static String E_ROLE_RR = "role";
    public final static String A_ROLEID_RR = "id";
    public final static String A_PARENTID_RR = "parentID";
    
    public final static String E_USERLIST_RR = "userList";
    public final static String E_USERROLES_RR = "userRoles";
    public final static String A_USERNAME_RR = "username";
    public final static String E_ROLEREF_RR = "roleRef";
    public final static String A_ROLEREFID_RR = "roleID";
    
    public final static String E_GROUPLIST_RR = "groupList";
    public final static String E_GROUPROLES_RR = "groupRoles";
    public final static String A_GROUPNAME_RR = "groupname";
    
}
