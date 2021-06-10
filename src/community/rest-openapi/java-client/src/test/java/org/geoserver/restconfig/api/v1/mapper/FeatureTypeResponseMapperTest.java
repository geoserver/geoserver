package org.geoserver.restconfig.api.v1.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.geoserver.openapi.model.catalog.FeatureTypeInfo;
import org.geoserver.openapi.model.catalog.MetadataLinkInfo;
import org.geoserver.openapi.v1.model.FeatureTypeResponse;
import org.geoserver.openapi.v1.model.FeatureTypeResponseWrapper;
import org.geoserver.openapi.v1.model.MetadataLinks;
import org.geoserver.restconfig.model.SerializationTest;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

public class FeatureTypeResponseMapperTest extends SerializationTest {

    private FeatureTypeResponseMapper mapper;

    public @Before void before() {
        mapper = Mappers.getMapper(FeatureTypeResponseMapper.class);
        assertNotNull(mapper);
    }

    public @Test void testMetadataLinks() throws IOException {
        final String response =
                "{\n"
                        + "            \"metadataLink\": [\n"
                        + "                {\n"
                        + "                    \"about\": \"Sample Metadata Title\",\n"
                        + "                    \"type\": \"text\\/plain\",\n"
                        + "                    \"metadataType\": \"ISO19115:2003\",\n"
                        + "                    \"content\": \"http:\\/\\/geoserver.org\"\n"
                        + "                }\n"
                        + "            ]\n"
                        + "        },\n";
        MetadataLinks decodedResponse = super.decode(response, MetadataLinks.class);
        assertNotNull(decodedResponse);
        List<MetadataLinkInfo> list = decodedResponse.getMetadataLink();
        assertNotNull(list);
        assertEquals(1, list.size());
        MetadataLinkInfo link = list.get(0);
        assertEquals("Sample Metadata Title", link.getAbout());
        assertEquals("text/plain", link.getType());
        assertEquals("ISO19115:2003", link.getMetadataType());
        assertEquals("http://geoserver.org", link.getContent());
    }

    public @Test void testMapFeatureTypeResponseToFeatureTypeInfo() throws IOException {

        final FeatureTypeInfo expected =
                super.decode("FeatureTypeInfo.json", FeatureTypeInfo.class, FeatureTypeInfo.class);

        final FeatureTypeResponseWrapper responseWrapper =
                super.decode(
                        "FeatureTypeResponse.json",
                        FeatureTypeInfo.class,
                        FeatureTypeResponseWrapper.class);
        final FeatureTypeResponse response = responseWrapper.getFeatureType();
        FeatureTypeInfo mapped = mapper.map(response);

        assertEquals(expected.getAbstract(), mapped.getAbstract());
        assertEquals(expected.getAdvertised(), mapped.getAdvertised());
        assertEquals(expected.getAlias(), mapped.getAlias());
        assertEquals(expected.getAttributes(), mapped.getAttributes());
        assertEquals(expected.getCircularArcPresent(), mapped.getCircularArcPresent());
        assertEquals(expected.getCqlFilter(), mapped.getCqlFilter());

        // TODO
        // assertEquals(expected.getDataLinks(), mapped.getDataLinks());

        assertEquals(expected.getDescription(), mapped.getDescription());
        assertEquals(expected.getDisabledServices(), mapped.getDisabledServices());
        assertEquals(expected.getEnabled(), mapped.getEnabled());
        assertEquals(expected.getEncodeMeasures(), mapped.getEncodeMeasures());
        assertEquals(expected.getForcedDecimal(), mapped.getForcedDecimal());
        assertEquals(expected.getKeywords(), mapped.getKeywords());
        assertEquals(expected.getLatLonBoundingBox(), mapped.getLatLonBoundingBox());
        assertEquals(expected.getLinearizationTolerance(), mapped.getLinearizationTolerance());
        assertEquals(expected.getMaxFeatures(), mapped.getMaxFeatures());
        assertEquals(expected.getName(), mapped.getName());

        // ignore the isolated property, doesn't come in the response.
        assertEquals(expected.getNamespace().getPrefix(), mapped.getNamespace().getPrefix());
        assertEquals(expected.getNamespace().getUri(), mapped.getNamespace().getUri());

        assertEquals(expected.getNativeBoundingBox(), mapped.getNativeBoundingBox());
        assertEquals(expected.getNativeCRS(), mapped.getNativeCRS());
        assertEquals(expected.getNativeName(), mapped.getNativeName());
        assertEquals(expected.getNumDecimals(), mapped.getNumDecimals());
        assertEquals(expected.getOverridingServiceSRS(), mapped.getOverridingServiceSRS());
        assertEquals(expected.getPadWithZeros(), mapped.getPadWithZeros());
        assertEquals(expected.getProjectionPolicy(), mapped.getProjectionPolicy());
        assertEquals(expected.getResponseSRS(), mapped.getResponseSRS());
        assertEquals(expected.getServiceConfiguration(), mapped.getServiceConfiguration());
        assertEquals(expected.getSkipNumberMatched(), mapped.getSkipNumberMatched());
        assertEquals(expected.getSrs(), mapped.getSrs());
        assertEquals(expected.getStore(), mapped.getStore());
        assertEquals(expected.getTitle(), mapped.getTitle());

        assertNotNull(expected.getMetadataLinks());
        assertEquals(1, expected.getMetadataLinks().getMetadataLink().size());

        assertEquals(expected.getMetadataLinks(), mapped.getMetadataLinks());
    }
}
