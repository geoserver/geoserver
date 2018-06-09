/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;

/**
 * Implementation for testing. All {@link Authentication} objects are stored serialized to be
 * prepared for clustering.
 *
 * @author mcr
 */
public class TestingAuthenticationCache implements AuthenticationCache {

    Map<String, Map<String, byte[]>> cache = new HashMap<String, Map<String, byte[]>>();

    public static Integer DEFAULT_IDLE_SECS = 60;
    public static Integer DEFAULT_LIVE_SECS = 600;

    Map<String, Map<String, Integer[]>> expireMap = new HashMap<String, Map<String, Integer[]>>();

    @Override
    public void removeAll() {
        cache.clear();
        expireMap.clear();
    }

    @Override
    public void removeAll(String filterName) {
        cache.remove(filterName);
        expireMap.remove(filterName);
    }

    @Override
    public void remove(String filterName, String cacheKey) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map != null) map.remove(cacheKey);

        Map<String, Integer[]> map2 = expireMap.get(filterName);
        if (map2 != null) map.remove(cacheKey);
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map != null) return deserializeAuthentication(map.get(cacheKey));
        else return null;
    }

    public Integer[] getExpireTimes(String filterName, String cacheKey) {
        Integer[] result = null;
        Map<String, Integer[]> map = expireMap.get(filterName);
        if (map != null) result = map.get(cacheKey);
        if (result == null) return new Integer[] {DEFAULT_IDLE_SECS, DEFAULT_LIVE_SECS};
        return result;
    }

    @Override
    public void put(
            String filterName,
            String cacheKey,
            Authentication auth,
            Integer timeToIdleSeconds,
            Integer timeToLiveSeconds) {
        put(filterName, cacheKey, auth);
        if (timeToIdleSeconds != null || timeToLiveSeconds != null) {
            Map<String, Integer[]> map = expireMap.get(filterName);
            if (map == null) {
                map = new HashMap<String, Integer[]>();
                expireMap.put(filterName, map);
            }
            map.put(cacheKey, new Integer[] {timeToIdleSeconds, timeToLiveSeconds});
        }
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
        Map<String, byte[]> map = cache.get(filterName);
        if (map == null) {
            map = new HashMap<String, byte[]>();
            cache.put(filterName, map);
        }
        map.put(cacheKey, serializeAuthentication(auth));
    }

    Authentication deserializeAuthentication(byte[] bytes) {
        if (bytes == null) return null;
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bin);
            Authentication auth = (Authentication) in.readObject();
            in.close();
            return auth;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] serializeAuthentication(Authentication auth) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(auth);
            out.close();
            return bout.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
