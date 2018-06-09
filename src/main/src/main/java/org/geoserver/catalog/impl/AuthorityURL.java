/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geotools.util.Utilities;

/**
 * Realization of {@link AuthorityURLInfo}; being a "data type" there's no {@code
 * createAuthorityURL()} method in {@code CatalogFactory}, instead directly instantiate this class.
 *
 * @author groldan
 */
public class AuthorityURL implements AuthorityURLInfo {

    private static final long serialVersionUID = 1L;

    private String name;

    private String href;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getHref() {
        return href;
    }

    @Override
    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorityURLInfo)) {
            return false;
        }
        AuthorityURLInfo oa = (AuthorityURLInfo) o;
        return Utilities.equals(name, oa.getName()) && Utilities.equals(href, oa.getHref());
    }

    @Override
    public int hashCode() {
        return Utilities.hash(name, 17) * Utilities.hash(href, 17);
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[ name: '")
                .append(name)
                .append("', href: '")
                .append(href)
                .append("']")
                .toString();
    }
}
