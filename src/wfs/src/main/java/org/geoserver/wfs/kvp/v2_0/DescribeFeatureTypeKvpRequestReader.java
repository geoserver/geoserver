/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.wfs20.DescribeFeatureTypeType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.catalog.Catalog;

public class DescribeFeatureTypeKvpRequestReader
        extends org.geoserver.wfs.kvp.DescribeFeatureTypeKvpRequestReader {

    public DescribeFeatureTypeKvpRequestReader(final Catalog catalog) {
        super(catalog, DescribeFeatureTypeType.class, Wfs20Factory.eINSTANCE);
    }
}
