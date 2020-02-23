/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
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
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.password.GeoServerMultiplexingPasswordEncoder;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * Class for common methods
 *
 * @author christian
 */
public class Util {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");
    /** Convert from string to boolean, use defaultValue in case of null or empty string */
    public static boolean convertToBoolean(String booleanString, boolean defaultValue) {
        if (booleanString == null || booleanString.trim().length() == 0) return defaultValue;
        return Boolean.valueOf(booleanString.trim());
    }

    /** Deep copy of the whole User/Group database */
    public static void copyFrom(GeoServerUserGroupService service, GeoServerUserGroupStore store)
            throws IOException, PasswordPolicyException {

        GeoServerPasswordEncoder encoder =
                store.getSecurityManager().loadPasswordEncoder(store.getPasswordEncoderName());
        encoder.initializeFor(store);

        GeoServerMultiplexingPasswordEncoder mEncoder =
                new GeoServerMultiplexingPasswordEncoder(store.getSecurityManager(), service);

        store.clear();
        Map<String, GeoServerUser> newUserDict = new HashMap<String, GeoServerUser>();
        Map<String, GeoServerUserGroup> newGroupDict = new HashMap<String, GeoServerUserGroup>();

        for (GeoServerUser user : service.getUsers()) {

            String rawPassword = null;
            String encPassword = null;
            try {
                rawPassword = mEncoder.decode(user.getPassword());
                encPassword = encoder.encodePassword(rawPassword, null);
            } catch (UnsupportedOperationException ex) {
                LOGGER.warning(
                        "Cannot recode user: "
                                + user.getUsername()
                                + " password: "
                                + user.getPassword());
                encPassword = user.getPassword();
            }
            GeoServerUser newUser =
                    store.createUserObject(user.getUsername(), encPassword, user.isEnabled());
            for (Object key : user.getProperties().keySet()) {
                newUser.getProperties().put(key, user.getProperties().get(key));
            }
            store.addUser(newUser);
            newUserDict.put(newUser.getUsername(), newUser);
        }
        for (GeoServerUserGroup group : service.getUserGroups()) {
            GeoServerUserGroup newGroup =
                    store.createGroupObject(group.getGroupname(), group.isEnabled());
            store.addGroup(newGroup);
            newGroupDict.put(newGroup.getGroupname(), newGroup);
        }
        for (GeoServerUserGroup group : service.getUserGroups()) {
            GeoServerUserGroup newGroup = newGroupDict.get(group.getGroupname());

            for (GeoServerUser member : service.getUsersForGroup(group)) {
                GeoServerUser newUser = newUserDict.get(member.getUsername());
                store.associateUserToGroup(newUser, newGroup);
            }
        }
    }

    /** Deep copy of the whole role database */
    public static void copyFrom(GeoServerRoleService service, GeoServerRoleStore store)
            throws IOException {
        store.clear();
        Map<String, GeoServerRole> newRoleDict = new HashMap<String, GeoServerRole>();

        for (GeoServerRole role : service.getRoles()) {
            GeoServerRole newRole = store.createRoleObject(role.getAuthority());
            for (Object key : role.getProperties().keySet()) {
                newRole.getProperties().put(key, role.getProperties().get(key));
            }
            store.addRole(newRole);
            newRoleDict.put(newRole.getAuthority(), newRole);
        }

        for (GeoServerRole role : service.getRoles()) {
            GeoServerRole parentRole = service.getParentRole(role);
            GeoServerRole newRole = newRoleDict.get(role.getAuthority());
            GeoServerRole newParentRole =
                    parentRole == null ? null : newRoleDict.get(parentRole.getAuthority());
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

    public static String convertPropsToString(Properties props, String heading) {
        StringBuffer buff = new StringBuffer();
        if (heading != null) {
            buff.append(heading).append("\n\n");
        }
        for (Entry<Object, Object> entry : props.entrySet()) {
            buff.append(entry.getKey().toString())
                    .append(": ")
                    .append(entry.getValue().toString())
                    .append("\n");
        }
        return buff.toString();
    }

    /**
     * Determines if the the input stream is xml if it is, use create properties loaded from xml
     * format, otherwise create properties from default format.
     */
    public static Properties loadUniversal(InputStream in) throws IOException {
        final String xmlDeclarationStart = "<?xml";
        BufferedInputStream bin = new BufferedInputStream(in);
        bin.mark(4096);

        BufferedReader reader = new BufferedReader(new InputStreamReader(bin));
        String line = reader.readLine();
        Properties props = new Properties();
        if (line != null) {
            boolean isXML = line.startsWith(xmlDeclarationStart);
            bin.reset();
            if (isXML) props.loadFromXML(bin);
            else props.load(bin);
        }
        return props;
    }

    /**
     * Reads a property file.
     *
     * <p>This method delegates to {@link #loadUniversal(InputStream)}.
     */
    public static Properties loadPropertyFile(File f) throws IOException {
        FileInputStream fin = new FileInputStream(f);
        try {
            return loadUniversal(fin);
        } finally {
            fin.close();
        }
    }

    /**
     * Reads a property file resource.
     *
     * <p>This method delegates to {@link #loadUniversal(InputStream)}.
     */
    public static Properties loadPropertyFile(Resource f) throws IOException {
        try (InputStream in = f.in()) {
            return loadUniversal(in);
        }
    }

    /**
     * Tries recoding the old passwords.
     *
     * <p>If it is not possible to retrieve the raw password (digest encoding, empty encoding), the
     * old encoding is used.
     *
     * <p>If it is possible to retrieve the raw password, the password is recoded using the actual
     * password encoder
     */
    public static void recodePasswords(GeoServerUserGroupStore store) throws IOException {
        GeoServerPasswordEncoder encoder =
                store.getSecurityManager().loadPasswordEncoder(store.getPasswordEncoderName());
        encoder.initializeFor(store);

        GeoServerMultiplexingPasswordEncoder mEncoder =
                new GeoServerMultiplexingPasswordEncoder(store.getSecurityManager(), store);
        for (GeoServerUser user : store.getUsers()) {
            if (encoder.isResponsibleForEncoding(user.getPassword())) continue; // nothing to do
            try {
                String rawpass = mEncoder.decode(user.getPassword());
                // to avoid password policy exceptions, recode explicitly
                String encPass = encoder.encodePassword(rawpass, null);
                user.setPassword(encPass);
                try {
                    store.updateUser(user);
                } catch (PasswordPolicyException e) {
                    store.load(); // rollback
                    throw new RuntimeException("Never should reach this point", e);
                }
            } catch (UnsupportedOperationException ex) {
                LOGGER.warning(
                        "Cannot recode user: "
                                + user.getUsername()
                                + " with password: "
                                + user.getPassword());
            }
        }
        store.store();
    }
}
