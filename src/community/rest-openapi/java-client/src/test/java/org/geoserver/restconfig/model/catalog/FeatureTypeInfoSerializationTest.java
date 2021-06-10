package org.geoserver.restconfig.model.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.geoserver.openapi.model.catalog.AttributeTypeInfo;
import org.geoserver.openapi.model.catalog.FeatureTypeInfo;
import org.geoserver.openapi.model.catalog.KeywordInfo;
import org.geoserver.openapi.model.catalog.ProjectionPolicy;
import org.geoserver.restconfig.model.SerializationTest;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringStartsWith;
import org.junit.Test;

public class FeatureTypeInfoSerializationTest extends SerializationTest {

    public @Test void testFeatureTypeResponse() throws IOException {
        FeatureTypeInfo fti =
                decode("FeatureTypeInfo.json", FeatureTypeInfo.class, FeatureTypeInfo.class);
        assertNotNull(fti);
        assertEquals("tasmania_roads", fti.getName());
        assertEquals("tasmania_roads", fti.getNativeName());

        assertEquals("topp", fti.getNamespace().getPrefix());
        assertEquals(
                "http://localhost:9090/geoserver/rest/namespaces/topp.json",
                fti.getNamespace().getUri());
        assertEquals(Boolean.TRUE, fti.getNamespace().getIsolated());

        assertEquals("Tasmania roads", fti.getTitle());
        assertEquals("Main Tasmania roads", fti.getAbstract());
        assertEquals(
                Arrays.asList(
                        new KeywordInfo().value("Roads"), //
                        new KeywordInfo().value("Tasmania"), //
                        new KeywordInfo()
                                .value("test keyword")
                                .language("es")
                                .vocabulary("spanish")), //
                fti.getKeywords());

        //		ResourceResponseDataLinks dataLinks = r.getDataLinks();
        //		assertNotNull(dataLinks);
        //		dataLinks.getMetadataLink()
        //		ResourceResponseMetadatalinks metadatalinks = r.getMetadatalinks();

        assertThat(fti.getNativeCRS(), IsInstanceOf.instanceOf(String.class));
        assertThat((String) fti.getNativeCRS(), StringStartsWith.startsWith("GEOGCS[\"WGS 84\""));

        assertEquals("EPSG:4326", fti.getSrs());

        super.assertResponseBounds(
                fti.getNativeBoundingBox(),
                145.19754,
                148.27298000000002,
                -43.423512,
                -40.852802,
                "EPSG:4326");
        super.assertResponseBounds(
                fti.getLatLonBoundingBox(),
                145.19754,
                148.27298000000002,
                -43.423512,
                -40.852802,
                "EPSG:4326");

        assertEquals(ProjectionPolicy.FORCE_DECLARED, fti.getProjectionPolicy());
        assertEquals(Boolean.TRUE, fti.getEnabled());
        // assertEquals("", r.getMetadata());//TODO

        assertNotNull(fti.getStore());
        assertEquals("taz_shapes", fti.getStore().getName());
        assertEquals("topp", fti.getStore().getWorkspace().getName());

        assertEquals(Boolean.TRUE, fti.getServiceConfiguration());
        assertEquals(Collections.singletonList("WFS"), fti.getDisabledServices());
        assertEquals(1000, fti.getMaxFeatures().intValue());
        assertEquals(5, fti.getNumDecimals().intValue());
        assertEquals(Boolean.TRUE, fti.getPadWithZeros());
        assertEquals(Boolean.TRUE, fti.getForcedDecimal());
        assertEquals(Arrays.asList("4326", "3857"), fti.getResponseSRS());
        assertEquals(Boolean.TRUE, fti.getOverridingServiceSRS());
        assertEquals(Boolean.TRUE, fti.getSkipNumberMatched());
        assertEquals(Boolean.TRUE, fti.getCircularArcPresent());
        assertEquals(Boolean.TRUE, fti.getEncodeMeasures());
        // TODO: add getLinearizationTolerance property

        List<AttributeTypeInfo> attributes = fti.getAttributes();
        assertNotNull(attributes);
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
}
