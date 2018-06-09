/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.geoserver.catalog.*;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public abstract class LayerGroupBaseTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // TODO: see GEOS-3040
        Catalog catalog = getCatalog();
        String lakes = MockData.LAKES.getLocalPart();
        String forests = MockData.FORESTS.getLocalPart();
        String bridges = MockData.BRIDGES.getLocalPart();

        setNativeBox(catalog, lakes);
        setNativeBox(catalog, forests);
        setNativeBox(catalog, bridges);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lakes");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        lg.getLayers().add(catalog.getLayerByName(forests));
        lg.getStyles().add(catalog.getStyleByName(forests));
        lg.getLayers().add(catalog.getLayerByName(bridges));
        lg.getStyles().add(catalog.getStyleByName(bridges));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        WorkspaceInfo ws = catalog.getWorkspaceByName("cite");
        LayerGroupInfo wslg = catalog.getFactory().createLayerGroup();

        wslg.setName("bridges");
        wslg.setWorkspace(ws);
        wslg.getLayers().add(catalog.getLayerByName(bridges));
        wslg.getStyles().add(catalog.getStyleByName(bridges));
        builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(wslg);
        catalog.add(wslg);

        lg = catalog.getFactory().createLayerGroup();
        lg.setName("nestedLayerGroup");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        testData.addStyle(
                "multiStyleGroup",
                "multiStyleGroup.sld",
                CatalogIntegrationTest.class,
                getCatalog());
        lg = catalog.getFactory().createLayerGroup();
        lg.setName("styleGroup");
        lg.getLayers().add(null);
        lg.getStyles().add(catalog.getStyleByName("multiStyleGroup"));
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(
                new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }
}
