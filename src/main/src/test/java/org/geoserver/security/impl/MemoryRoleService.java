/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;

/**
 * Implementation for testing uses serialization into a byte array
 *
 * @author christian
 */
public class MemoryRoleService extends AbstractRoleService {

    byte[] byteArray;
    protected String toBeEncrypted;

    public String getToBeEncrypted() {
        return toBeEncrypted;
    }

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        MemoryRoleStore store = new MemoryRoleStore();
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
            helper.roleMap = (TreeMap<String, GeoServerRole>) oin.readObject();
            helper.role_parentMap = (HashMap<GeoServerRole, GeoServerRole>) oin.readObject();
            helper.user_roleMap = (TreeMap<String, SortedSet<GeoServerRole>>) oin.readObject();
            helper.group_roleMap = (TreeMap<String, SortedSet<GeoServerRole>>) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new MemoryGeoserverRole(role);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        toBeEncrypted = (((MemoryRoleServiceConfigImpl) config).getToBeEncrypted());
    }
}
