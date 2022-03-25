/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class LayerGroupStyleListenerTest extends GeoServerSystemTestSupport {

    @Test
    public void testContainedLayerGroupStyleNameChangeOnUpdate() throws Exception {
        buildLayerGroup("testStyleRenaming", LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testStyleRenaming");
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("changeGroupStyle");
        groupStyle.setName(styleName);
        groupStyle.getStyles().add(getCatalog().getStyleByName("BasicPolygons"));
        groupStyle.getLayers().add(getCatalog().getLayerByName("cite:BasicPolygons"));
        groupInfo.getLayerGroupStyles().add(groupStyle);
        getCatalog().save(groupInfo);

        buildLayerGroup("testStyleRenamingContainer", LayerGroupInfo.Mode.SINGLE);
        LayerGroupInfo container = getCatalog().getLayerGroupByName("testStyleRenamingContainer");
        LayerGroupStyle containerStyle = new LayerGroupStyleImpl();
        StyleInfo containerStyleName = new StyleInfoImpl(getCatalog());
        containerStyleName.setName("containerStyle");
        containerStyle.setName(containerStyleName);
        containerStyle.getStyles().add(styleName);
        containerStyle.getLayers().add(groupInfo);
        container.getLayerGroupStyles().add(containerStyle);
        getCatalog().save(container);

        buildLayerGroup("testStyleRenamingContainer2", LayerGroupInfo.Mode.OPAQUE_CONTAINER);

        LayerGroupInfo container2 = getCatalog().getLayerGroupByName(container.prefixedName());
        container2.getLayers().add(groupInfo);
        container2.getStyles().add(styleName);
        getCatalog().save(container2);

        StyleInfo name = container.getLayerGroupStyles().get(0).getStyles().get(0);

        assertEquals("changeGroupStyle", name.getName());

        name = container2.getStyles().get(1);

        assertEquals("changeGroupStyle", name.getName());

        groupInfo = getCatalog().getLayerGroupByName(groupInfo.prefixedName());

        groupInfo.getLayerGroupStyles().get(0).getName().setName("changed");

        getCatalog().save(groupInfo);

        container = getCatalog().getLayerGroupByName(container.prefixedName());

        name = container.getLayerGroupStyles().get(0).getStyles().get(0);

        assertEquals("changed", name.getName());

        container2 = getCatalog().getLayerGroupByName(container2.prefixedName());

        name = container2.getStyles().get(1);

        assertEquals("changed", name.getName());
    }

    private void buildLayerGroup(String groupName, LayerGroupInfo.Mode mode) throws Exception {
        Catalog catalog = getCatalog();
        String lakes = MockData.BASIC_POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName(groupName);
        lg.setMode(mode);
        lg.getLayers().add(catalog.getLayerByName(lakes));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }
}
