/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;

public final class InfoIdentities {

    private static final InfoIdentities SINGLETON = new InfoIdentities();

    public static final InfoIdentities get() {
        return SINGLETON;
    }

    private static final Class<?>[] ROOT_CLASSES =
            new Class<?>[] {
                MapInfo.class,
                NamespaceInfo.class,
                LayerInfo.class,
                LayerGroupInfo.class,
                ResourceInfo.class,
                StoreInfo.class,
                StyleInfo.class,
                WorkspaceInfo.class
            };

    @SuppressWarnings("unchecked")
    public static <T extends Info> Class<? extends Info> root(Class<T> clazz) {
        for (Class<?> rootClass : ROOT_CLASSES) {
            if (rootClass.isAssignableFrom(clazz)) {
                return (Class<? extends Info>) rootClass;
            }
        }
        return null;
    }

    private final Map<Class<? extends Info>, String[][]> descriptors = new HashMap<>();

    public final <T extends Info> List<InfoIdentity> getIdentities(T info) {
        Class<? extends Info> rootClazz = root(info.getClass());
        if (rootClazz == null) {
            return Collections.emptyList();
        } else {
            List<InfoIdentity> list = new ArrayList<>();
            for (String[] descriptor : descriptors.get(rootClazz)) {
                String[] idValues = new String[descriptor.length];
                for (int i = 0; i < descriptor.length; i++) {
                    try {
                        idValues[i] =
                                PropertyUtils.getNestedProperty(info, descriptor[i]).toString();
                    } catch (NestedNullException e) {
                        idValues[i] = null;
                    } catch (IllegalAccessException
                            | InvocationTargetException
                            | NoSuchMethodException e) {
                        throw new IllegalStateException(e);
                    }
                }
                list.add(new InfoIdentity(rootClazz, descriptor, idValues));
            }
            return list;
        }
    }

    @SafeVarargs
    private final <T extends Info> void put(Class<T> clazz, String[]... descriptor) {
        descriptors.put(clazz, descriptor);
    }

    private InfoIdentities() {
        put(MapInfo.class, new String[] {"name"});
        put(NamespaceInfo.class, new String[] {"prefix"}, new String[] {"URI"});
        put(LayerGroupInfo.class, new String[] {"name"});
        put(LayerInfo.class, new String[] {"resource.id"});
        put(ResourceInfo.class, new String[] {"name"}, new String[] {"namespace.id", "name"});
        put(StoreInfo.class, new String[] {"name"}, new String[] {"workspace.id", "name"});
        put(StyleInfo.class, new String[] {"name"}, new String[] {"workspace.id", "name"});
        put(WorkspaceInfo.class, new String[] {"name"});
    }
}
