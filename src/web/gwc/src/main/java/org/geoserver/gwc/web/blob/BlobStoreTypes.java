/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.BlobStoreInfo;

/**
 * Access point for BlobStore Types stored in Spring
 *
 * @author Niels Charlier
 */
public final class BlobStoreTypes {

    private BlobStoreTypes() {}

    /** Lazy loaded map of blob store types */
    private static Map<Class<? extends BlobStoreInfo>, BlobStoreType<?>> TYPES;

    private static Map<Class<? extends BlobStoreInfo>, BlobStoreType<?>> getTypes() {
        if (TYPES == null) {
            // the treemap with comparator makes sure that the types are always displayed in the
            // same order, alphabetically sorted on name

            TreeMap<Class<? extends BlobStoreInfo>, BlobStoreType<?>> tmp =
                    new TreeMap<>(Comparator.comparing(Object::toString));
            for (BlobStoreType<?> type : GeoWebCacheExtensions.extensions(BlobStoreType.class)) {
                tmp.put(type.getConfigClass(), type);
            }
            TYPES = tmp;
        }
        return TYPES;
    }

    /** Get BlobStoreType from BlobStoreInfo class */
    public static BlobStoreType<?> getFromClass(Class<? extends BlobStoreInfo> clazz) {
        return getTypes().get(clazz);
    }

    /** Get all BlobStoreTypes */
    public static List<BlobStoreType<?>> getAll() {
        return new ArrayList<BlobStoreType<?>>(getTypes().values());
    }
}
