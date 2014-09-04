/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

/**
 * A GetDiff response
 * 
 * @author aaime
 */
public class GetDiffResponseType {
    QName typeName;

    long fromVersion;

    long toVersion;

    TransactionType transaction;

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

    public long getToVersion() {
        return toVersion;
    }

    public void setToVersion(long toVersion) {
        this.toVersion = toVersion;
    }

    public TransactionType getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionType transaction) {
        this.transaction = transaction;
    }

}
