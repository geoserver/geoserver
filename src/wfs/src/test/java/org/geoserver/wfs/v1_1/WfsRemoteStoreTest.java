/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.util.NullProgressListener;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.util.decorate.Wrapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains integration tests related with GeoServer WFS 2.0 remote store. */
public class WfsRemoteStoreTest extends WFSTestSupport {

    public MockHttpClient httpClient = new MockHttpClient();

    @Test
    public void testAddRemoteWfsLayer20() throws Exception {

        // register the http calls the http mock should expect
        registerHttpGetFromResource(
                httpClient,
                "/wfs?NAMESPACE=xmlns%28topp%3Dhttp%3A%2F%2Fwww.topp.com%29&TYPENAME=topp%3Aroads22&REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS",
                "desc_feature.xml");
        registerHttpGetFromResource(
                httpClient,
                "/wfs?REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS",
                "desc_110.xml");

        // RESPONSE URLs
        registerHttpGetFromResource(
                httpClient,
                "/wfs?PROPERTYNAME=the_geom%2Ccat%2Clabel&FILTER=%3Cogc%3AFilter+xmlns%3Axs%3D%22http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%22+xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22+xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3Cogc%3ABBOX%3E%3Cogc%3APropertyName%2F%3E%3Cgml%3AEnvelope+srsDimension%3D%222%22+srsName%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%2Fsrs%2Fepsg.xml%234326%22%3E%3Cgml%3AlowerCorner%3E-103.73937+43.47669%3C%2Fgml%3AlowerCorner%3E%3Cgml%3AupperCorner%3E-102.739377+44.47536%3C%2Fgml%3AupperCorner%3E%3C%2Fgml%3AEnvelope%3E%3C%2Fogc%3ABBOX%3E%3C%2Fogc%3AFilter%3E&TYPENAME=topp%3Aroads22&REQUEST=GetFeature&RESULTTYPE=HITS&OUTPUTFORMAT=text%2Fxml%3B+subtype%3Dgml%2F3.1.1&SRSNAME=EPSG%3A4326&VERSION=1.1.0&MAXFEATURES=10&SERVICE=WFS",
                "wfs_response_4326.xml");
        registerHttpGetFromResource(
                httpClient,
                "/wfs?PROPERTYNAME=the_geom%2Ccat%2Clabel&FILTER=%3Cogc%3AFilter+xmlns%3Axs%3D%22http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%22+xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22+xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3Cogc%3ABBOX%3E%3Cogc%3APropertyName%2F%3E%3Cgml%3AEnvelope+srsDimension%3D%222%22+srsName%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%2Fsrs%2Fepsg.xml%234326%22%3E%3Cgml%3AlowerCorner%3E-103.73937+43.47669%3C%2Fgml%3AlowerCorner%3E%3Cgml%3AupperCorner%3E-102.739377+44.47536%3C%2Fgml%3AupperCorner%3E%3C%2Fgml%3AEnvelope%3E%3C%2Fogc%3ABBOX%3E%3C%2Fogc%3AFilter%3E&TYPENAME=topp%3Aroads22&REQUEST=GetFeature&RESULTTYPE=RESULTS&OUTPUTFORMAT=text%2Fxml%3B+subtype%3Dgml%2F3.1.1&SRSNAME=EPSG%3A4326&VERSION=1.1.0&MAXFEATURES=10&SERVICE=WFS",
                "wfs_response_4326.xml");
        registerHttpGetFromResource(
                httpClient,
                "/wfs?PROPERTYNAME=the_geom&FILTER=%3Cogc%3AFilter+xmlns%3Axs%3D%22http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%22+xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22+xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3Cogc%3ABBOX%3E%3Cogc%3APropertyName%2F%3E%3Cgml%3AEnvelope+srsDimension%3D%222%22+srsName%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%2Fsrs%2Fepsg.xml%234326%22%3E%3Cgml%3AlowerCorner%3E-103.73937+43.47669%3C%2Fgml%3AlowerCorner%3E%3Cgml%3AupperCorner%3E-102.739377+44.47536%3C%2Fgml%3AupperCorner%3E%3C%2Fgml%3AEnvelope%3E%3C%2Fogc%3ABBOX%3E%3C%2Fogc%3AFilter%3E&TYPENAME=topp%3Aroads22&REQUEST=GetFeature&RESULTTYPE=RESULTS&OUTPUTFORMAT=text%2Fxml%3B+subtype%3Dgml%2F3.1.1&SRSNAME=EPSG%3A4326&VERSION=1.1.0&MAXFEATURES=10&SERVICE=WFS",
                "wfs_response_4326.xml");

        try {
            // add the remote wfs store using a file capabilities document
            DataStoreInfo storeInfo =
                    createWfsDataStore(getCatalog(), "RemoteWfsStore", "wfs_cap_110.xml");

            // retrieve the wfs store and make sure we set it to use our mocked http client
            DataAccess dataStore = storeInfo.getDataStore(new NullProgressListener());
            WFSDataStore wfsDatStore = extractWfsDataStore(dataStore);
            wfsDatStore.getWfsClient().setHttpClient(httpClient);

            // create a feature based on a remote layer (feature type)
            createWfsRemoteLayer(getCatalog(), storeInfo, "topp_roads22");

            // perform a get feature request to make sure the layer works properly
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gs:topp_roads22&srsName=EPSG:4326&bbox=-103.73937,43.47669,-102.739377,44.47536,EPSG:4326&maxFeatures=10");
            assertThat(response.getStatus(), is(200));
            String content = response.getContentAsString();
            assertThat(content, notNullValue());
            // assertThat(content.contains("numberMatched=\"1\" numberReturned=\"1\""), is(true));
        } finally {
            // let's clean the catalog
            DataStoreInfo dataStoreInfo =
                    getCatalog().getStoreByName("RemoteWfsStore", DataStoreInfo.class);
            if (dataStoreInfo != null) {
                // the store exists let's remove it, the associated layer will also be removed
                CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(getCatalog());
                dataStoreInfo.accept(visitor);
            }
        }
    }

    /**
     * Helper method that creates a layer in GeoServer catalog from a WFS remote store, the provided
     * layer name should match an entry on the remote WFS server.
     */
    private static LayerInfo createWfsRemoteLayer(
            Catalog catalog, DataStoreInfo storeInfo, String name) throws Exception {
        // let's create the feature type based on the remote layer capabilities description
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        catalogBuilder.setStore(storeInfo);
        // the following call will trigger a describe feature type call to the remote server
        FeatureTypeInfo featureTypeInfo = catalogBuilder.buildFeatureType(new NameImpl("", name));
        featureTypeInfo.getMetadata().put(FeatureTypeInfo.OTHER_SRS, "EPSG:4326,EPSG:3857");
        catalog.add(featureTypeInfo);
        // create the layer info based on the feature type info we just created
        LayerInfo layerInfo = catalogBuilder.buildLayer(featureTypeInfo);
        catalog.add(layerInfo);
        // return the layer info we just created
        layerInfo = catalog.getLayerByName(name);
        assertThat(layerInfo, notNullValue());
        return layerInfo;
    }

    /**
     * Helper method that creates a WFS data store based ont he provided capabilities document,
     * which should be a resource available in the classpath. The created store will be saved in the
     * GeoServer catalog on the default workspace.
     */
    private static DataStoreInfo createWfsDataStore(
            Catalog catalog, String name, String capabilitiesDocument) {
        // get the capabilities document url
        URL url = WfsRemoteStoreTest.class.getResource(capabilitiesDocument);
        assertThat(url, notNullValue());
        // build the wfs data store
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        DataStoreInfo storeInfo = catalogBuilder.buildDataStore(name);
        storeInfo.setType("Web Feature Server (NG)");
        storeInfo.getConnectionParameters().put(WFSDataStoreFactory.URL.key, url);
        storeInfo.getConnectionParameters().put(WFSDataStoreFactory.PROTOCOL.key, Boolean.FALSE);
        // local capabilities document are only supported in testing mode
        storeInfo.getConnectionParameters().put("TESTING", Boolean.TRUE);
        // enable remote re-projection
        storeInfo
                .getConnectionParameters()
                .put(WFSDataStoreFactory.USEDEFAULTSRS.key, Boolean.FALSE);
        catalog.add(storeInfo);
        // return the wfs data store we just build
        storeInfo = catalog.getStoreByName(name, DataStoreInfo.class);
        assertThat(storeInfo, notNullValue());
        return storeInfo;
    }

    /**
     * Help method that register in the provided mock HTTP client an HTTP GET URL and the respective
     * response, the response will be retrieved from the provided resource name which should be
     * available in the class-path.
     */
    private static void registerHttpGetFromResource(
            MockHttpClient httpClient, String url, String resourceName) throws Exception {
        URL finalUrl = new URL(TestHttpClientProvider.MOCKSERVER + url);
        URL resourceUrl = WfsRemoteStoreTest.class.getResource(resourceName);
        assertThat(resourceUrl, notNullValue());
        httpClient.expectGet(finalUrl, new MockHttpResponse(resourceUrl, "text/xml"));
    }

    /**
     * Helper method that obtains a WFS data store from a generic data access by unwrapping it if
     * necessary.
     */
    private static WFSDataStore extractWfsDataStore(DataAccess dataStore) {
        assertThat(dataStore, notNullValue());
        if (dataStore instanceof Wrapper) {
            // an exception will be throw if no wfs data store can be found
            return ((Wrapper) dataStore).unwrap(WFSDataStore.class);
        }
        assertThat(dataStore, instanceOf(WFSDataStore.class));
        return (WFSDataStore) dataStore;
    }
}
