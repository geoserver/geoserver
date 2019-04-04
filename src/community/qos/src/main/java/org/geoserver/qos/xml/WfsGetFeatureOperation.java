/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.List;

public class WfsGetFeatureOperation extends QosAbstractOperation {

    private List<WfsAdHocQueryConstraints> adHocQueryConstraints;
    private List<WfsStoredQueryConstraintsType> storedQueryConstraints;

    public WfsGetFeatureOperation() {}

    public List<WfsAdHocQueryConstraints> getAdHocQueryConstraints() {
        return adHocQueryConstraints;
    }

    public void setAdHocQueryConstraints(List<WfsAdHocQueryConstraints> adHocQueryConstraints) {
        this.adHocQueryConstraints = adHocQueryConstraints;
    }

    public List<WfsStoredQueryConstraintsType> getStoredQueryConstraints() {
        return storedQueryConstraints;
    }

    public void setStoredQueryConstraints(
            List<WfsStoredQueryConstraintsType> storedQueryConstraints) {
        this.storedQueryConstraints = storedQueryConstraints;
    }
}
