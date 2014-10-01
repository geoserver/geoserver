/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geotools.data.DataUtilities;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertNotNull;

public class StyleGeneratorTest {

    @Test
    public void testCreateInWorkspace() throws Exception {
        ResourcePool rp = createNiceMock(ResourcePool.class);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getFactory()).andReturn(new CatalogFactoryImpl(null)).anyTimes();
        expect(cat.getResourcePool()).andReturn(rp).anyTimes();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getName()).andReturn("foo").anyTimes();

        replay(rp, ft, ws, cat);

        StyleGenerator gen = new StyleGenerator(cat);
        gen.setWorkspace(ws);

        SimpleFeatureType schema = DataUtilities.createType("foo", "geom:Point");
        StyleInfo style = gen.createStyle(ft, schema);
        assertNotNull(style);
        assertNotNull(style.getWorkspace());
    }
}
