/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.junit.Before;
import org.junit.Test;

public class FeatureTypeInfoImplTest {

    Catalog catalog;

    @Before
    public void setUp() throws Exception {
        catalog = new CatalogImpl();
    }

    @Test
    public void testEqualsWithAttributes() {
        CatalogFactory factory = catalog.getFactory();

        FeatureTypeInfoImpl ft1 = (FeatureTypeInfoImpl) factory.createFeatureType();
        FeatureTypeInfoImpl ft2 = (FeatureTypeInfoImpl) factory.createFeatureType();

        ft1.setName("featureType");
        ft2.setName("featureType");

        AttributeTypeInfo at1 = factory.createAttribute();
        AttributeTypeInfo at2 = factory.createAttribute();

        at1.setName("attribute");
        at2.setName("attribute");

        at1.setFeatureType(ft1);
        at2.setFeatureType(ft2);

        ft1.setAttributes(Collections.singletonList(at1));
        ft2.setAttributes(Collections.singletonList(at2));

        assertEquals(ft1, ft2);
    }
}
