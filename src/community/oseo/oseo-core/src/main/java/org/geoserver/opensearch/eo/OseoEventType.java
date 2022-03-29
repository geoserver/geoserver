/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.List;
import org.opengis.util.CodeList;

/** Event Types for OSEO Data Store */
public class OseoEventType extends CodeList<OseoEventType> {
    private static final long serialVersionUID = -4218786755116808448L;
    private static final List<OseoEventType> VALUES = new ArrayList<>(5);

    /**
     * Notification of inserted features, before insertion occurs (collection contains newly
     * inserted features). Depending on the operation, only pre or post insert will be issued
     */
    public static final OseoEventType PRE_INSERT =
            new OseoEventType("PreInsert", "Features just inserted");

    /**
     * Notification of inserted features, after insertion occurs (collection contains newly inserted
     * features). Depending on the operation, only pre or post insert will be issued
     */
    public static final OseoEventType POST_INSERT =
            new OseoEventType("PostInsert", "Features just inserted");

    /**
     * Notification of updated features, before update occurs (collection contains original values)
     */
    public static final OseoEventType PRE_UPDATE =
            new OseoEventType("PreUpdate", "Feature values before update");

    /**
     * Notification of updated features, after update occurs (collection contains updated features)
     */
    public static final OseoEventType POST_UPDATE =
            new OseoEventType("PostUpdate", "Feature values after update");

    /**
     * Notification of deleted features, before deletion occurs (collection contains features that
     * will be deleted)
     */
    public static final OseoEventType PRE_DELETE =
            new OseoEventType("PreDelete", "Features just deleted");

    protected OseoEventType(String name, String description) {
        super(name, VALUES);
    }

    @Override
    public OseoEventType[] family() {
        synchronized (VALUES) {
            return VALUES.toArray(new OseoEventType[VALUES.size()]);
        }
    }
}
