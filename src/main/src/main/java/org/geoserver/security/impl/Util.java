/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.geoserver.security.validation.PasswordPolicyException;


/**
 * Class for common methods
 * 
 * 
 * @author christian
 *
 */
public class Util {
    
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");
    /**
     * Convert from string to boolean, use defaultValue
     * in case of null or empty string
     * 
     * @param booleanString
     * @param defaultValue
     * @return
     */
    static public boolean convertToBoolean(String booleanString, boolean defaultValue) {        
        if (booleanString == null || booleanString.trim().length()==0)
            return defaultValue;
        return Boolean.valueOf(booleanString.trim());
    }

    /**
     * Checks if users can be copied from one {@link GeoServerUserGroupService} to another.
     * This depends on the password encoding
     * 
     * @param service
     * @param store
     * @return
     */
    static public boolean copyPossible(GeoServerUserGroupService service, GeoServerUserGroupStore store) {
        
        GeoServerPasswordEncoder from = (GeoServerPasswordEncoder)
            service.getSecurityManager().loadPasswordEncoder(service.getPasswordEncoderName());
        if (from.getEncodingType()==PasswordEncodingType.PLAIN || 
                from.getEncodingType()==PasswordEncodingType.ENCRYPT)
            return true;
        
        return service.getPasswordEncoderName().equals(store.getPasswordEncoderName());
    }
    
    /**
     * Deep copy of the whole User/Group database
     * 
     * @param service
     * @param store
     * @throws IOException
     */
    static public void copyFrom(GeoServerUserGroupService service, GeoServerUserGroupStore store) throws IOException,PasswordPolicyException {
        
        
        if (copyPossible(service, store)==false)
            throw new IOException("Cannot copy user/group data, passwords encoders are not compatible");
        
        GeoServerPasswordEncoder storeEncoder = null;
        GeoServerPasswordEncoder serviceEncoder = 
            service.getSecurityManager().loadPasswordEncoder(service.getPasswordEncoderName());

        serviceEncoder.initializeFor(service);
        if (serviceEncoder.getEncodingType()==PasswordEncodingType.PLAIN || 
                serviceEncoder.getEncodingType()==PasswordEncodingType.ENCRYPT) {
            storeEncoder = 
                service.getSecurityManager().loadPasswordEncoder(store.getPasswordEncoderName());
            storeEncoder.initializeFor(store);
        }

        store.clear();
        Map<String,GeoServerUser> newUserDict = new HashMap<String,GeoServerUser>();
        Map<String,GeoServerUserGroup> newGroupDict = new HashMap<String,GeoServerUserGroup>();
        
        for (GeoServerUser user : service.getUsers()) {
            GeoServerUser newUser = store.createUserObject(user.getUsername(),user.getPassword(), user.isEnabled());
            if (storeEncoder!=null) {
                String plainText =serviceEncoder.decode(user.getPassword());
                newUser.setPassword(storeEncoder.encodePassword(plainText, null));
            }
            for (Object key: user.getProperties().keySet()) {
                newUser.getProperties().put(key, user.getProperties().get(key));
            }
            store.addUser(newUser);
            newUserDict.put(newUser.getUsername(),newUser);
        }
        for (GeoServerUserGroup group : service.getUserGroups()) {
            GeoServerUserGroup newGroup = store.createGroupObject(group.getGroupname(),group.isEnabled());
            store.addGroup(newGroup);
            newGroupDict.put(newGroup.getGroupname(),newGroup);
        }
        for (GeoServerUserGroup group : service.getUserGroups()) {
            GeoServerUserGroup newGroup = newGroupDict.get(group.getGroupname());
            
            for (GeoServerUser member : service.getUsersForGroup(group)) {
                GeoServerUser newUser = newUserDict.get(member.getUsername());
                store.associateUserToGroup(newUser, newGroup);
            }
        }        
    }
    
    /**
     * Deep copy of the whole role database
     * 
     * @param service
     * @param store
     * @throws IOException
     */
    static public void copyFrom(GeoServerRoleService service, GeoServerRoleStore store) throws IOException {
        store.clear();
        Map<String,GeoServerRole> newRoleDict = new HashMap<String,GeoServerRole>();
        
        for (GeoServerRole role : service.getRoles()) {
            GeoServerRole newRole = store.createRoleObject(role.getAuthority());
            for (Object key: role.getProperties().keySet()) {
                newRole.getProperties().put(key, role.getProperties().get(key));
            }
            store.addRole(newRole);
            newRoleDict.put(newRole.getAuthority(),newRole);
        }
        
        for (GeoServerRole role : service.getRoles()) {
            GeoServerRole parentRole = service.getParentRole(role);
            GeoServerRole newRole = newRoleDict.get(role.getAuthority());
            GeoServerRole newParentRole = parentRole == null ?
                    null : newRoleDict.get(parentRole.getAuthority());
            store.setParentRole(newRole, newParentRole);
        }
        
        for (GeoServerRole role : service.getRoles()) {
            GeoServerRole newRole = newRoleDict.get(role.getAuthority());
            SortedSet<String> usernames = service.getUserNamesForRole(role);
            for (String username : usernames) {
                store.associateRoleToUser(newRole, username);
            }
            SortedSet<String> groupnames = service.getGroupNamesForRole(role);
            for (String groupname : groupnames) {
                store.associateRoleToGroup(newRole, groupname);
            }            
        }                        
    }
    
    static SortedSet<GeoServerUser> usersHavingRole(GeoServerRole role) {
        // TODO
        return null;
    }

    public static String convertPropsToString(Properties props, String heading) {
        StringBuffer buff = new StringBuffer();
        if (heading !=null) {
            buff.append(heading).append("\n\n");
        }
        for (Entry<Object,Object> entry : props.entrySet()) {
            buff.append(entry.getKey().toString()).append(": ")
                .append(entry.getValue().toString()).append("\n");
        }
        return buff.toString();
    }

    /**
     * Determines if the the input stream is xml
     * if it is, use create properties loaded from xml
     * format, otherwise create properties from default
     * format.
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static Properties loadUniversal(InputStream in) throws IOException {
        final String xmlDeclarationStart = "<?xml";
        BufferedInputStream bin = new BufferedInputStream(in);
        bin.mark(4096);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(bin));
        String line = reader.readLine();                
        boolean isXML = line.startsWith(xmlDeclarationStart);
        
        bin.reset();        
        Properties props = new Properties();
        
        if (isXML)
            props.loadFromXML(bin);
        else
            props.load(bin);
                
        return props;
    }

    /**
     * Reads a property file.
     * <p>
     * This method delegates to {@link #loadUniversal(InputStream)}.
     * </p>
     */
    public static Properties loadPropertyFile(File f) throws IOException {
        FileInputStream fin = new FileInputStream(f);
        try {
            return loadUniversal(fin);
        }
        finally {
            fin.close();
        }
    }
}
