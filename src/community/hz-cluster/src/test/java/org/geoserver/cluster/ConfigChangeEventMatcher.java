/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import org.easymock.EasyMock;
import org.geoserver.catalog.Info;
import org.geoserver.cluster.ConfigChangeEvent.Type;

public class ConfigChangeEventMatcher extends EventMatcher {
    /** id of object */
    String id;

    /** name of object */
    String name;

    /** name of workspace qualifying the object */
    String workspaceId;

    /** class of object */
    Class<?> clazz;

    /** type of config change */
    ConfigChangeEvent.Type type;

    public static <T extends Info> ConfigChangeEvent configChangeEvent(
            Object source, String id, String name, String workspaceId, Class<T> clazz, Type type) {
        EasyMock.reportMatcher(
                new ConfigChangeEventMatcher(source, id, name, workspaceId, clazz, type));
        return null;
    }

    public ConfigChangeEventMatcher(
            Object source, String id, String name, String workspaceId, Class<?> clazz, Type type) {
        super(source);
        this.id = id;
        this.name = name;
        this.workspaceId = workspaceId;
        this.clazz = clazz;
        this.type = type;
    }

    static boolean nullsafeEquals(Object expected, Object actual) {
        return expected == null ? actual == null : expected == actual;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof ConfigChangeEvent) {
            ConfigChangeEvent evt = (ConfigChangeEvent) argument;
            return super.matches(argument)
                    && nullsafeEquals(this.id, evt.getObjectId())
                    && nullsafeEquals(this.name, evt.getObjectName())
                    && nullsafeEquals(this.workspaceId, evt.getWorkspaceId())
                    && nullsafeEquals(this.clazz, evt.getObjectInterface())
                    && nullsafeEquals(this.type, evt.getChangeType());
        } else {
            return false;
        }
    }

    @Override
    public void appendTo(StringBuffer buffer) {}
}
