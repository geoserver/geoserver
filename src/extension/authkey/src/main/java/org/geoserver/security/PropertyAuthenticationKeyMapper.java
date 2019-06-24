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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * Uses the property file <code>$GEOSERVER_DATA_DIR/security/authkey.properties</code> as the source
 * for unique user identifiers. The file format is:
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

    public boolean supportsReadOnlyUserGroupService() {
        return true;
    }

    @Override
    public synchronized GeoServerUser getUser(String key) throws IOException {
        checkProperties();

        if (authKeyProps == null) {
            synchronize();
        }

        if (fileWatcher.isStale()) // reload if necessary
        authKeyProps = fileWatcher.getProperties();

        String userName = authKeyProps.getProperty(key);
        if (StringUtils.hasLength(userName) == false) {
            LOGGER.warning("Cannot find user for auth key: " + key);
            return null;
        }
        GeoServerUser theUser = null;
        try {
            theUser = (GeoServerUser) getUserGroupService().loadUserByUsername(userName);
        } catch (UsernameNotFoundException ex) {
            LOGGER.warning(
                    "Cannot find user: "
                            + userName
                            + " in user/group service: "
                            + getUserGroupServiceName());
            return null;
        }

        if (theUser.isEnabled() == false) {
            LOGGER.info(
                    "Found user "
                            + theUser.getUsername()
                            + " for key "
                            + key
                            + ", but this user is disabled");
            return null;
        }

        return theUser;
    }

    @Override
    protected void checkProperties() throws IOException {
        super.checkProperties();
    }

    @Override
    public synchronized int synchronize() throws IOException {
        checkProperties();

        File propFile = new File(getSecurityManager().userGroup().dir(), getUserGroupServiceName());
        propFile = new File(propFile, AUTHKEYS_FILE);

        File backupFile =
                new File(getSecurityManager().userGroup().dir(), getUserGroupServiceName());
        backupFile = new File(backupFile, AUTHKEYS_FILE + ".backup");

        // check if the previous synchronize failed
        if (backupFile.exists())
            throw new IOException(
                    "The file: " + backupFile.getCanonicalPath() + " has to be removed first");

        authKeyProps = new Properties();
        Properties oldProps = new Properties();

        // check if property file exists and reload
        if (propFile.exists()) {
            FileUtils.copyFile(propFile, backupFile);
            FileInputStream inputFile = new FileInputStream(backupFile);
            try {
                oldProps.load(inputFile);
            } finally {
                inputFile.close();
            }
        }

        Map<Object, Object> reverseMap = new HashMap<Object, Object>();
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
        FileOutputStream outputFile = new FileOutputStream(propFile, false);
        try {
            authKeyProps.store(outputFile, "Format is authkey=username");
        } finally {
            outputFile.close();
        }

        if (backupFile.exists()) backupFile.delete();

        fileWatcher = new PropertyFileWatcher(Files.asResource(propFile));
        return counter;
    }
}
