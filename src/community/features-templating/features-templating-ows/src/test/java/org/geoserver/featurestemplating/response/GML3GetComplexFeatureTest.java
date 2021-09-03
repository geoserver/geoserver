package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;
import org.w3c.dom.Document;

public class GML3GetComplexFeatureTest extends TemplateComplexTestSupport {

    private static final String MF_GML3 = "MappedFeatureGML31";
    private static final String MF_GML3_PARAM = "&" + MF_GML3 + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeatureGML31.xml";
        setUpTemplate(
                "requestParam('" + MF_GML3 + "')='true'",
                SupportedFormat.GML,
                templateMappedFeature,
                MF_GML3,
                ".xml",
                "gsml",
                mappedFeature);
    }

    @Test
    public void getMappedFeature() throws IOException {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=gml3"
                                + MF_GML3_PARAM);
        assertXpathCount(5, "//gsml:MappedFeature", doc);
        assertXpathCount(5, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        // filter dynamic value element
        assertXpathCount(1, "//gsml:MappedFeature/gml:name", doc);
        // filtered element
        assertXpathCount(2, "//gsml:GeologicUnit", doc);
        assertXpathCount(4, "//gsml:lithology", doc);
    }

    @Test
    public void getMappedFeatureBackwardsMapping() throws IOException {

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml3"
                                + "&cql_filter=wfs:FeatureCollection.gml:featureMember"
                                + ".gsml:MappedFeature.gsml:specification.gsml:GeologicUnit"
                                + ".gsml:composition.gsml:CompositionPart.gsml:lithology.gsml:ControlledConcept.gsml:name = 'name_cc_3'"
                                + MF_GML3_PARAM);
        assertXpathCount(2, "//gsml:MappedFeature", doc);
    }
}
