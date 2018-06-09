/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.gen;

import org.geotools.data.Repository;

/**
 * @author Christian Mueller
 *     <p>Repository implementation using the geoserver catalog
 * @deprecated use {@link org.geoserver.catalog.CatalogRepository}
 */
public class CatalogRepository extends org.geoserver.catalog.CatalogRepository
        implements Repository {

    public CatalogRepository() {}
}
