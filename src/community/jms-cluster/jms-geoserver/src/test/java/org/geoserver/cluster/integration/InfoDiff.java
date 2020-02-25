/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import org.geoserver.catalog.Info;

/**
 * Represents a difference between two GeoServe instances. If one of the object is NULL it means
 * that it doesn't exists in one of the GeoServer instances. For example, if we are comparing
 * GeoServer A with GeoServer B and if info A is NULL it means that info B doesn't exists in
 * GeoServer A.
 */
public final class InfoDiff {

    private final Info infoA;
    private final Info infoB;

    public InfoDiff(Info infoA, Info infoB) {
        if (infoA == null && infoB == null) {
            // if both object are NULL it means that there is no difference
            throw new RuntimeException("Both infos are NULL.");
        }
        this.infoA = infoA;
        this.infoB = infoB;
    }

    public Info getInfoA() {
        return infoA;
    }

    public Info getInfoB() {
        return infoB;
    }
}
