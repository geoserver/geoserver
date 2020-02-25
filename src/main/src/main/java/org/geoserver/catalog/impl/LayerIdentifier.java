/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.LayerIdentifierInfo;
import org.geotools.util.Utilities;

/**
 * Realization of {@link LayerIdentifierInfo}; being a "data type" there's no {@code
 * createAuthorityURL()} method in {@code CatalogFactory}, instead directly instantiate this class.
 *
 * @author groldan
 */
public class LayerIdentifier implements LayerIdentifierInfo {

    private static final long serialVersionUID = 1L;

    private String authority;

    private String identifier;

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public void setAuthority(String authorityName) {
        this.authority = authorityName;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LayerIdentifierInfo)) {
            return false;
        }
        LayerIdentifierInfo o2 = (LayerIdentifierInfo) o;
        return Utilities.equals(authority, o2.getAuthority())
                && Utilities.equals(identifier, o2.getIdentifier());
    }

    @Override
    public int hashCode() {
        return Utilities.hash(authority, 17) * Utilities.hash(identifier, 17);
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[ authority: '")
                .append(authority)
                .append("', identifier: '")
                .append(identifier)
                .append("']")
                .toString();
    }
}
