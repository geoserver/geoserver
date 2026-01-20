/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;

public class GeopkgPagedUniqueTest extends WPSTestSupport {

    static final String FIELD_NAME = "NAME";

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        super.setUpInternal(data);

        Catalog cat = getCatalog();
        WorkspaceInfo ws = cat.getWorkspaceByName(CiteTestData.CITE_PREFIX);
        // 1) create GeoPackage datastore in the 'cite' workspace
        DataStoreInfo geopkg = cat.getFactory().createDataStore();
        geopkg.setName("gpkgforests");
        geopkg.setWorkspace(ws);
        geopkg.setEnabled(true);

        Map<String, Serializable> params = geopkg.getConnectionParameters();
        params.put("dbtype", "geopkg");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/forests.gpkg");
        cat.add(geopkg);

        // 2) create schema + load SystemTestData.FORESTS
        DataStore ds = (DataStore) geopkg.getDataStore(null);
        SimpleFeatureSource src = getFeatureSource(SystemTestData.FORESTS);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(src.getSchema());
        ds.createSchema(tb.buildFeatureType());

        SimpleFeatureStore store = (SimpleFeatureStore) ds.getFeatureSource(SystemTestData.FORESTS.getLocalPart());
        store.addFeatures(src.getFeatures());

        // 3) publish the new layer under the 'cite' workspace
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setWorkspace(ws);
        cb.setStore(geopkg);
        FeatureTypeInfo ft = cb.buildFeatureType(store);
        ft.setEnabled(true);
        cat.add(ft);

        LayerInfo layer = cb.buildLayer(ft);
        layer.setEnabled(true);
        layer.setAdvertised(true);
        cat.add(layer);
    }

    @Test
    public void testDescOrder() throws Exception {
        // build WPS request, referencing the WFS endpoint directly
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute service=\"WPS\" version=\"1.0.0\"\n"
                + "    xmlns=\"http://www.opengis.net/wps/1.0.0\"\n"
                + "    xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"\n"
                + "    xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
                + "    xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:PagedUnique</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml\"\n"
                + "                     xlink:href=\"http://geoserver/wfs\"\n"
                + "                     method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                + "            <wfs:Query typeName=\"" + CiteTestData.CITE_PREFIX + ":Forests\">\n"
                + "              <ogc:SortBy>\n"
                + "                <ogc:SortProperty>\n"
                + "                  <ogc:PropertyName>" + FIELD_NAME + "</ogc:PropertyName>\n"
                + "                  <ogc:SortOrder>DESC</ogc:SortOrder>\n"
                + "                </ogc:SortProperty>\n"
                + "              </ogc:SortBy>\n"
                + "            </wfs:Query>\n"
                + "          </wfs:GetFeature>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>fieldName</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>" + FIELD_NAME + "</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>startIndex</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>0</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>maxFeatures</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>100</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput mimeType=\"application/json\">\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";

        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");

        List<String> list = new ArrayList<>();
        for (Object o : values) list.add((String) o);

        assertTrue(
                "Values should be in descending order",
                Ordering.<String>natural().reverse().isOrdered(list));
    }
}
