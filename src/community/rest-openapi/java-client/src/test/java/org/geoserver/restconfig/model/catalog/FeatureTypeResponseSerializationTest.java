package org.geoserver.restconfig.model.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.geoserver.openapi.model.catalog.AttributeTypeInfo;
import org.geoserver.openapi.model.catalog.FeatureTypeInfo;
import org.geoserver.openapi.model.catalog.ProjectionPolicy;
import org.geoserver.openapi.v1.model.AttributeTypeInfoResponse;
import org.geoserver.openapi.v1.model.FeatureTypeResponse;
import org.geoserver.openapi.v1.model.FeatureTypeResponseWrapper;
import org.geoserver.restconfig.model.SerializationTest;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringStartsWith;
import org.junit.Test;

public class FeatureTypeResponseSerializationTest extends SerializationTest {

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

        assertEquals("topp", r.getNamespace().getName());
        assertEquals(
                "http://localhost:9090/geoserver/rest/namespaces/topp.json",
                r.getNamespace().getHref());
        assertEquals("Tasmania roads", r.getTitle());
        assertEquals("Main Tasmania roads", r.getAbstract());
        assertEquals("Main Tasmania roads", r.getDescription());
        assertEquals(
                Arrays.asList(
                        "Roads",
                        "Tasmania",
                        "test keyword\\@language=es\\;\\@vocabulary=spanish\\;"),
                r.getKeywords().getString());

        //		ResourceResponseDataLinks dataLinks = r.getDataLinks();
        //		assertNotNull(dataLinks);
        //		dataLinks.getMetadataLink()
        //		ResourceResponseMetadatalinks metadatalinks = r.getMetadatalinks();

        assertThat(r.getNativeCRS(), IsInstanceOf.instanceOf(String.class));
        assertThat((String) r.getNativeCRS(), StringStartsWith.startsWith("GEOGCS[\"WGS 84\""));

        assertEquals("EPSG:4326", r.getSrs());

        super.assertResponseBounds(
                r.getNativeBoundingBox(), 145.19754, 148.27298000000002, -43.423512, -40.852802);
        assertThat(r.getNativeBoundingBox().getCrs(), IsInstanceOf.instanceOf(String.class));
        assertEquals("EPSG:4326", r.getNativeBoundingBox().getCrs());

        super.assertResponseBounds(
                r.getLatLonBoundingBox(), 145.19754, 148.27298000000002, -43.423512, -40.852802);
        assertThat(r.getLatLonBoundingBox().getCrs(), IsInstanceOf.instanceOf(String.class));
        assertEquals("EPSG:4326", r.getLatLonBoundingBox().getCrs());

        assertEquals(ProjectionPolicy.FORCE_DECLARED, r.getProjectionPolicy());
        assertEquals(Boolean.TRUE, r.getEnabled());
        // assertEquals("", r.getMetadata());//TODO

        assertEquals("topp:taz_shapes", r.getStore().getName());
        assertEquals("dataStore", r.getStore().getAtClass());
        assertEquals(
                "http://localhost:9090/geoserver/rest/workspaces/topp/datastores/taz_shapes.json",
                r.getStore().getHref());
        assertEquals(Boolean.TRUE, r.getServiceConfiguration());
        assertEquals(Collections.singletonList("WFS"), r.getDisabledServices().getString());
        assertEquals(1000, r.getMaxFeatures().intValue());
        assertEquals(5, r.getNumDecimals().intValue());
        assertEquals(Boolean.TRUE, r.getPadWithZeros());
        assertEquals(Boolean.TRUE, r.getForcedDecimal());
        assertEquals(Arrays.asList("4326", "3857"), r.getResponseSRS().getString());
        assertEquals(Boolean.TRUE, r.getOverridingServiceSRS());
        assertEquals(Boolean.TRUE, r.getSkipNumberMatched());
        assertEquals(Boolean.TRUE, r.getCircularArcPresent());
        assertEquals(Boolean.TRUE, r.getEncodeMeasures());
        // TODO: add getLinearizationTolerance property

        AttributeTypeInfoResponse attributesResponse = r.getAttributes();
        assertNotNull(attributesResponse);
        assertNotNull(attributesResponse.getAttribute());
        List<AttributeTypeInfo> attributes = attributesResponse.getAttribute();

        assertEquals(2, attributes.size());
        assertAttribue(
                attributes.get(0),
                "the_geom",
                0,
                1,
                true,
                "org.locationtech.jts.geom.MultiLineString",
                null);
        assertAttribue(attributes.get(1), "TYPE", 0, 1, true, "java.lang.String", 7);
    }

    private void assertAttribue(
            AttributeTypeInfo att,
            String name,
            int minOccurs,
            int maxOccurs,
            boolean nillable,
            String bindingClassName,
            @Nullable Integer length) {
        assertEquals(name, att.getName());
        assertEquals(minOccurs, att.getMinOccurs().intValue());
        assertEquals(maxOccurs, att.getMaxOccurs().intValue());
        assertEquals(nillable, att.getNillable().booleanValue());
        assertEquals(bindingClassName, att.getBinding());
        assertEquals(length, att.getLength());
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
}
