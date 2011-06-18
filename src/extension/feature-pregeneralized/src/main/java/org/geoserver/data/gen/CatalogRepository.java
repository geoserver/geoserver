/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.data.gen;

import org.geotools.data.Repository;

/**
 * @author Christian Mueller
 * 
 * Repository implementation using the geoserver catalog
 * 
 * @deprecated use {@link org.geoserver.catalog.CatalogRepository}
 */
public class CatalogRepository extends org.geoserver.catalog.CatalogRepository implements Repository {

    public CatalogRepository() {
    }
}
