/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.remote.v2_0;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.util.NullProgressListener;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.util.decorate.Wrapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains integration tests related with GeoServer WFS 2.0 remote store. */
public class WfsRemoteStoreTest extends WFS20TestSupport {

    @Test
    public void testAddRemoteWfsLayer() throws Exception {

        // configure the test environment
        Map<String, String> environment = new HashMap<>();
        // get tiger roads schemas location
        URL tigerRoadsSchema =
                WfsRemoteStoreTest.class.getResource("tiger_roads_describe_feature_type.xml");
        assertThat(tigerRoadsSchema, notNullValue());
        environment.put("TIGER_ROADS_SCHEMA_LOCATION", tigerRoadsSchema.toString());

        // setup http mock for the remote WFS server
        MockHttpClient httpClient = new MockHttpClient();
        // register the http calls the http mock should expect
        registerHttpGetFromResource(
                httpClient,
                "/ows?REQUEST=DescribeFeatureType"
                        + "&VERSION=2.0.0"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&NAMESPACES=xmlns%28tiger%2Chttp%3A%2F%2Fwww.census.gov%29"
                        + "&SERVICE=WFS",
                "tiger_roads_describe_feature_type.xml");
        registerHttpGetFromResource(
                httpClient,
                "/ows?PROPERTYNAME=the_geom"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&REQUEST=GetFeature"
                        + "&RESULTTYPE=RESULTS"
                        + "&OUTPUTFORMAT=application%2Fgml%2Bxml%3B+version%3D3.2"
                        + "&VERSION=2.0.0"
                        + "&SERVICE=WFS",
                "tiger_roads_get_feature_resp_1.xml",
                environment);
        registerHttpGetFromResource(
                httpClient,
                "/ows?PROPERTYNAME=the_geom%2CCFCC%2CNAME"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&REQUEST=GetFeature"
                        + "&RESULTTYPE=RESULTS"
                        + "&OUTPUTFORMAT=application%2Fgml%2Bxml%3B+version%3D3.2"
                        + "&VERSION=2.0.0"
                        + "&SERVICE=WFS",
                "tiger_roads_get_feature_resp_1.xml",
                environment);

        try {
            // add the remote wfs store using a file capabilities document
            DataStoreInfo storeInfo =
                    createWfsDataStore(
                            getCatalog(), "RemoteWfsStore", "remote_wfs_capabilities.xml");

            // retrieve the wfs store and make sure we set it to use our mocked http client
            DataAccess dataStore = storeInfo.getDataStore(new NullProgressListener());
            WFSDataStore wfsDatStore = extractWfsDataStore(dataStore);
            wfsDatStore.getWfsClient().setHttpClient(httpClient);

            // create a feature based on a remote layer (feature type)
            createWfsRemoteLayer(getCatalog(), storeInfo, "tiger_tiger_roads");

            // perform a get feature request to make sure the layer works properly
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wfs?request=GetFeature&typenames=gs:tiger_tiger_roads&version=2.0.0&service=wfs");
            assertThat(response.getStatus(), is(200));
            String content = response.getContentAsString();
            assertThat(content, notNullValue());
            assertThat(content.contains("numberMatched=\"1\" numberReturned=\"1\""), is(true));
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

    @Test
    public void testAddRemoteWfsLayerSpecialChars() throws Exception {

        // configure the test environment
        Map<String, String> environment = new HashMap<>();
        // get tiger roads schemas location
        URL tigerRoadsSchema =
                WfsRemoteStoreTest.class.getResource(
                        "tiger_roads_describe_feature_type_special_chars.xml");
        assertThat(tigerRoadsSchema, notNullValue());
        environment.put("TIGER_ROADS_SCHEMA_LOCATION", tigerRoadsSchema.toString());

        // setup http mock for the remote WFS server
        MockHttpClient httpClient = new MockHttpClient();
        // register the http calls the http mock should expect
        registerHttpGetFromResource(
                httpClient,
                "/ows?REQUEST=DescribeFeatureType"
                        + "&VERSION=2.0.0"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&NAMESPACES=xmlns%28tiger%2Chttp%3A%2F%2Fwww.census.gov%29"
                        + "&SERVICE=WFS",
                "tiger_roads_describe_feature_type_special_chars.xml");
        registerHttpGetFromResource(
                httpClient,
                "/ows?PROPERTYNAME=the_geom"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&REQUEST=GetFeature"
                        + "&RESULTTYPE=RESULTS"
                        + "&OUTPUTFORMAT=application%2Fgml%2Bxml%3B+version%3D3.2"
                        + "&VERSION=2.0.0"
                        + "&SERVICE=WFS",
                "tiger_roads_get_feature_resp_special_chars.xml",
                environment);
        registerHttpGetFromResource(
                httpClient,
                "/ows?PROPERTYNAME=the_geom%2C%C3%A6nd%2C%C3%B8st%2Cn%C3%B8j"
                        + "&TYPENAMES=tiger%3Atiger_roads"
                        + "&REQUEST=GetFeature"
                        + "&RESULTTYPE=RESULTS"
                        + "&OUTPUTFORMAT=application%2Fgml%2Bxml%3B+version%3D3.2"
                        + "&VERSION=2.0.0"
                        + "&SERVICE=WFS",
                "tiger_roads_get_feature_resp_special_chars.xml",
                environment);

        try {
            // add the remote wfs store using a file capabilities document
            DataStoreInfo storeInfo =
                    createWfsDataStore(
                            getCatalog(), "RemoteWfsStore", "remote_wfs_capabilities.xml");

            // retrieve the wfs store and make sure we set it to use our mocked http client
            DataAccess dataStore = storeInfo.getDataStore(new NullProgressListener());
            WFSDataStore wfsDatStore = extractWfsDataStore(dataStore);
            wfsDatStore.getWfsClient().setHttpClient(httpClient);

            // create a feature based on a remote layer (feature type)
            createWfsRemoteLayer(getCatalog(), storeInfo, "tiger_tiger_roads");

            // perform a get feature request to make sure the layer works properly
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wfs?request=GetFeature&typenames=gs:tiger_tiger_roads&version=2.0.0&service=wfs");
            assertThat(response.getStatus(), is(200));
            String content = response.getContentAsString();
            assertThat(content, notNullValue());
            assertThat(content.contains("numberMatched=\"1\" numberReturned=\"1\""), is(true));
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
        catalog.add(storeInfo);
        // return the wfs data store we just build
        storeInfo = catalog.getStoreByName(name, DataStoreInfo.class);
        assertThat(storeInfo, notNullValue());
        return storeInfo;
    }

    /**
     * Help method that register in the provided mock HTTP client an HTTP GET URL and the respective
     * response, the response will be retrieved from the provided resource name which should be
     * available in the class-path. The provided environment map will be used to substitute any
     * matching place holder.
     */
    private static void registerHttpGetFromResource(
            MockHttpClient httpClient,
            String url,
            String resourceName,
            Map<String, String> environment)
            throws Exception {
        // get the resource URL
        URL finalUrl = new URL(TestHttpClientProvider.MOCKSERVER + url);
        try (InputStream input = WfsRemoteStoreTest.class.getResourceAsStream(resourceName)) {
            assertThat(input, notNullValue());
            // get the resource content to a string
            String content = IOUtils.toString(input);
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                // if a placeholder exists replace it whit the corresponding value
                content = content.replaceAll("\\$\\{" + entry.getKey() + "}", entry.getValue());
            }
            // use the content resolved with the provided environment
            httpClient.expectGet(finalUrl, new MockHttpResponse(content, "text/xml"));
        }
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
