/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKey;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.KeyStoreProvider;

/**
 * Locking wrapper for {@link KeyStoreProviderImpl}
 *
 * @author christian
 */
public class LockingKeyStoreProvider implements KeyStoreProvider {

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    protected KeyStoreProvider provider;

    /** get a read lock */
    protected void readLock() {
        readLock.lock();
    }

    /** free read lock */
    protected void readUnLock() {
        readLock.unlock();
    }

    /** get a write lock */
    protected void writeLock() {
        writeLock.lock();
    }

    /** free write lock */
    protected void writeUnLock() {
        writeLock.unlock();
    }

    public LockingKeyStoreProvider(KeyStoreProvider prov) {
        this.provider = prov;
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        provider.setSecurityManager(securityManager);
    }

    @Override
    public Resource getResource() {
        return provider.getResource();
    }

    @Override
    public void reloadKeyStore() throws IOException {
        writeLock();
        try {
            provider.reloadKeyStore();
        } finally {
            writeUnLock();
        }
    }

    @Override
    public Key getKey(String alias) throws IOException {
        readLock();
        try {
            return provider.getKey(alias);
        } finally {
            readUnLock();
        }
    }

    @Override
    public byte[] getConfigPasswordKey() throws IOException {
        readLock();
        try {
            return provider.getConfigPasswordKey();
        } finally {
            readUnLock();
        }
    }

    @Override
    public boolean hasConfigPasswordKey() throws IOException {
        readLock();
        try {
            return provider.hasConfigPasswordKey();
        } finally {
            readUnLock();
        }
    }

    @Override
    public boolean containsAlias(String alias) throws IOException {
        readLock();
        try {
            return provider.containsAlias(alias);
        } finally {
            readUnLock();
        }
    }

    @Override
    public byte[] getUserGroupKey(String serviceName) throws IOException {
        readLock();
        try {
            return provider.getUserGroupKey(serviceName);
        } finally {
            readUnLock();
        }
    }

    @Override
    public boolean hasUserGroupKey(String serviceName) throws IOException {
        readLock();
        try {
            return provider.hasUserGroupKey(serviceName);
        } finally {
            readUnLock();
        }
    }

    @Override
    public SecretKey getSecretKey(String name) throws IOException {
        readLock();
        try {
            return provider.getSecretKey(name);
        } finally {
            readUnLock();
        }
    }

    @Override
    public PublicKey getPublicKey(String name) throws IOException {
        readLock();
        try {
            return provider.getPublicKey(name);
        } finally {
            readUnLock();
        }
    }

    @Override
    public PrivateKey getPrivateKey(String name) throws IOException {
        readLock();
        try {
            return provider.getPrivateKey(name);
        } finally {
            readUnLock();
        }
    }

    @Override
    public String aliasForGroupService(String serviceName) {
        return provider.aliasForGroupService(serviceName);
    }

    @Override
    public boolean isKeyStorePassword(char[] password) throws IOException {
        readLock();
        try {
            return provider.isKeyStorePassword(password);
        } finally {
            readUnLock();
        }
    }

    @Override
    public void setSecretKey(String alias, char[] key) throws IOException {
        writeLock();
        try {
            provider.setSecretKey(alias, key);
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void setUserGroupKey(String serviceName, char[] password) throws IOException {
        writeLock();
        try {
            provider.setUserGroupKey(serviceName, password);
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void removeKey(String alias) throws IOException {
        writeLock();
        try {
            provider.removeKey(alias);
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void storeKeyStore() throws IOException {
        writeLock();
        try {
            provider.storeKeyStore();
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword)
            throws IOException {
        writeLock();
        try {
            provider.prepareForMasterPasswordChange(oldPassword, newPassword);
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void abortMasterPasswordChange() {
        writeLock();
        try {
            provider.abortMasterPasswordChange();
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void commitMasterPasswordChange() throws IOException {
        writeLock();
        try {
            provider.commitMasterPasswordChange();
        } finally {
            writeUnLock();
        }
    }
}
