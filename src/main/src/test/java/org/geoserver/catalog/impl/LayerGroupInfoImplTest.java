package org.geoserver.catalog.impl;

import static org.junit.Assert.assertNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.junit.Before;
import org.junit.Test;

public class LayerGroupInfoImplTest {

    Catalog catalog;

    @Before
    public void setUp() throws Exception {
        catalog = new CatalogImpl();
    }

    @Test
    public void testI18NSetters() {
        LayerGroupInfo info = catalog.getFactory().createLayerGroup();

        info.setAbstract("test");
        info.setInternationalAbstract(null);
        assertNull(info.getInternationalAbstract());
        info.setInternationalTitle(null);
        assertNull(info.getInternationalTitle());
    }
}
