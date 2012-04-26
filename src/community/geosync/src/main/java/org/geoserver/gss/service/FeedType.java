package org.geoserver.gss.service;

/**
 * Enumeration of the different feeds this GSS can serve.
 */
public enum FeedType {
    /**
     * The Proposed Change Feed shall be used to record suggested or proposed changes to a data
     * provider's data store. Any authorized user can post entries to the Proposed Change Feed.
     * Changes shall be expressed as WFS transactions upon features in a data provider's data store.
     */
    CHANGEFEED,
    /**
     * The Resolution feed shall be used to provide feedback to users who have posted items to the
     * Proposed Change Feed. When a proposed change is accepted or rejected, the Resolution Feed is
     * updated to indicate the disposition of the change. Users can subscribe to the Resolution feed
     * to be notified when a decision is reached about their proposed change(s).
     */
    RESOLUTIONFEED,
    /**
     * The Replication Feed is a log of changes that have been applied to a data provider's data
     * store. Changes shall be recorded as WFS transactions each describing a change to a single
     * feature. Interested parties can then use the entries in the Replication feed to synchronize a
     * local copy of the data with the data provider's data store. Synchronization can be performed
     * by an agent external for the GSS or interested parties can subscribe to the Replication Feed
     * or a topic thereof and have the GSS perform the synchronization.
     */
    REPLICATIONFEED;
}
