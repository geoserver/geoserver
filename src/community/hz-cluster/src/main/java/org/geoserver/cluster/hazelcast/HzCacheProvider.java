/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.util.CacheProvider;
import org.geoserver.util.DefaultCacheProvider;

/**
 * {@link CacheProvider} for a cluster configuration. Looked up by interface by {@link
 * DefaultCacheProvider#findProvider()} hence shall be declared in {@code applicationContext.xml}.
 */
public class HzCacheProvider implements CacheProvider {

    private static final long DEFAULT_TTL = 5;

    private static final TimeUnit DEFAULT_TTL_UNIT = TimeUnit.MINUTES;

    public static final String DEFAULT_TIME_KEY = "evictionTime";

    /** Expiration time in minutes for each entry */
    public final long expirationMinutes =
            Long.parseLong(System.getProperty(DEFAULT_TIME_KEY, DEFAULT_TTL + ""));

    private Map<String, Cache<?, ?>> inUse = Maps.newConcurrentMap();

    private XStreamPersisterFactory serializationFactory;

    public HzCacheProvider(XStreamPersisterFactory serializationFactory) {
        this.serializationFactory = serializationFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <K extends Serializable, V extends Serializable> Cache<K, V> getCache(
            final String cacheName) {

        Cache<K, V> distributedCache = (Cache<K, V>) inUse.get(cacheName);
        if (distributedCache == null) {
            // distributedCache = new NullCache<K, V>();
            if ("catalog".equals(cacheName)) {
                distributedCache =
                        (Cache<K, V>)
                                new HzCatalogCache(
                                        cacheName,
                                        expirationMinutes,
                                        DEFAULT_TTL_UNIT,
                                        serializationFactory);
            } else {
                distributedCache =
                        new HzCache<K, V>(cacheName, expirationMinutes, DEFAULT_TTL_UNIT);
            }
            inUse.put(cacheName, distributedCache);
        }
        return distributedCache;
    }

    private static final class HzCache<K extends Serializable, V extends Serializable>
            implements Cache<K, V> {

        private IMap<K, V> hzMap;

        private final long ttl;

        private final TimeUnit timeunit;

        private final String mapName;

        public HzCache(String mapName, long ttl, TimeUnit ttlUnit) {
            this.mapName = mapName;
            this.hzMap = null;
            this.ttl = ttl;
            this.timeunit = ttlUnit;
        }

        private boolean available() {
            if (hzMap == null) {
                if (HzCluster.getInstanceIfAvailable().isPresent()) {
                    HzCluster hzCluster = HzCluster.getInstanceIfAvailable().get();
                    HazelcastInstance hazelcastInstance = hzCluster.getHz();
                    hzMap = hazelcastInstance.getMap(mapName);
                }
            }
            return hzMap != null;
        }

        @Override
        public V getIfPresent(Object key) {
            if (available()) {
                return hzMap.get(key);
            }
            return null;
        }

        @Override
        public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
            V value = getIfPresent(key);
            if (value == null) {
                try {
                    value = valueLoader.call();
                    if (value != null) {
                        put(key, value);
                    }
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }
            return value;
        }

        @Override
        public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
            if (available()) {
                Set<K> set = new HashSet<K>();
                for (Object k : keys) {
                    set.add((K) k);
                }
                Map<K, V> allPresent = hzMap.getAll(set);
                return ImmutableMap.copyOf(allPresent);
            }
            return ImmutableMap.of();
        }

        @Override
        public void put(K key, V value) {
            if (available()) {
                hzMap.putTransient(key, value, ttl, timeunit);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void invalidate(Object key) {
            if (available()) {
                hzMap.remove((K) key);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void invalidateAll(Iterable<?> keys) {
            if (available()) {
                for (Object k : keys) {
                    hzMap.remove((K) k);
                }
            }
        }

        @Override
        public void invalidateAll() {
            if (available()) {
                hzMap.clear();
            }
        }

        @Override
        public long size() {
            return available() ? hzMap.size() : 0L;
        }

        @Override
        public CacheStats stats() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConcurrentMap<K, V> asMap() {
            if (available()) {
                return hzMap;
            }
            return Maps.newConcurrentMap();
        }

        @Override
        public void cleanUp() {
            //
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            hzMap.putAll(m);
        }
    }

    private static final class HzCatalogCache implements Cache<String, Info> {

        private IMap<String, byte[]> hzMap;

        private final long ttl;

        private final TimeUnit timeunit;

        private final String mapName;

        private XStreamPersisterFactory serializationFactory;

        private XStreamPersister persister;

        public HzCatalogCache(
                String mapName,
                long ttl,
                TimeUnit ttlUnit,
                XStreamPersisterFactory serializationFactory2) {
            this.mapName = mapName;
            this.ttl = ttl;
            this.timeunit = ttlUnit;
            this.serializationFactory = serializationFactory2;
            this.hzMap = null;
        }

        private boolean available() {
            Optional<HzCluster> cluster = HzCluster.getInstanceIfAvailable();
            if (!cluster.isPresent()) {
                return false;
            }
            HzCluster hzCluster = cluster.get();
            if (hzMap == null && hzCluster.isRunning()) {
                HazelcastInstance hazelcastInstance = hzCluster.getHz();
                Catalog catalog = hzCluster.getRawCatalog();
                hzMap = hazelcastInstance.getMap(mapName);
                persister = serializationFactory.createXMLPersister();
                persister.setCatalog(catalog);
            }
            return hzMap != null && hzCluster.isRunning();
        }

        @Override
        public Info getIfPresent(Object key) {
            Info info = null;
            if (available()) {
                byte[] serialForm = hzMap.get(key);
                if (serialForm != null) {
                    info = unmarshal(serialForm);
                }
            }
            return info;
        }

        private Info unmarshal(byte[] serialForm) {
            Info info;
            try {
                info = persister.load(new ByteArrayInputStream(serialForm), Info.class);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            return info;
        }

        @Override
        public Info get(String key, Callable<? extends Info> valueLoader)
                throws ExecutionException {
            Info value = getIfPresent(key);
            if (value == null) {
                try {
                    value = valueLoader.call();
                    put(key, value);
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }
            return value;
        }

        @Override
        public ImmutableMap<String, Info> getAllPresent(Iterable<?> keys) {
            if (available()) {
                Set<String> set = new HashSet<String>();
                for (Object k : keys) {
                    set.add((String) k);
                }
                Map<String, byte[]> allPresent = hzMap.getAll(set);
                Function<byte[], Info> function =
                        new Function<byte[], Info>() {
                            @Override
                            public Info apply(byte[] input) {
                                return unmarshal(input);
                            }
                        };
                Map<String, Info> transformedValues = Maps.transformValues(allPresent, function);
                return ImmutableMap.copyOf(transformedValues);
            }
            return ImmutableMap.of();
        }

        @Override
        public void put(String key, Info value) {
            if (available()) {
                byte[] serialForm = serialize(value);
                hzMap.putTransient(key, serialForm, ttl, timeunit);
            }
        }

        private byte[] serialize(Info value) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                persister.save(value, out);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            byte[] serialForm = out.toByteArray();
            return serialForm;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Info> m) {
            Function<Info, byte[]> f =
                    new Function<Info, byte[]>() {
                        @Override
                        public byte[] apply(Info input) {
                            return serialize(input);
                        }
                    };
            Map<? extends String, byte[]> map = Maps.transformValues(m, f);
            hzMap.putAll(map);
        }

        @Override
        public void invalidate(Object key) {
            if (available()) {
                hzMap.remove(String.valueOf(key));
            }
        }

        @Override
        public void invalidateAll(Iterable<?> keys) {
            if (available()) {
                for (Object k : keys) {
                    hzMap.remove(String.valueOf(k));
                }
            }
        }

        @Override
        public void invalidateAll() {
            if (available()) {
                hzMap.clear();
            }
        }

        @Override
        public long size() {
            return available() ? hzMap.size() : 0L;
        }

        @Override
        public CacheStats stats() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConcurrentMap<String, Info> asMap() {
            if (available()) {

                Function<byte[], Info> function =
                        new Function<byte[], Info>() {
                            @Override
                            public Info apply(byte[] input) {
                                return unmarshal(input);
                            }
                        };
                Map<String, Info> transformedValues = Maps.transformValues(hzMap, function);
                return new ConcurrentHashMap<String, Info>(transformedValues);
            }
            return Maps.newConcurrentMap();
        }

        @Override
        public void cleanUp() {
            //
        }
    }

    private static class NullCache<K, V> implements Cache<K, V> {

        @Override
        public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
            try {
                V value = valueLoader.call();
                return value;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public V getIfPresent(Object key) {
            return null;
        }

        @Override
        public long size() {
            return 0L;
        }

        @Override
        public void invalidate(Object key) {
            //
        }

        @Override
        public void invalidateAll() {
            //
        }

        @Override
        public void put(K key, V value) {
            //
        }

        @Override
        public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
            return ImmutableMap.of();
        }

        @Override
        public void invalidateAll(Iterable<?> keys) {
            //
        }

        @Override
        public CacheStats stats() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConcurrentMap<K, V> asMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cleanUp() {
            //
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            //
        }
    }
}
