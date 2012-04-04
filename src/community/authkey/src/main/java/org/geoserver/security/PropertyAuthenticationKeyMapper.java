/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserDao;
import org.geotools.util.logging.Logging;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.User;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Uses the property file <code>$GEOSERVER_DATA_DIR/security/authkey.properties</code> as the source
 * for unique user identifiers. The file format is:
 * <ul>
 * <li>userkey1=username1</li>
 * <li>userkey2=username2</li>
 * <li>...</li>
 * </ul>
 * 
 * The file will be automatically reloaded when modified
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class PropertyAuthenticationKeyMapper implements AuthenticationKeyMapper {

    /**
     * Name of the file used to store the authentication keys
     */
    public static final String AUTHKEYS_FILE = "authkeys.properties";

    static final Logger LOGGER = Logging.getLogger(PropertyAuthenticationKeyMapper.class);

    PropertyFileWatcher userDefinitionsFile;

    Map<String, String> userMap;

    GeoServerSecurityManager secMgr;

    /**
     * Either loads the default property file on the first access, or reloads it if it has been
     * modified since last access.
     * 
     * @throws DataAccessResourceFailureException
     */
    void checkKeyMap() throws DataAccessResourceFailureException {
        if ((userMap == null) || userDefinitionsFile == null || userDefinitionsFile.isStale()) {
            try {
                if (userDefinitionsFile == null) {
                    // lookup the property file and build a watcher if it's there, otherwise
                    // try to create a sample file
                    File propFile = getPropertyFile();
                    if (propFile != null) {
                        if (!propFile.exists()) {
                            writeDefaultPropertyFile(propFile);
                        }

                        if (propFile.exists()) {
                            userDefinitionsFile = new PropertyFileWatcher(propFile);
                        }
                    }
                }

                // if we got a hold on the property file load it, otherwise work with an empty map
                if (userDefinitionsFile == null) {
                    userMap = Collections.emptyMap();
                } else {
                    userMap = new HashMap(userDefinitionsFile.getProperties());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred loading user definitions", e);
            }
        }
    }

    private void writeDefaultPropertyFile(File propFile) {
        try {
            // write out an example file, but fully commented out
            BufferedWriter writer = new BufferedWriter(new FileWriter(propFile));
            writer.write("# Format is authkey=username");
            writer.newLine();

            // if we have a list of users available build a sample key for each
            if (secMgr != null) {
                writer.write("# Here is an example content based on your users, uncomment to activate");
                writer.newLine();
                writer.write("#" + UUID.randomUUID().toString() + "=admin");
                writer.newLine();

                for (GeoServerUserGroupService ugService : secMgr.loadUserGroupServices()) {
                    Set<GeoServerUser> users = ugService.getUsers();
                    if (users.size() > 0) {
                        for (GeoServerUser user : users) {
                            if(!"admin".equals(user.getUsername())) {
                                writer.write("#" + UUID.randomUUID().toString() + "=" + user.getUsername());
                                writer.newLine();
                            }
                        }
                    }
                }
            }

            writer.close();
        } catch (IOException e) {
            // this is fine, we tried but the data dir might be not writable by us
            LOGGER.log(Level.SEVERE,
                    "Failed to write out example authekey file " + propFile.getAbsolutePath(), e);
        }
    }

    /**
     * Locates and returns the property file, or returns null otherwise (e.g., the parent folder is
     * not there, or permission issues)
     * 
     * @return
     * @throws ConfigurationException
     */
    private File getPropertyFile() throws ConfigurationException {
        File securityDir = GeoserverDataDirectory.findCreateConfigDir("security");
        if (securityDir != null) {
            return new File(securityDir, AUTHKEYS_FILE);
        } else {
            return null;
        }
    }

    public String getUserName(String key) {
        checkKeyMap();
        return userMap.get(key);
    }

    /**
     * Optional property, if available it will be used to build a sample authkey.properties file
     *
     */
    public void setSecurityManager(GeoServerSecurityManager secMgr) {
        this.secMgr = secMgr;
        try {
            File file = getPropertyFile();
            if (file != null && !file.exists()) {
                writeDefaultPropertyFile(file);
            }
        } catch(Exception e) {
            // let it go, we're just trying to setup an example file
        }
    }

}
