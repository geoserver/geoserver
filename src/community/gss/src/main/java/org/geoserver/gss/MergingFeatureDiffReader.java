/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.geotools.data.FeatureDiff;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.postgis.FeatureDiffImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Merges the diffs of the various delegates into one. Delegates will be examined in order to build
 * a global diff. All changes to feaures under conflict will be ignored. This class is used to get a
 * history of all the local changes that still haven't been communicated to Central, skipping
 * Central own changes.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
class MergingFeatureDiffReader implements FeatureDiffReader {

    /**
     * The readers that we need to merge. They are supposed to be sorted
     */
    FeatureDiffReader[] delegates;

    /**
     * The lowest fromVersion
     */
    String fromVersion;

    /**
     * The highest toVersion
     */
    String toVersion;

    /**
     * The schema shared by all readers
     */
    SimpleFeatureType schema;

    /**
     * The set of fids that we have "live" FeatureDiff for. At even next() we just want to grab the
     * lowest and proces it.
     */
    TreeSet<String> sortedFids = new TreeSet<String>();

    /**
     * A map from the ids to the set of FeatureDiff gathered from the readers. Usually we'll just
     * have one of them, but we might have one per readers. The various diffs are placed in the same
     * order as the readers, there might be holes.
     */
    Map<String, FeatureDiff[]> diffHistory = new HashMap<String, FeatureDiff[]>();

    /**
     * Flag marking if we have an outstanding FeatureDiff to be processed for the corresponding
     * reader. Since the readers return diffs in feature id order we just need to keep one
     * FeatureDiff per reader at any time.
     */
    boolean[] featureRead;

    /**
     * The next difference we're going to return
     */
    FeatureDiff nextDifference;
    
    /**
     * Builds a new merging reader. Mind, the order of the delegates is important, as changes are
     * built up in order
     * 
     * @param delegates
     */
    public MergingFeatureDiffReader(FeatureDiffReader... delegates) throws IOException {
        this.delegates = new FeatureDiffReader[delegates.length];
        System.arraycopy(delegates, 0, this.delegates, 0, delegates.length);
        this.featureRead = new boolean[delegates.length];
        this.fromVersion = delegates[0].getFromVersion();
        this.toVersion = delegates[delegates.length - 1].getToVersion();
        this.schema = delegates[0].getSchema();
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public SimpleFeatureType getSchema() {
        return schema;
    }

    public String getToVersion() {
        return toVersion;
    }

    public boolean hasNext() throws IOException {
        if (nextDifference != null) {
            return true;
        }

        advance();
        while (sortedFids.size() > 0) {
            nextDifference = buildNextDiff();
            if (nextDifference != null && (nextDifference.getState() != FeatureDiff.UPDATED
                    || nextDifference.getChangedAttributes().size() > 0)) {
                return true;
            } else {
                // we grabbed the result of a revert over modifications -> two modification changes
                // that amount to no global change. Let's move to the next set of changes.
                nextDifference = null;
                advance();
            }
        }

        // if we got here it means the set of fids to process dried up
        return false;
    }

    public FeatureDiff next() throws IOException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("No more feature diffs in this reader");
        }

        FeatureDiff result = nextDifference;
        nextDifference = null;

        return result;
    }

    /**
     * Builds the next difference object using the stored fids and diffs (assumes you called
     * advance() before calling it)
     * 
     * @return
     */
    FeatureDiff buildNextDiff() {
        String fid = sortedFids.first();
        sortedFids.remove(fid);
        FeatureDiff[] history = diffHistory.remove(fid);
        // cases we need to handle
        // - we have only updates -> build a cumulative update
        // - we have insert and then updates -> build a new insert with the final values
        // - we have a remove at the end -> forget the rest, it is a remove
        // - we have some diff, then a remove and then an insert -> the remove has been rolled back
        // but we don't really know if the roll back included also the roll backs of
        // updates -> the set of changes has to be rebuilt
        SimpleFeature from = null;
        SimpleFeature to = null;
        boolean removed = false;
        for (int i = 0; i < history.length; i++) {
            FeatureDiff diff = history[i];
            if (diff != null) {
                if (diff.getState() == FeatureDiff.INSERTED) {
                    // is this the rollback of a removal?
                    if(removed == true) {
                        from = null;
                        to = null;
                    } else {
                        from = null;
                        to = diff.getFeature();
                    }
                    removed = false;
                } else if (diff.getState() == FeatureDiff.DELETED) {
                    if (from == null) {
                        from = diff.getOldFeature();
                    }
                    to = null;
                    removed = true;
                } else {
                    // might we have a removed flag before this update? Maybe... if Central
                    // for some reason reinserted the feature (remember we're skipping
                    // changes coming from Central. But in that case we start over fresh
                    if (removed) {
                        removed = false;
                        from = diff.getOldFeature();
                    }

                    // is this the first diff or we have a history?
                    if (from == null) {
                        from = diff.getOldFeature();
                    }
                    to = diff.getFeature();
                }
            }
        }
        
        if(from == null && to == null) {
            return null;
        }

        return new FeatureDiffImpl(from, to);
    }

    /**
     * Reads a new FeatureDiff from all readers that still don't have a pending one and that are
     * still open
     */
    void advance() throws IOException {
        for (int i = 0; i < delegates.length; i++) {
            if (delegates[i] != null && !featureRead[i]) {
                // read the next diff that is not linked to a conflicting feature
                while (delegates[i].hasNext()) {
                    // grab a new feature diff
                    FeatureDiff fd = delegates[i].next();
                    String fid = fd.getID();
                    
                    // is it about a feature id we already know about?
                    FeatureDiff[] history;
                    if (sortedFids.contains(fid)) {
                        history = diffHistory.get(fid);
                    } else {
                        history = new FeatureDiff[delegates.length];
                        diffHistory.put(fid, history);
                        sortedFids.add(fid);
                    }
                    history[i] = fd;
                    // ok, we're done
                    break;
                } 
                
                // did we exit the loop because the reader ended?
                if(!delegates[i].hasNext()) {
                    // close the delegate and get rid of it
                    delegates[i].close();
                    delegates[i] = null;
                }
            }
        }
    }

    public void close() throws IOException {
        for (int i = 0; i < delegates.length; i++) {
            if (delegates[i] != null) {
                delegates[i].close();
                delegates[i] = null;
            }
        }
    }

}
