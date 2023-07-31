/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;

/**
 * {@link CatalogInfoLookup} adding a {@link NamespaceInfo#getURI() URI} multi-valued index for
 * {@link #findAllByUri(String) fast lookup} of namespaces by uri.
 *
 * <p>All {@link CatalogInfoLookup} mutating methods are overridden to maintain the index
 * consistency
 */
class NamespaceInfoLookup extends CatalogInfoLookup<NamespaceInfo> {

    private ConcurrentHashMap<String, List<NamespaceInfo>> index = new ConcurrentHashMap<>();

    /** guards modifications to the index and its ArrayList values */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private static Comparator<NamespaceInfo> VALUE_ORDER =
            (n1, n2) -> n1.getId().compareTo(n2.getId());

    public NamespaceInfoLookup() {
        super(DefaultCatalogFacade.NAMESPACE_NAME_MAPPER);
    }

    /** Uses the internal URI index to locate all the {@link NamespaceInfo}s with such URI */
    public List<NamespaceInfo> findAllByUri(String uri) {
        lock.readLock().lock();
        try {
            return List.copyOf(valueList(uri, false));
        } finally {
            lock.readLock().unlock();
        }
    }

    /** type-narrowing for the return type */
    @Override
    public NamespaceInfoLookup setCatalog(Catalog catalog) {
        super.setCatalog(catalog);
        return this;
    }

    @Override
    public NamespaceInfo add(NamespaceInfo value) {
        lock.writeLock().lock();
        try {
            NamespaceInfo ns = super.add(value);
            addInternal(value);
            return ns;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addInternal(NamespaceInfo value) {
        List<NamespaceInfo> values = valueList(value.getURI(), true);
        values.add(ModificationProxy.unwrap(value));
        values.sort(VALUE_ORDER);
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            super.clear();
            index.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public NamespaceInfo remove(NamespaceInfo value) {
        lock.writeLock().lock();
        try {
            String uri = value.getURI();
            NamespaceInfo ns = super.remove(value);
            removeInternal(value, uri);
            return ns;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeInternal(NamespaceInfo value, String uri) {
        List<NamespaceInfo> list = valueList(uri, false);
        if (!list.isEmpty()) list.remove(ModificationProxy.unwrap(value));
        if (list.isEmpty()) {
            index.remove(uri);
        }
    }

    @Override
    public void update(NamespaceInfo value) {
        lock.writeLock().lock();
        try {
            ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(value);
            NamespaceInfo actualValue = (NamespaceInfo) h.getProxyObject();

            String oldUri = actualValue.getURI();
            String newUri = value.getURI();

            final boolean uriChanged = !Objects.equals(oldUri, newUri);
            super.update(value);

            if (uriChanged) {
                removeInternal(value, oldUri);
                addInternal(value);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Looks up the list of values associated to the given {@code uri}
     *
     * @param uri the index key
     * @param create whether to create the index entry list if it doesn't exist
     * @return the index entry, may an unmodifiable empty list if it doesn't exist and {@code create
     *     == false}
     */
    @VisibleForTesting
    List<NamespaceInfo> valueList(String uri, boolean create) {
        if (create) {
            return index.computeIfAbsent(uri, v -> new ArrayList<>());
        }
        return index.getOrDefault(uri, List.of());
    }
}
