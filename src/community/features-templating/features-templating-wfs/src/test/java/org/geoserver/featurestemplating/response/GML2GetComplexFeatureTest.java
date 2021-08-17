package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;
import org.w3c.dom.Document;

public class GML2GetComplexFeatureTest extends TemplateComplexTestSupport {

    private static final String MF_GML2 = "MappedFeatureGML2";
    private static final String MF_GML2_PARAM = "&" + MF_GML2 + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeatureGML2.xml";
        setUpTemplate(
                "requestParam('" + MF_GML2 + "')='true'",
                SupportedFormat.GML,
                templateMappedFeature,
                MF_GML2,
                ".xml",
                "gsml",
                mappedFeature);
    }

    @Test
    public void getMappedFeature() throws IOException {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text%2Fxml%3B%20subtype%3Dgml%2F2.1.2"
                                + MF_GML2_PARAM);
        assertXpathCount(5, "//gsml:MappedFeature", doc);
        assertXpathCount(5, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Polygon", doc);
        // filtered static valued attribute
        assertXpathCount(4, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathCount(
                1, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@xlink:title", doc);
        // lithology array has been flattened in the template to directly have the gsml:name value
        assertXpathEvaluatesTo(
                "name_cc_5",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/gsml:GeologicUnit/gsml:lithology",
                doc);
    }

    @Test
    public void testBackwardMappingOnFilterStaticAttribute() throws IOException {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text%2Fxml%3B%20subtype%3Dgml%2F2.1.2&cql_filter=wfs:FeatureCollection.gml:featureMember.gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.xlink:title='a static xlink:title filtered'"
                                + MF_GML2_PARAM);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
    }
}
