/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.util.GrowableInternationalString;
import org.junit.Before;
import org.junit.Test;

public class FeatureTypeInfoImplTest {

    Catalog catalog;

    @Before
    public void setUp() throws Exception {
        GeoServerExtensionsHelper.setIsSpringContext(false);
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

        // AttributeTypeInfoImpl's source and nillable are derived properties if unset, make sure
        // they don't affect equality checks
        assertEquals(at1.getName(), at1.getSource());
        assertEquals(at1.getSource(), at2.getSource());
        assertEquals(at1, at2);
        assertEquals(at1.hashCode(), at2.hashCode());
        at1.setSource(at1.getSource());
        assertEquals(at1, at2);
        assertEquals(at1.hashCode(), at2.hashCode());

        assertTrue(at1.isNillable());
        assertTrue(at2.isNillable());
        at1.setNillable(true);
        assertEquals(at1, at2);
        assertEquals(at1.hashCode(), at2.hashCode());
    }

    @Test
    public void testI18nSetters() {
        CatalogFactory factory = catalog.getFactory();

        FeatureTypeInfoImpl ft1 = (FeatureTypeInfoImpl) factory.createFeatureType();

        ft1.setAbstract("test");
        ft1.setInternationalAbstract(null);
        assertNull(ft1.getInternationalAbstract());
        ft1.setInternationalTitle(null);
        assertNull(ft1.getInternationalTitle());
    }

    @Test
    public void testEqualityI18nTitle() throws Exception {
        FeatureTypeInfoImpl f1 = new FeatureTypeInfoImpl();
        FeatureTypeInfoImpl f2 = new FeatureTypeInfoImpl();

        // initialize both with the same state, no title but i18n title available
        Consumer<FeatureTypeInfoImpl> initer =
                f -> {
                    GrowableInternationalString title =
                            new GrowableInternationalString("default language");
                    title.add(Locale.ITALIAN, "lingua italiana");
                    f.setInternationalTitle(title);
                };
        initer.accept(f1);
        initer.accept(f2);

        assertEquals(f1, f2);
    }

    @Test
    public void testEqualityI18nAbstract() throws Exception {
        FeatureTypeInfoImpl f1 = new FeatureTypeInfoImpl();
        FeatureTypeInfoImpl f2 = new FeatureTypeInfoImpl();

        // initialize both with the same state, no abstract but i18n abstract available
        Consumer<FeatureTypeInfoImpl> initer =
                f -> {
                    GrowableInternationalString abs =
                            new GrowableInternationalString("default language");
                    abs.add(Locale.ITALIAN, "lingua italiana");
                    f.setInternationalAbstract(abs);
                };
        initer.accept(f1);
        initer.accept(f2);

        assertEquals(f1, f2);
    }
}
