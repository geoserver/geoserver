/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SortedSet;
import java.util.TreeMap;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;

/**
 * Implementation for testing uses serialization into a byte array
 *
 * @author christian
 */
public class MemoryUserGroupService extends AbstractUserGroupService {

    byte[] byteArray;
    protected String toBeEncrypted;

    public String getToBeEncrypted() {
        return toBeEncrypted;
    }

    public MemoryUserGroupService() {}

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        MemoryUserGroupStore store = new MemoryUserGroupStore();
        store.initializeFromService(this);
        return store;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deserialize() throws IOException {
        clearMaps();
        if (byteArray == null) return;
        ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
        ObjectInputStream oin = new ObjectInputStream(in);
        try {
            helper.userMap = (TreeMap<String, GeoServerUser>) oin.readObject();
            helper.groupMap = (TreeMap<String, GeoServerUserGroup>) oin.readObject();
            helper.user_groupMap =
                    (TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>>) oin.readObject();
            helper.group_userMap =
                    (TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>>) oin.readObject();
            helper.propertyMap = (TreeMap<String, SortedSet<GeoServerUser>>) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        GeoServerUser user = new MemoryGeoserverUser(username, this);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new MemoryGeoserverUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        this.name = config.getName();
        SecurityUserGroupServiceConfig ugConfig = (SecurityUserGroupServiceConfig) config;
        passwordEncoderName = ugConfig.getPasswordEncoderName();
        GeoServerPasswordEncoder enc =
                getSecurityManager().loadPasswordEncoder(passwordEncoderName);

        if (enc.getEncodingType() == PasswordEncodingType.ENCRYPT) {
            KeyStoreProvider prov = getSecurityManager().getKeyStoreProvider();
            String alias = prov.aliasForGroupService(name);
            if (prov.containsAlias(alias) == false) {
                prov.setUserGroupKey(
                        name,
                        getSecurityManager()
                                .getRandomPassworddProvider()
                                .getRandomPasswordWithDefaultLength());
                prov.storeKeyStore();
            }
        }
        enc.initializeFor(this);
        passwordValidatorName = ugConfig.getPasswordPolicyName();
        toBeEncrypted = (((MemoryUserGroupServiceConfigImpl) config).getToBeEncrypted());
    }
}
