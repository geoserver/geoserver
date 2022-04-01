/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.logging.TestAppender;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.w3c.dom.Document;

public class UniqueProcessTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup an H2 datastore for the purpose of checking database delegation
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("h2");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/h2");
        cat.add(ds);

        SimpleFeatureSource fs3 = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fs3.getSchema());
        // remove the property types H2 has troubles with (including multiple geometries)
        tb.remove("surfaceProperty");
        tb.remove("curveProperty");
        tb.remove("uriProperty");
        store.createSchema(tb.buildFeatureType());
        SimpleFeatureStore targetFeatureStore =
                (SimpleFeatureStore)
                        store.getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE.getLocalPart());
        targetFeatureStore.addFeatures(fs3.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        FeatureTypeInfo ft = cb.buildFeatureType(targetFeatureStore);
        cat.add(ft);
    }

    @Test
    public void testUnique() throws Exception {
        String xml = getUniqueRequest(getLayerId(MockData.PRIMITIVEGEOFEATURE));

        Document doc = postAsDOM(root(), xml);
        // print(doc);
        assertXpathEvaluatesTo("5", "count(//gml:value)", doc);
    }

    @Test
    public void testUniqueDatabaseDelegation() throws Exception {
        Logger logger = Logging.getLogger(JDBCDataStore.class);
        Level previousLevel = logger.getLevel();
        logger.info("hello world");
        try (TestAppender appender = TestAppender.createAppender("testAutoCodeLogsErrors", null)) {
            appender.startRecording("org.geotools.jdbc");

            logger.setLevel(Level.FINE);
            logger.fine("this is fine");

            String xml = getUniqueRequest("gs:PrimitiveGeoFeature");

            Document doc = postAsDOM(root(), xml);
            assertXpathEvaluatesTo("5", "count(//gml:value)", doc);

            appender.assertTrue(
                    "Function has not been delegated to database, could not find select distinct in the logs",
                    "SELECT distinct(\"intProperty\")");

            appender.stopRecording("org.geotools.jdbc");
        } finally {

            logger.setLevel(previousLevel);
        }
    }

    public String getUniqueRequest(String layerId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:Unique</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                + "            <wfs:Query typeName=\""
                + layerId
                + "\"/>\n"
                + "          </wfs:GetFeature>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>attribute</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>intProperty</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";
    }
}
