/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.*;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Implementation for testing uses serialization into a byte array
 *
 * @author christian
 */
public class MemoryUserGroupStore extends AbstractUserGroupStore {

    @Override
    protected void serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(helper.userMap);
        oout.writeObject(helper.groupMap);
        oout.writeObject(helper.user_groupMap);
        oout.writeObject(helper.group_userMap);
        oout.writeObject(helper.propertyMap);
        ((MemoryUserGroupService) service).byteArray = out.toByteArray();
        oout.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deserialize() throws IOException {
        clearMaps();
        byte[] bytes = ((MemoryUserGroupService) service).byteArray;
        if (bytes == null) {
            setModified(false);
            return;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
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
        setModified(false);
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new MemoryGeoserverUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }
}
