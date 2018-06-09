/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.vfny.geoserver.global.ConfigurationException;

/**
 * The EditableUserDAO class provides a UserDetailsService implementation that allows modifying user
 * information programmatically.
 *
 * @author David Winslow - <dwinslow@openplans.org>
 */
public class EditableUserDAO implements UserDetailsService {

    /*
    Things to test:
    properties file exists, but is empty (should result in no defined users)
    properties file exists and is valid  (should result in users as defined by file)
    properties file is changed by an external process (should result in users being dynamically loaded)
    properties file is deleted (should result in default admin user being created)
    properties file does not exist and cannot be created (should result in a default user anyway so people can still log in)
    */

    /** The Map used for in-memory storage of user details */
    private Map<String, UserDetails> myDetailStorage;

    /** A PropertyFileWatcher to track outside changes to the user properties file */
    private PropertyFileWatcher myWatcher;

    /** The GeoServer instance this EditableUserDAO is servicing. */
    private GeoServer geoServer;

    /**
     * Find the file that should provide the user information.
     *
     * @throws ConfigurationException if the user configuration file does not exist and cannot be
     *     created
     * @throws IOException if an error occurs while opening the user configuration file
     */
    private Resource getUserFile() throws ConfigurationException, IOException {
        GeoServerResourceLoader loader = geoServer.getCatalog().getResourceLoader();
        return loader.get("security/users.properties");
    }

    /**
     * Create an EditableUserDAO object. This currently entails:
     *
     * <ul>
     *   <li>Finding the user configuration file
     *   <li>Creating a PropertyFileWatcher to track changes to it
     *   <li>Loading the user details into a map in memory
     * </ul>
     *
     * If no user information is found, a default user will be created.
     */
    public EditableUserDAO() {
        myDetailStorage = new HashMap<String, UserDetails>();
        try {
            myWatcher = new PropertyFileWatcher(getUserFile());
        } catch (Exception e) {
            // TODO:log error someplace
            createDefaultUser();
        }

        update();
        if (myDetailStorage.isEmpty()) createDefaultUser();
    }

    /**
     * Generate the default geoserver administrator user. The administrator will be added directly
     * to the in-memory storage of the user details, rather than returned by this method.
     */
    private void createDefaultUser() {
        String name = (geoServer == null ? "admin" : geoServer.getGlobal().getAdminUsername());
        String passwd =
                (geoServer == null ? "geoserver" : geoServer.getGlobal().getAdminPassword());

        Collection<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
        auths.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        myDetailStorage.put(name, new User(name, passwd, true, true, true, true, auths));
    }

    /**
     * Find a user's details based on the username.
     *
     * @param username the username for the desired user
     * @return a UserDetails object with the user's details, or null if no such user exists
     */
    public UserDetails loadUserByUsername(String username) {
        update();
        return (UserDetails) myDetailStorage.get(username);
    }

    /**
     * Create a user with the specified credentials and authority. The user will be automatically
     * added to persistant storage. If the user already exists, the previously existing user will be
     * overwritten.
     *
     * @param username the username for the user being added
     * @param details a UserAttribute containing the credentials and authorities for the user
     * @throws ConfigurationException if the user configuration file does not exist and cannot be
     *     created
     * @throws IOException if an error occurs while opening the user configuration file
     */
    public void setUserDetails(String username, UserAttribute details)
            throws IOException, ConfigurationException {
        update();
        myDetailStorage.put(username, makeUser(username, details));
        syncChanges();
    }

    /**
     * Remove a user specified by name. If the username is not used by any known user, nothing
     * happens.
     *
     * @param username the name of the user to delete
     * @throws ConfigurationException if the user configuration file does not exist and cannot be
     *     created
     * @throws IOException if an error occurs while opening the user configuration file
     */
    public void deleteUser(String username) throws IOException, ConfigurationException {
        update();
        myDetailStorage.remove(username);
        syncChanges();
    }

    /**
     * Ensure the user data map matches the information in the user data file. This should be called
     * automatically, so that no code outside of this class needs to access this method.
     */
    private void update() {
        try {
            if (myWatcher == null) {
            } else if (myWatcher.isStale()) {
                Properties prop = myWatcher.getProperties();
                UserAttributeEditor uae = new UserAttributeEditor();
                myDetailStorage.clear();

                Iterator<Object> it = prop.keySet().iterator();
                while (it.hasNext()) {
                    String username = (String) it.next();
                    uae.setAsText(prop.getProperty(username));
                    UserAttribute attrs = (UserAttribute) uae.getValue();
                    if (attrs != null) {
                        myDetailStorage.put(username, makeUser(username, attrs));
                    }
                }
            }
        } catch (IOException ioe) {
            // TODO: handle the exception properly
            myDetailStorage.clear();
            createDefaultUser();
        }
    }

    /**
     * Convenience method for creating users from a UserAttribute and a username.
     *
     * @param username the name of the new user
     * @param attrs the attributes to assign to the new user (authorities and credentials)
     * @return a UserDetails object with the provided username and attributes
     */
    private UserDetails makeUser(String username, UserAttribute attrs) {
        return new User(
                username,
                attrs.getPassword(),
                attrs.isEnabled(),
                true, // account not expired
                true, // credentials not expired
                true, // account not locked
                attrs.getAuthorities());
    }

    /**
     * Write the changes to persistant storage. This should happen automatically when changes are
     * made, so no code outside of this class should need to call this method.
     *
     * @throws ConfigurationException if the user configuration file does not exist and cannot be
     *     created
     * @throws IOException if an error occurs while opening the user configuration file
     */
    private void syncChanges() throws IOException, ConfigurationException {
        Properties prop = new Properties();

        for (UserDetails details : myDetailStorage.values()) {
            String key = details.getUsername();
            String value = details.getPassword();
            for (GrantedAuthority auth : details.getAuthorities()) {
                value += "," + auth.getAuthority();
            }
            if (!details.isEnabled()) {
                value += ",disabled";
            }
            prop.setProperty(key, value);
        }

        OutputStream os = new BufferedOutputStream(getUserFile().out());

        prop.store(
                os,
                "Geoserver user data. Format is username=password,role1,role2,...[enabled|disabled]");
    }

    /**
     * Spring-friendly getter to go along with the setter.
     *
     * @return this object's associated GeoServer instance
     */
    public GeoServer getGeoServer() {
        return geoServer;
    }

    /**
     * Spring-friendly setter so we can easily get a reference to the GeoServer instance
     *
     * @param geoServer the GeoServer instance this DAO will be working with
     */
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /**
     * TODO: Actually document this!
     *
     * @author David Winslow
     */
    public Set<String> getNameSet() {
        return myDetailStorage.keySet();
    }
}
