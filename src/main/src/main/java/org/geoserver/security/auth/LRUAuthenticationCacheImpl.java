/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.auth;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * An {@link AuthenticationCache} implementation using a {@link LRUCache} for
 * caching authentication tokens. 
 * 
 * For an explanation of the time parameters, see {@link AuthenticationCacheEntry}
 * 
 * The class uses a {@link ReentrantReadWriteLock} object to synchronize 
 * access from multiple threads 
 * 
 * Additionally, a {@link TimerTask} is started to remove expired entries. 
 * 
 * @author christian
 *
 */
public class LRUAuthenticationCacheImpl implements AuthenticationCache {

    protected LRUCache<AuthenticationCacheKey, AuthenticationCacheEntry> cache;
    int timeToIdleSeconds,timeToLiveSeconds,maxEntries;
    
    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    protected final Timer timer = new Timer(true);

    
    /**
     * Clean up task is run every 60 seconds
     */
    private static final int DEFAULT_MILLIS_BETWEEN_REMOVE_EXPIRED = 60 * 1000;
    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    /**
     * Timer task to remove unused entries
     * 
     */
    TimerTask removeExpiredTask = new TimerTask() {
        @Override
        public void run() {
            LOGGER.fine("Start searching for expired authentication tokens");
            writeLock.lock();
            try {
                Set<AuthenticationCacheKey> toBeRemoved = new HashSet<AuthenticationCacheKey>();
                long currentTime=System.currentTimeMillis();
                for (Entry<AuthenticationCacheKey, AuthenticationCacheEntry> e: cache.entrySet()) {
                    if (e.getValue().hasExpired(currentTime))
                        toBeRemoved.add(e.getKey());
                }
                LOGGER.fine("Number of expired authentication tokens found: " + toBeRemoved.size());
                for (AuthenticationCacheKey key: toBeRemoved)
                    cache.remove(key);                
            } finally {
                writeLock.unlock();
            }
            LOGGER.fine("End searching for expired authentication tokens");
        }
        
    };

    public LRUAuthenticationCacheImpl(int maxEntries) {
        this(DEFAULT_IDLE_TIME, DEFAULT_LIVE_TIME, maxEntries);
    }

    public LRUAuthenticationCacheImpl(int timeToIdleSeconds, int timeToLiveSeconds, int maxEntries) {
        super();
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;
        this.maxEntries = maxEntries;
        cache = new LRUCache<AuthenticationCacheKey, AuthenticationCacheEntry>(maxEntries);
        timer.schedule(removeExpiredTask,DEFAULT_MILLIS_BETWEEN_REMOVE_EXPIRED,DEFAULT_MILLIS_BETWEEN_REMOVE_EXPIRED );
    }

    public int getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }


    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }


    public int getMaxEntries() {
        return maxEntries;
    }


    
    @Override
    public void removeAll() {
        writeLock.lock();
        try {
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeAll(String filterName) {
        if (filterName==null) return;
        writeLock.lock();
        try {
            Set<AuthenticationCacheKey> toBeRemoved = new HashSet<AuthenticationCacheKey>();
            for (AuthenticationCacheKey key: cache.keySet()) {
                if(filterName.equals(key.getFilterName()))
                    toBeRemoved.add(key);
            }
            for (AuthenticationCacheKey key: toBeRemoved)
                cache.remove(key);
            
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String filterName, String cacheKey) {
        writeLock.lock();
        try {
            cache.remove(new AuthenticationCacheKey(filterName, cacheKey));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        readLock.lock();
        boolean hasTobeRemoved=false;
        try {
            long currentTime=System.currentTimeMillis();
            AuthenticationCacheEntry entry = cache.get(new AuthenticationCacheKey(filterName, cacheKey));
            if (entry==null)
                return null;
            if (entry.hasExpired(currentTime)) {
                hasTobeRemoved=true;                
                return null;
            }            
            entry.setLastAccessed(currentTime);
            return entry.getAuthentication();
            
        } finally {
            readLock.unlock();
            if (hasTobeRemoved)
                remove(filterName,cacheKey);
        }
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth,
            Integer timeToIdleSeconds, Integer timeToLiveSeconds) {
        
        timeToIdleSeconds = timeToIdleSeconds != null ? timeToIdleSeconds : this.timeToIdleSeconds;;
        timeToLiveSeconds = timeToLiveSeconds != null ? timeToLiveSeconds : this.timeToLiveSeconds;

        writeLock.lock();
        try {
            cache.put(new AuthenticationCacheKey(filterName, cacheKey),
                    new AuthenticationCacheEntry(auth, 
                            timeToIdleSeconds,
                            timeToLiveSeconds));
        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
        put(filterName,cacheKey,auth,timeToIdleSeconds,timeToLiveSeconds);
    }

    public void runRemoveExpiredTaskSynchron() {
        removeExpiredTask.run();
    }
}
