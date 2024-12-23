/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * Uses the property file <code>$GEOSERVER_DATA_DIR/security/authkey.properties</code> as the source for unique user
 * identifiers. The file format is:
 *
 * <ul>
 *   <li>userkey1=username1
 *   <li>userkey2=username2
 *   <li>...
 * </ul>
 *
 * The file will be automatically reloaded when modified
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PropertyAuthenticationKeyMapper extends AbstractAuthenticationKeyMapper {

    /** Name of the file used to store the authentication keys */
    public static final String AUTHKEYS_FILE = "authkeys.properties";

    PropertyFileWatcher fileWatcher;
    Properties authKeyProps;

    @Override
    public boolean supportsReadOnlyUserGroupService() {
        return true;
    }

    @Override
    public Set<String> getAvailableParameters() {
        return new HashSet<>(List.of("cacheTtlSeconds"));
    }

    @Override
    public synchronized GeoServerUser getUserInternal(String key) throws IOException {
        if (authKeyProps == null) {
            synchronize();
        }

        if (fileWatcher.isStale()) // reload if necessary
        authKeyProps = fileWatcher.getProperties();

        String userName = authKeyProps.getProperty(key);
        if (!StringUtils.hasLength(userName)) {
            LOGGER.warning("Cannot find user for auth key: " + key);
            return null;
        }
        GeoServerUser theUser = null;
        try {
            theUser = (GeoServerUser) getUserGroupService().loadUserByUsername(userName);
        } catch (UsernameNotFoundException ex) {
            LOGGER.warning("Cannot find user: " + userName + " in user/group service: " + getUserGroupServiceName());
            return null;
        }

        if (!theUser.isEnabled()) {
            LOGGER.info("Found user " + theUser.getUsername() + " for key " + key + ", but this user is disabled");
            return null;
        }

        return theUser;
    }

    @Override
    protected void checkPropertiesInternal() throws IOException {}

    @Override
    public synchronized int synchronize() throws IOException {
        checkProperties();

        File propFile = new File(getSecurityManager().userGroup().dir(), getUserGroupServiceName());
        propFile = new File(propFile, AUTHKEYS_FILE);

        File backupFile = new File(getSecurityManager().userGroup().dir(), getUserGroupServiceName());
        backupFile = new File(backupFile, AUTHKEYS_FILE + ".backup");

        // check if the previous synchronizing failed
        if (backupFile.exists())
            throw new IOException("The file: " + backupFile.getCanonicalPath() + " has to be removed first");

        // Clear the local cache
        resetUserCache();
        authKeyProps = new Properties();
        Properties oldProps = new Properties();

        // check if a property file exists and reload
        if (propFile.exists()) {
            FileUtils.copyFile(propFile, backupFile);
            try (FileInputStream inputFile = new FileInputStream(backupFile)) {
                oldProps.load(inputFile);
            }
        }

        Map<Object, Object> reverseMap = new HashMap<>();
        for (Entry<Object, Object> entry : oldProps.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }

        GeoServerUserGroupService service = getUserGroupService();

        int counter = 0;
        for (GeoServerUser user : service.getUsers()) {
            if (reverseMap.containsKey(user.getUsername())) {
                authKeyProps.put(reverseMap.get(user.getUsername()), user.getUsername());
            } else {
                authKeyProps.put(createAuthKey(), user.getUsername());
                counter++;
            }
        }
        try (FileOutputStream outputFile = new FileOutputStream(propFile, false)) {
            authKeyProps.store(outputFile, "Format is authkey=username");
        }

        if (backupFile.exists()) backupFile.delete();

        fileWatcher = new PropertyFileWatcher(Files.asResource(propFile));
        return counter;
    }
}
