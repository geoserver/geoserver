/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;

/**
 * Helper class for encryption of passwords in connection parameters for {@link StoreInfo} objects.
 *
 * <p>This class will encrypt any password parameter from {@link StoreInfo#getConnectionParameters()}.
 *
 * @author christian
 */
public class ConfigurationPasswordEncryptionHelper {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** cache of datastore factory class to fields to encrypt */
    protected static ConcurrentMap<Class<? extends DataAccessFactory>, Set<String>> CACHE = new ConcurrentHashMap<>();
    /**
     * cache of {@link StoreInfo#getType()} to fields to encrypt, if key not found defer to full DataAccessFactory
     * lookup
     */
    protected static ConcurrentMap<String, Set<String>> STORE_INFO_TYPE_CACHE = new ConcurrentHashMap<>();

    GeoServerSecurityManager securityManager;

    public ConfigurationPasswordEncryptionHelper(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Catalog getCatalog() {
        // JD: this class gets called during catalog initialization when reading store instances
        // that
        // potentially have encrypted parameters, so we have to be careful about how we access the
        // catalog, raw catalog directly to avoid triggering the initialization of the secure
        // catalog as we are reading the raw catalog contents (this could for instance cause a rule
        // to be ignored since a workspace has not been read)
        return (Catalog) GeoServerExtensions.bean("rawCatalog");
    }

    /**
     * Determines the fields in {@link StoreInfo#getConnectionParameters()} that require encryption for this type of
     * store object.
     */
    public Set<String> getEncryptedFields(StoreInfo info) {
        if (!(info instanceof DataStoreInfo)) {
            // only datastores supposed at this time, TODO: fix this

            List<EncryptedFieldsProvider> encryptedFieldsProviders =
                    GeoServerExtensions.extensions(EncryptedFieldsProvider.class);
            if (!encryptedFieldsProviders.isEmpty()) {
                Set<String> fields = new HashSet<>();
                for (EncryptedFieldsProvider provider : encryptedFieldsProviders) {
                    Set<String> providedFields = provider.getEncryptedFields(info);
                    if (providedFields != null && !providedFields.isEmpty()) {
                        fields.addAll(providedFields);
                    }
                }
                if (!fields.isEmpty()) {
                    return fields;
                }
            }

            return Collections.emptySet();
        }

        Set<String> toEncrypt;

        // fast lookup by store type
        final String storeType = info.getType();
        if (storeType != null) {
            toEncrypt = STORE_INFO_TYPE_CACHE.get(storeType);
            if (toEncrypt != null) {
                return toEncrypt;
            }
        }

        // store type not cached, find this store object data access factory
        DataAccessFactory factory;
        try {
            factory = getCatalog().getResourcePool().getDataStoreFactory((DataStoreInfo) info);
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error looking up factory for store : " + info + ". Unable to " + "encrypt connection parameters.",
                    e);
            return Collections.emptySet();
        }

        if (factory == null) {
            LOGGER.warning(
                    "Could not find factory for store : " + info + ". Unable to encrypt " + "connection parameters.");
            return Collections.emptySet();
        }

        // if factory returns no info no need to continue
        if (factory.getParametersInfo() == null) {
            return Collections.emptySet();
        }

        toEncrypt = CACHE.get(factory.getClass());
        if (toEncrypt != null) {
            if (storeType != null && null == STORE_INFO_TYPE_CACHE.putIfAbsent(storeType, toEncrypt)) {
                LOGGER.fine("Cached encryption fields for storeType " + storeType);
            }
            return toEncrypt;
        }

        toEncrypt = CACHE.get(info.getClass());
        if (toEncrypt != null) {
            return toEncrypt;
        }

        toEncrypt = Collections.emptySet();
        if (info.getConnectionParameters() != null) {
            toEncrypt = new HashSet<>(3);
            for (Param p : factory.getParametersInfo()) {
                if (p.isPassword()) {
                    toEncrypt.add(p.getName());
                }
            }
        }
        CACHE.put(factory.getClass(), toEncrypt);
        if (storeType != null) {
            STORE_INFO_TYPE_CACHE.put(storeType, toEncrypt);
        }
        return toEncrypt;
    }

    /**
     * Encrypts a parameter value.
     *
     * <p>If no encoder is configured then the value is returned as is.
     */
    public String encode(String value) {
        String encoderName = securityManager.getSecurityConfig().getConfigPasswordEncrypterName();
        if (encoderName != null) {
            GeoServerPasswordEncoder pwEncoder = securityManager.loadPasswordEncoder(encoderName);
            if (pwEncoder != null) {
                String prefix = pwEncoder.getPrefix();
                if (value.startsWith(prefix + GeoServerPasswordEncoder.PREFIX_DELIMTER)) {
                    throw new RuntimeException("Cannot encode a password with prefix: "
                            + prefix
                            + GeoServerPasswordEncoder.PREFIX_DELIMTER);
                }
                value = pwEncoder.encodePassword(value, null);
            }
        } else {
            LOGGER.warning("Encryption disabled, no password encoder set");
        }
        return value;
    }

    /** Decrypts previously encrypted store connection parameters. */
    public void decode(StoreInfo info) {
        Set<String> encryptedFields = getEncryptedFields(info);
        if (!encryptedFields.isEmpty() && info.getConnectionParameters() != null) {
            List<GeoServerPasswordEncoder> encoders = securityManager.loadPasswordEncoders(null, true, null);
            for (String key : info.getConnectionParameters().keySet()) {
                if (encryptedFields.contains(key)) {
                    String value = (String) info.getConnectionParameters().get(key);
                    if (value != null) {
                        info.getConnectionParameters().put(key, decode(value, encoders));
                    }
                }
            }
        }
    }

    /** Decrypts a previously encrypted value. */
    public String decode(String value) {
        return decode(value, securityManager.loadPasswordEncoders(null, true, null));
    }

    String decode(String value, List<GeoServerPasswordEncoder> encoders) {
        for (GeoServerPasswordEncoder encoder : encoders) {
            if (encoder.isReversible() == false) continue; // should not happen
            if (encoder.isResponsibleForEncoding(value)) {
                return encoder.decode(value);
            }
        }
        return value;
    }
}
