/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import javax.xml.namespace.QName;

/**
 * A GetDiff request
 * 
 * @author aaime
 */
public class GetDiffType extends GSSRequest {
    QName typeName;

    long fromVersion;

    /**
     * The layer to be
     * 
     * @return
     */
    public QName getTypeName() {
        return typeName;
    }

    public void setTypeName(QName typeName) {
        this.typeName = typeName;
    }

    public long getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(long fromVersion) {
        this.fromVersion = fromVersion;
    }

}
