/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import javax.xml.namespace.QName;

import org.eclipse.emf.ecore.util.EcoreUtil;

import net.opengis.wfs.TransactionType;

/**
 * A PostDiff request
 * 
 * @author aaime
 */
public class PostDiffType extends GSSRequest {

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (fromVersion ^ (fromVersion >>> 32));
        result = prime * result + (int) (toVersion ^ (toVersion >>> 32));
        result = prime * result + ((transaction == null) ? 0 : transaction.hashCode());
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PostDiffType other = (PostDiffType) obj;
        if (fromVersion != other.fromVersion)
            return false;
        if (toVersion != other.toVersion)
            return false;
        if (transaction == null) {
            if (other.transaction != null)
                return false;
        } else if (!EcoreUtil.equals(transaction, other.transaction))
            return false;
        if (typeName == null) {
            if (other.typeName != null)
                return false;
        } else if (!typeName.equals(other.typeName))
            return false;
        return true;
    }

}
