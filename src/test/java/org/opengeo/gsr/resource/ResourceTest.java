package org.opengeo.gsr.resource;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;

public class ResourceTest extends CatalogRESTTestSupport {

    protected Catalog catalog;

    protected String baseURL;

    @Override
    public void setUpInternal() throws Exception {
        baseURL = "/gsr/services/";
        catalog = getCatalog();
        CatalogFactory catalogFactory = catalog.getFactory();

        NamespaceInfo ns = catalogFactory.createNamespace();
        ns.setPrefix("nsPrefix");
        ns.setURI("nsURI");

        WorkspaceInfo ws = catalogFactory.createWorkspace();
        ws.setName("LocalWorkspace");

        DataStoreInfo ds = catalogFactory.createDataStore();
        ds.setEnabled(true);
        ds.setName("dsName");
        ds.setDescription("dsDescription");
        ds.setWorkspace(ws);

        FeatureTypeInfo ft1 = catalogFactory.createFeatureType();
        ft1.setEnabled(true);
        ft1.setName("ftName");
        ft1.setAbstract("ftAbstract");
        ft1.setDescription("ftDescription");
        ft1.setStore(ds);
        ft1.setNamespace(ns);

        LayerInfo layer1 = catalogFactory.createLayer();
        layer1.setResource(ft1);
        layer1.setName("layer1");

        FeatureTypeInfo ft2 = catalogFactory.createFeatureType();
        ft2.setEnabled(true);
        ft2.setName("ftName2");
        ft2.setAbstract("ftAbstract2");
        ft2.setDescription("ftDescription2");
        ft2.setStore(ds);
        ft2.setNamespace(ns);

        LayerInfo layer2 = catalogFactory.createLayer();
        layer2.setResource(ft2);
        layer2.setName("layer2");

        LayerGroupInfo layerGroup1 = catalogFactory.createLayerGroup();
        layerGroup1.setName("layerGroup1");
        layerGroup1.getLayers().add(layer1);
        layerGroup1.getLayers().add(layer2);

        catalog.add(layer1);
        catalog.add(layer2);
        catalog.add(layerGroup1);
    }

    public void testConfig() {
        assertEquals("/gsr/services/", this.baseURL);
    }
}
