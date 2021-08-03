package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.w3c.dom.Document;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GML3GetComplexFeatureTest extends TemplateComplexTestSupport {

    @Test
    public void getMappedFeature() throws IOException {
        String requestParam = "GML3GetMappedFeature";
        String condition = "requestParam('" + requestParam + "')='true'";
        setUpTemplate(
                condition,
                SupportedFormat.GML,
                "MappedFeatureGML31.xml",
                requestParam,
                ".xml",
                "gsml",
                mappedFeature);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=gml3"
                                + "&"
                                + requestParam
                                + "=true");
        assertXpathCount(4, "//gsml:MappedFeature", doc);
        assertXpathCount(4, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(4, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        // filter dynamic value element
        assertXpathCount(1, "//gsml:MappedFeature/gml:name", doc);
        // filtered element
        assertXpathCount(2, "//gsml:GeologicUnit", doc);
        assertXpathCount(4, "//gsml:lithology", doc);
    }

    @Test
    public void getMappedFeatureBackwardsMapping() throws IOException {
        String requestParam = "GML3GetMappedFeatureBackwardsMapping";
        String condition = "requestParam('" + requestParam + "')='true'";
        setUpTemplate(
                condition,
                SupportedFormat.GML,
                "MappedFeatureGML31.xml",
                requestParam,
                ".xml",
                "gsml",
                mappedFeature);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml3"
                                + "&cql_filter=wfs:FeatureCollection.gml:featureMember"
                                + ".gsml:MappedFeature.gsml:specification.gsml:GeologicUnit"
                                + ".gsml:composition.gsml:CompositionPart.gsml:lithology.gsml:ControlledConcept.gsml:name = 'name_cc_3'"
                                + "&"
                                + requestParam
                                + "=true");
        assertXpathCount(2, "//gsml:MappedFeature", doc);
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.GML31.getFilename();
    }
}
