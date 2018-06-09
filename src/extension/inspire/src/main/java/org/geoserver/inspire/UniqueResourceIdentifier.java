/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.io.Serializable;

/**
 * A INSPIRE download service spatial dataset identifier
 *
 * @author Andrea Aime - GeoSolutions
 */
public class UniqueResourceIdentifier implements Serializable {
    private static final long serialVersionUID = 3277074136449520282L;

    private String code;

    private String namespace;

    private String metadataURL;

    public UniqueResourceIdentifier() {}

    public UniqueResourceIdentifier(String code) {
        this.code = code;
    }

    public UniqueResourceIdentifier(String code, String namespace) {
        this.code = code;
        this.namespace = namespace;
    }

    public UniqueResourceIdentifier(String code, String namespace, String metadataURL) {
        this.code = code;
        this.namespace = namespace;
        this.metadataURL = metadataURL;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetadataURL() {
        return metadataURL;
    }

    public void setMetadataURL(String metadataURL) {
        this.metadataURL = metadataURL;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((metadataURL == null) ? 0 : metadataURL.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        UniqueResourceIdentifier other = (UniqueResourceIdentifier) obj;
        if (code == null) {
            if (other.code != null) return false;
        } else if (!code.equals(other.code)) return false;
        if (metadataURL == null) {
            if (other.metadataURL != null) return false;
        } else if (!metadataURL.equals(other.metadataURL)) return false;
        if (namespace == null) {
            if (other.namespace != null) return false;
        } else if (!namespace.equals(other.namespace)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "SpatialDataUniqueResourceIdentifier [code="
                + code
                + ", namespace="
                + namespace
                + ", metadataURL="
                + metadataURL
                + "]";
    }
}
