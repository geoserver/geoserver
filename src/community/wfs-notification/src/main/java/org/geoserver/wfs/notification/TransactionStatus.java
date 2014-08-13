/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.Transaction;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;

/**
 * An object that lives during the course of a transaction.
 */
class TransactionStatus {
    private Transaction transaction;

    /**
     * Incoming sets of features to check on right before commit.
     */
    private final Map<Name, Set<Identifier>> affected = new HashMap<Name, Set<Identifier>>();

    /**
     * Sets of "related features" found before CRUD statements were executed, organized by typename.
     */
    private final Map<Name, Set<Identifier>> potentiallyModified = new HashMap<Name, Set<Identifier>>();

    /**
     * Queries that should be re-run to check for modifications to features in {@link #potentiallyModified},
     * organized by typename.
     */
    //final Map<Name, Collection<Pair<FeatureSource, Filter>>> queriesToCheck = new HashMap<Name, Collection<Pair<FeatureSource,Filter>>>();

    /**
     * Features relevant to the above, mapped by FID. (Feature don't fit very well into {@link Set}s.
     */
    private final Map<Identifier, Feature> fidMap = new HashMap<Identifier, Feature>();
    
    private static <T, U> Set<U> getSet(T key, Map<T, Set<U>> multiMap) {
        Set<U> s = multiMap.get(key);
        if(s == null) multiMap.put(key, s = new HashSet<U>());
        return s;
    }
    
    public Map<Identifier, Feature> getFidMap() {
        return fidMap;
    }

    public Map<Name, Set<Identifier>> getAffected() {
        return affected;
    }
    
    public Map<Name, Set<Identifier>> getPotentiallyModified() {
        return potentiallyModified;
    }
    
    public Transaction getTransaction() {
        return transaction;
    }
    
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void affected(Feature f) {
        FeatureId id = f.getIdentifier();
        getSet(WFSNotify.getTypeName(f), affected).add(id);
        fidMap.put(id, f);
    }

    public void modified(Feature f) {
        FeatureId id = f.getIdentifier();
        getSet(WFSNotify.getTypeName(f), potentiallyModified).add(id);
        // Don't save references to features that are merely modified
        // fidMap.put(id, f);
    }

    /**
     * Check feature against what we have. This will remove the feature from out map of 'potentially
     * modified features' and will return TRUE if the feature was modified since the last time we saw it
     * or is new. TODO: This method just returns TRUE for now, and doesn't check equality.
     *
     * @param f
     * @return
     */
    public boolean checkFeature(Feature f) {
        Set<Identifier> fidSet = potentiallyModified.get(WFSNotify.getTypeName(f));
        if(fidSet == null) {
            return true; // We haven't seen it yet.
        }

        boolean contained = fidSet.remove(f.getIdentifier());

        if(contained) {
//                Feature f2 = fidMap.get(f.getIdentifier());
//                return !f.equals(f2);
            return true; // Just assume it has changed for now
        } else {
            return true; // We haven't seen it yet
        }
    }

    public void destroy() {
        fidMap.clear();
        affected.clear();
        potentiallyModified.clear();
    }
}