package org.geoserver.restconfig.model.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.geoserver.openapi.model.catalog.FeatureTypeInfo;
import org.geoserver.openapi.v1.model.ConnectionParameterEntry;
import org.geoserver.openapi.v1.model.ConnectionParameters;
import org.geoserver.openapi.v1.model.DataStoreResponse;
import org.geoserver.openapi.v1.model.DataStoreWrapper;
import org.geoserver.openapi.v1.model.DataStoresListResponse;
import org.geoserver.openapi.v1.model.FeatureTypeResponse;
import org.geoserver.openapi.v1.model.FeatureTypeResponseWrapper;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.openapi.v1.model.StyleListWrapper;
import org.geoserver.restconfig.model.SerializationTest;
import org.junit.Test;

public class CatalogResponseSerializationTest extends SerializationTest {

    public @Test void testNamedLinkSerializationHref() throws IOException {
        String raw =
                "{\"name\":\"nyc\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc.json\"}";
        NamedLink link = decode(raw, NamedLink.class);
        assertNotNull(link);
        assertEquals("nyc", link.getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/nyc.json",
                link.getHref());
        assertNull(link.getLink());
    }

    public @Test void testNamedLinkSerializationLink() throws IOException {
        String raw =
                "{\"name\":\"nyc\",\"link\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc.json\"}";
        NamedLink link = decode(raw, NamedLink.class);
        assertNotNull(link);
        assertEquals("nyc", link.getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/nyc.json",
                link.getLink());
        assertNull(link.getHref());
    }

    public @Test void testNamedLinks() throws IOException {
        String raw =
                "[{\"name\":\"nyc\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc.json\"}]";
        ParameterizedType parameterizedType = TypeUtils.parameterize(List.class, NamedLink.class);
        List<NamedLink> links = decode(raw, parameterizedType);
        assertNotNull(links);
        assertEquals(1, links.size());
        assertEquals("nyc", links.get(0).getName());
    }

    public @Test void testConectionParameterEntry() throws IOException {
        String raw = "{\"@key\":\"namespace\",\"$\":\"http:\\/\\/www.census.gov\"}";
        ConnectionParameterEntry e = decode(raw, ConnectionParameterEntry.class);
        assertNotNull(e);
        assertEquals("namespace", e.getAtKey());
        assertEquals("http://www.census.gov", e.getValue());
    }

    public @Test void testDataStoresResponse() throws IOException {
        String raw =
                "{\"dataStores\":{\"dataStore\":[{\"name\":\"nyc\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc.json\"}]}}";
        DataStoresListResponse response = decode(raw, DataStoresListResponse.class);
        assertNotNull(response);
        assertNotNull(response.getDataStores());
        List<NamedLink> dataStores = response.getDataStores().getDataStore();
        assertEquals(1, dataStores.size());
        assertEquals("nyc", dataStores.get(0).getName());
        assertNotNull(dataStores.get(0).getHref());
    }

    public @Test void testDataStoreWrapper() throws IOException {
        String raw =
                "{\"dataStore\":{\"name\":\"nyc\",\"description\":\"Topologically Integrated Geographic Encoding and Referencing (TIGER) dataset\",\"enabled\":true,\"workspace\":{\"name\":\"tiger\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger.json\"},\"connectionParameters\":{\"entry\":[{\"@key\":\"create spatial index\",\"$\":\"false\"},{\"@key\":\"memory mapped buffer\",\"$\":\"false\"},{\"@key\":\"namespace\",\"$\":\"http:\\/\\/www.census.gov\"},{\"@key\":\"cache and reuse memory maps\",\"$\":\"false\"},{\"@key\":\"url\",\"$\":\"file:data\\/nyc\"}]},\"_default\":false,\"featureTypes\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc\\/featuretypes.json\"}}";
        DataStoreWrapper decoded = decode(raw, DataStoreWrapper.class);
        assertNotNull(decoded);
        assertNotNull(decoded.getDataStore());
        assertNycStore(decoded.getDataStore());
    }

    public @Test void testDataStoreInfo() throws IOException {
        String raw =
                "{\"name\":\"nyc\",\"description\":\"Topologically Integrated Geographic Encoding and Referencing (TIGER) dataset\",\"enabled\":true,\"workspace\":{\"name\":\"tiger\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger.json\"},\"connectionParameters\":{\"entry\":[{\"@key\":\"create spatial index\",\"$\":\"false\"},{\"@key\":\"memory mapped buffer\",\"$\":\"false\"},{\"@key\":\"namespace\",\"$\":\"http:\\/\\/www.census.gov\"},{\"@key\":\"cache and reuse memory maps\",\"$\":\"false\"},{\"@key\":\"url\",\"$\":\"file:data\\/nyc\"}]},\"_default\":false,\"featureTypes\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/tiger\\/datastores\\/nyc\\/featuretypes.json\"}";
        DataStoreResponse store = decode(raw, DataStoreResponse.class);
        assertNotNull(store);
        assertNycStore(store);
    }

    private void assertNycStore(DataStoreResponse dataStoreResponse) {
        assertEquals("nyc", dataStoreResponse.getName());
        assertEquals(
                "Topologically Integrated Geographic Encoding and Referencing (TIGER) dataset",
                dataStoreResponse.getDescription());
        assertTrue(dataStoreResponse.getEnabled().booleanValue());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/nyc/featuretypes.json",
                dataStoreResponse.getFeatureTypes().toString());

        ConnectionParameters connectionParameters = dataStoreResponse.getConnectionParameters();
        assertNotNull(connectionParameters);
        List<ConnectionParameterEntry> entry = connectionParameters.getEntry();
        assertNotNull(entry);
        Map<String, String> expected = new HashMap<>();
        expected.put("create spatial index", "false");
        expected.put("memory mapped buffer", "false");
        expected.put("namespace", "http://www.census.gov");
        expected.put("cache and reuse memory maps", "false");
        expected.put("url", "file:data/nyc");
        Map<String, String> actual =
                entry.stream()
                        .collect(
                                Collectors.toMap(
                                        ConnectionParameterEntry::getAtKey,
                                        ConnectionParameterEntry::getValue));
        assertEquals(expected, actual);
    }

    public @Test void testFeatureTypeResponse() throws IOException {
        FeatureTypeResponseWrapper rw =
                decode(
                        "FeatureTypeResponse.json",
                        FeatureTypeInfo.class,
                        FeatureTypeResponseWrapper.class);
        FeatureTypeResponse r = rw.getFeatureType();
        assertNotNull(r);
        assertEquals("tasmania_roads", r.getName());
        assertEquals("tasmania_roads", r.getNativeName());
    }

    public @Test void testFeatureTypeResponse2() throws IOException {
        String raw =
                "{\"name\":\"roads\",\"nativeName\":\"roads\",\"namespace\":{\"name\":\"createFullInformation-ws1-463194\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/namespaces\\/createFullInformation-ws1-463194.json\"},\"title\":\"title\",\"abstract\":\"abstract\",\"keywords\":{\"string\":[\"features\",\"roads\"]},\"srs\":\"EPSG:4326\","
                        + "\"nativeBoundingBox\":{\"minx\":589434.85646865,\"maxx\":609527.21021496,\"miny\":4914006.33783702,\"maxy\":4928063.39801461,\"crs\":{\"@class\":\"projected\",\"$\":\"EPSG:26713\"}},\"latLonBoundingBox\":{\"minx\":589434.85646865,\"maxx\":609527.21021496,\"miny\":4914006.33783702,\"maxy\":4928063.39801461,\"crs\":\"EPSG:4326\"},\"projectionPolicy\":\"FORCE_DECLARED\",\"enabled\":true,\"store\":{\"@class\":\"dataStore\",\"name\":\"createFullInformation-ws1-463194:roadsStoreWs1\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/createFullInformation-ws1-463194\\/datastores\\/roadsStoreWs1.json\"},\"serviceConfiguration\":false,\"maxFeatures\":0,\"numDecimals\":0,\"padWithZeros\":false,\"forcedDecimal\":false,\"overridingServiceSRS\":false,\"skipNumberMatched\":false,\"circularArcPresent\":false,\"attributes\":{\"attribute\":[{\"name\":\"the_geom\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"org.locationtech.jts.geom.MultiLineString\"},{\"name\":\"ID\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10},{\"name\":\"CAT_ID\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10},{\"name\":\"CAT_DESC\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10}]}}";
        FeatureTypeResponse r = decode(raw, FeatureTypeResponse.class);
        assertNotNull(r);
        assertNotNull(r.getNativeBoundingBox());
        assertTrue(r.getNativeBoundingBox().getCrs() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> crs = (Map<String, String>) r.getNativeBoundingBox().getCrs();
        assertEquals("projected", crs.get("@class"));
        assertEquals("EPSG:26713", crs.get("$"));
    }

    public @Test void testFeatureTypeResponseWrapper2() throws IOException {
        String raw =
                "{\"featureType\":{\"name\":\"roads\",\"nativeName\":\"roads\",\"namespace\":{\"name\":\"createFullInformation-ws1-463194\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/namespaces\\/createFullInformation-ws1-463194.json\"},\"title\":\"title\",\"abstract\":\"abstract\",\"keywords\":{\"string\":[\"features\",\"roads\"]},\"srs\":\"EPSG:4326\",\"nativeBoundingBox\":{\"minx\":589434.85646865,\"maxx\":609527.21021496,\"miny\":4914006.33783702,\"maxy\":4928063.39801461,\"crs\":{\"@class\":\"projected\",\"$\":\"EPSG:26713\"}},\"latLonBoundingBox\":{\"minx\":589434.85646865,\"maxx\":609527.21021496,\"miny\":4914006.33783702,\"maxy\":4928063.39801461,\"crs\":\"EPSG:4326\"},\"projectionPolicy\":\"FORCE_DECLARED\",\"enabled\":true,\"store\":{\"@class\":\"dataStore\",\"name\":\"createFullInformation-ws1-463194:roadsStoreWs1\",\"href\":\"http:\\/\\/localhost:8080\\/geoserver\\/rest\\/workspaces\\/createFullInformation-ws1-463194\\/datastores\\/roadsStoreWs1.json\"},\"serviceConfiguration\":false,\"maxFeatures\":0,\"numDecimals\":0,\"padWithZeros\":false,\"forcedDecimal\":false,\"overridingServiceSRS\":false,\"skipNumberMatched\":false,\"circularArcPresent\":false,\"attributes\":{\"attribute\":[{\"name\":\"the_geom\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"org.locationtech.jts.geom.MultiLineString\"},{\"name\":\"ID\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10},{\"name\":\"CAT_ID\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10},{\"name\":\"CAT_DESC\",\"minOccurs\":0,\"maxOccurs\":1,\"nillable\":true,\"binding\":\"java.lang.Long\",\"length\":10}]}}}";
        FeatureTypeResponse r = decode(raw, FeatureTypeResponse.class);
        assertNotNull(r);
    }

    public @Test void styleList() throws IOException {
        String raw =
                "{\n"
                        + //
                        "	\"styles\": \n"
                        + //
                        "	{\n"
                        + //
                        "		\"style\": \n"
                        + //
                        "		[\n"
                        + //
                        "			{\n"
                        + //
                        "				\"name\": \"generic\",\n"
                        + //
                        "				\"href\": \"http:\\/\\/localhost:8080\\/rest\\/styles\\/generic.json\"\n"
                        + //
                        "			},\n"
                        + //
                        "\n"
                        + //
                        "			{\n"
                        + //
                        "				\"name\": \"line\",\n"
                        + //
                        "				\"href\": \"http:\\/\\/localhost:8080\\/rest\\/styles\\/line.json\"\n"
                        + //
                        "			}\n"
                        + //
                        "		]\n"
                        + //
                        "	}\n"
                        + //
                        "}";
        StyleListWrapper wrapper = decode(raw, StyleListWrapper.class);
        assertNotNull(wrapper);
        assertEquals(2, wrapper.getStyles().getStyle().size());
    }
}
