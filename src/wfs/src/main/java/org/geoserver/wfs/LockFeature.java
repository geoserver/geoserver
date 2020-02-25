/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.Lock;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.LockFeatureResponse;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.InProcessLockingManager;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Web Feature Service 1.0 LockFeature Operation.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LockFeature {
    /** The logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    /** Web Feature Service configuration */
    WFSInfo wfs;

    /** The catalog */
    Catalog catalog;

    /** Filter factory */
    FilterFactory filterFactory;

    /** */
    public LockFeature(WFSInfo wfs, Catalog catalog) {
        this(wfs, catalog, null);
    }

    public LockFeature(WFSInfo wfs, Catalog catalog, FilterFactory filterFactory) {
        this.wfs = wfs;
        this.catalog = catalog;
        this.filterFactory = filterFactory;
    }

    public void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    /**
     * Locks features according to the request.
     *
     * @return the WFS 1.1 required response
     * @throws WFSException if a lock failed and the lock specified all locks, or if an another
     *     error occurred processing the lock operation
     */
    public LockFeatureResponse lockFeature(LockFeatureRequest request) throws WFSException {
        FeatureLock fLock = null;

        try {
            // check we are dealing with a well formed request, there is at
            // least on lock request?
            List<Lock> locks = request.getLocks();

            if ((locks == null) || locks.isEmpty()) {
                String msg = "A LockFeature request must contain at least one LOCK element";
                throw new WFSException(request, msg);
            }

            LOGGER.info("locks size is " + locks.size());

            // create a new lock (token used to manage locks across datastores)
            fLock = newFeatureLock(request);

            // prepare the response object
            LockFeatureResponse response = request.createResponse();
            response.setLockId(fLock.getAuthorization());

            // go thru each lock request, and try to perform locks on a feature
            // by feature basis
            // in order to allow for both "all" and "some" lock behaviour
            // TODO: if the lock is the default this default, lock the whole
            // query directly, should be a lot faster
            for (int i = 0, n = locks.size(); i < n; i++) {
                Lock lock = locks.get(i);
                LOGGER.info("curLock is " + lock);

                QName typeName = lock.getTypeName();

                // get out the filter, and default to no filtering if none was
                // provided
                Filter filter = lock.getFilter();

                if (filter == null) {
                    filter = Filter.INCLUDE;
                }

                FeatureTypeInfo meta;
                FeatureSource<? extends FeatureType, ? extends Feature> source;
                FeatureCollection<? extends FeatureType, ? extends Feature> features;

                try {
                    meta =
                            catalog.getFeatureTypeByName(
                                    typeName.getNamespaceURI(), typeName.getLocalPart());

                    if (meta == null) {
                        throw new WFSException(
                                request,
                                "Unknown feature type "
                                        + typeName.getPrefix()
                                        + ":"
                                        + typeName.getLocalPart());
                    }

                    source = meta.getFeatureSource(null, null);

                    // make sure all geometric elements in the filter have a crs, and that the
                    // filter
                    // is reprojected to store's native crs as well
                    CoordinateReferenceSystem declaredCRS =
                            WFSReprojectionUtil.getDeclaredCrs(
                                    source.getSchema(), request.getVersion());
                    filter =
                            WFSReprojectionUtil.normalizeFilterCRS(
                                    filter, source.getSchema(), declaredCRS);

                    // now gather the features
                    features = source.getFeatures(filter);

                    if (source instanceof FeatureLocking) {
                        ((FeatureLocking) source).setFeatureLock(fLock);
                    }
                } catch (IOException e) {
                    throw new WFSException(request, e);
                }

                FeatureIterator reader = null;
                int numberLocked = -1;

                try {
                    for (reader = features.features(); reader.hasNext(); ) {
                        SimpleFeature feature = (SimpleFeature) reader.next();

                        FeatureId fid = fid(feature.getID());
                        Id fidFilter = fidFilter(fid);

                        if (!(source instanceof FeatureLocking)) {
                            LOGGER.fine(
                                    "Lock "
                                            + fid
                                            + " not supported by data store (authID:"
                                            + fLock.getAuthorization()
                                            + ")");

                            response.addNotLockedFeature(fid);
                            // lockFailedFids.add(fid);
                        } else {
                            // DEFQuery is just some indirection, should be in
                            // the locking interface.
                            // int numberLocked =
                            // ((DEFQueryFeatureLocking)source).lockFeature(feature);
                            // HACK: Query.NO_NAMES isn't working in postgis
                            // right now,
                            // so we'll just use all.
                            Query query =
                                    new Query(
                                            meta.getName(),
                                            (Filter) fidFilter,
                                            Query.DEFAULT_MAX,
                                            Query.ALL_NAMES,
                                            lock.getHandle());

                            numberLocked = ((FeatureLocking) source).lockFeatures(query);

                            if (numberLocked == 1) {
                                LOGGER.fine(
                                        "Lock "
                                                + fid
                                                + " (authID:"
                                                + fLock.getAuthorization()
                                                + ")");
                                response.addLockedFeature(fid);

                                // lockedFids.add(fid);
                            } else if (numberLocked == 0) {
                                LOGGER.fine(
                                        "Lock "
                                                + fid
                                                + " conflict (authID:"
                                                + fLock.getAuthorization()
                                                + ")");
                                response.addNotLockedFeature(fid);

                                // lockFailedFids.add(fid);
                            } else {
                                LOGGER.warning(
                                        "Lock "
                                                + numberLocked
                                                + " "
                                                + fid
                                                + " (authID:"
                                                + fLock.getAuthorization()
                                                + ") duplicated FeatureID!");
                                response.addLockedFeature(fid);

                                // lockedFids.add(fid);
                            }
                        }
                    }
                } catch (IOException ioe) {
                    throw new WFSException(request, ioe);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }

                // refresh lock times, so they all start the same instant and we
                // are nearer
                // to the spec when it says the expiry should start when the
                // lock
                // feature response has been totally written
                if (numberLocked > 0) {
                    Transaction t = new DefaultTransaction();

                    try {
                        try {
                            t.addAuthorization(fLock.getAuthorization());
                            DataStore dataStore = (DataStore) source.getDataStore();
                            dataStore.getLockingManager().refresh(fLock.getAuthorization(), t);
                        } finally {
                            t.commit();
                        }
                    } catch (IOException e) {
                        throw new WFSException(request, e);
                    } finally {
                        try {
                            t.close();
                        } catch (IOException e) {
                            throw new WFSException(request, e);
                        }
                    }
                }
            }

            // should we release all? if not set default to true

            boolean lockAll = !request.isLockActionSome();

            List notLocked = response.getNotLockedFeatures();
            if (lockAll && (notLocked != null && !notLocked.isEmpty())) {
                // I think we need to release and fail when lockAll fails
                //
                // abort will release the locks
                throw new WFSException(
                        request,
                        "Could not acquire locks for:" + notLocked,
                        WFSException.CANNOT_LOCK_ALL_FEATURES);
            }

            return response;
        } catch (WFSException e) {
            // release locks when something fails
            if (fLock != null) {
                try {
                    release(fLock.getAuthorization());
                } catch (WFSException e1) {
                    // log it
                    LOGGER.log(Level.SEVERE, "Error occured releasing locks", e1);
                }
            }

            throw e;
        }
    }

    /** Release lock by authorization */
    public void release(String lockId) throws WFSException {
        try {
            List dataStores = catalog.getDataStores();

            for (Iterator i = dataStores.iterator(); i.hasNext(); ) {
                DataStoreInfo meta = (DataStoreInfo) i.next();
                DataStore dataStore = null;

                // TODO: support locking for DataAccess
                if (meta.isEnabled()) {
                    DataAccess da = meta.getDataStore(null);
                    if (da instanceof DataStore) {
                        dataStore = (DataStore) da;
                    }
                }

                if (dataStore == null) {
                    continue; // disabled or not a DataStore
                }

                LockingManager lockingManager = dataStore.getLockingManager();

                if (lockingManager == null) {
                    continue; // locks not supported
                }

                org.geotools.data.Transaction t =
                        new DefaultTransaction("Refresh " + meta.getWorkspace().getName());

                try {
                    t.addAuthorization(lockId);
                    lockingManager.release(lockId, t);

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                } finally {
                    try {
                        t.close();
                    } catch (IOException closeException) {
                        LOGGER.log(Level.FINEST, closeException.getMessage(), closeException);
                    }
                }
            }
        } catch (Exception e) {
            throw new WFSException(e);
        }
    }

    /**
     * Release all feature locks currently held.
     *
     * <p>This is the implementation for the Admin "free lock" action, transaction locks are not
     * released.
     */
    public void releaseAll() throws WFSException {
        try {
            List dataStores = catalog.getDataStores();

            for (Iterator i = dataStores.iterator(); i.hasNext(); ) {
                DataStoreInfo meta = (DataStoreInfo) i.next();
                DataStore dataStore = null;

                // TODO: support locking for DataAccess
                if (meta.isEnabled()) {
                    DataAccess da = meta.getDataStore(null);
                    if (da instanceof DataStore) {
                        dataStore = (DataStore) da;
                    }
                }

                if (dataStore == null) {
                    continue; // disabled or not a DataStore
                }

                LockingManager lockingManager = dataStore.getLockingManager();

                if (lockingManager == null) {
                    continue; // locks not supported
                }

                // TODO: implement LockingManger.releaseAll()
                // count += lockingManager.releaseAll();
            }
        } catch (Exception e) {
            throw new WFSException(e);
        }
    }

    public boolean exists(String lockId) throws WFSException {
        try {
            List dataStores = catalog.getDataStores();

            for (Iterator i = dataStores.iterator(); i.hasNext(); ) {
                DataStoreInfo meta = (DataStoreInfo) i.next();
                DataStore dataStore = null;

                // TODO: support locking for DataAccess
                if (meta.isEnabled()) {
                    DataAccess da = meta.getDataStore(null);
                    if (da instanceof DataStore) {
                        dataStore = (DataStore) da;
                    }
                }

                if (dataStore == null) {
                    continue; // disabled or not a DataStore
                }

                LockingManager lockingManager = dataStore.getLockingManager();

                if (lockingManager == null) {
                    continue; // locks not supported
                }

                if (lockingManager.exists(lockId)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            throw new WFSException(e);
        }
    }

    public void refresh(String lockId, boolean throwOnRefreshFail) throws WFSException {
        boolean refresh = false;
        boolean lockFound = false;
        try {
            List dataStores = catalog.getDataStores();

            // check for lock existance

            for (Iterator i = dataStores.iterator(); i.hasNext(); ) {
                DataStoreInfo meta = (DataStoreInfo) i.next();
                DataStore dataStore = null;

                // TODO: support locking for DataAccess
                if (meta.isEnabled()) {
                    DataAccess da = meta.getDataStore(null);
                    if (da instanceof DataStore) {
                        dataStore = (DataStore) da;
                    }
                }

                if (dataStore == null) {
                    continue; // disabled or not a DataStore
                }

                LockingManager lockingManager = dataStore.getLockingManager();

                if (lockingManager == null) {
                    continue; // locks not supported
                }

                // calling "exists" clears an expired lock, do this instead to verify existence
                // since for the past 10+ years InProcessLockingManager has been the only game in
                // town
                if (lockingManager instanceof InProcessLockingManager) {
                    InProcessLockingManager ip = (InProcessLockingManager) lockingManager;
                    Set<InProcessLockingManager.Lock> locks = ip.allLocks();
                    if (locks != null) {
                        lockFound |= locks.stream().anyMatch(l -> l.isMatch(lockId));
                    }
                }

                org.geotools.data.Transaction t =
                        new DefaultTransaction("Refresh " + meta.getWorkspace().getName());

                try {
                    t.addAuthorization(lockId);

                    if (lockingManager.refresh(lockId, t)) {
                        refresh = true;
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                } finally {
                    try {
                        t.close();
                    } catch (IOException closeException) {
                        LOGGER.log(Level.FINEST, closeException.getMessage(), closeException);
                    }
                }
            }
        } catch (Exception e) {
            throw new WFSException(e);
        }

        // the API does not give us a way to check if a lock exists but it's expired, but WFS 2.0
        // requires to send back a different response... we'll make a guess
        if (!refresh && throwOnRefreshFail) {
            if (!lockFound) {
                throw new ServiceException(
                        "Unknown lock id", WFSException.INVALID_LOCK_ID, "lockId");
            } else {
                throw new ServiceException(
                        "Lock has expired", WFSException.LOCK_HAS_EXPIRED, "lockId");
            }
        }
    }

    private FeatureId fid(String fid) {
        return filterFactory.featureId(fid);
    }

    private Id fidFilter(FeatureId fid) {
        HashSet ids = new HashSet();
        ids.add(fid);

        return filterFactory.id(ids);
    }

    protected FeatureLock newFeatureLock(LockFeatureRequest request) {
        String handle = request.getHandle();
        if ((handle == null) || handle.equals("")) {
            handle = "GeoServer";
            request.setHandle(handle);
        }

        BigInteger expiry = request.getExpiry();
        if (expiry == null) {
            expiry = BigInteger.valueOf(0);
            request.setExpiry(expiry);
        }

        int lockExpiry = expiry.intValue();

        if (lockExpiry < 0) {
            // negative time used to query if lock is available!
            return new FeatureLock(handle, lockExpiry);
        }

        if (lockExpiry == 0) {
            // perma lock with no expiry!
            return new FeatureLock(handle, 0);
        }

        // FeatureLock is specified in minutes or seconds depending on the version
        if (request.getAdaptee() instanceof net.opengis.wfs20.LockFeatureType) {
            return new FeatureLock(handle, lockExpiry * 1000);
        } else {
            return new FeatureLock(handle, lockExpiry * 60 * 1000);
        }
    }
}
