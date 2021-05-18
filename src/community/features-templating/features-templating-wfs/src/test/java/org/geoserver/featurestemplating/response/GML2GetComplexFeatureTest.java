package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.w3c.dom.Document;

public class GML2GetComplexFeatureTest extends TemplateComplexTestSupport {

    @Test
    public void getMappedFeature() throws IOException {
        setUpComplex("MappedFeatureGML2.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text%2Fxml%3B%20subtype%3Dgml%2F2.1.2");
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
        setUpComplex("MappedFeatureGML2.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text%2Fxml%3B%20subtype%3Dgml%2F2.1.2&cql_filter=wfs:FeatureCollection.gml:featureMember.gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.xlink:title='a static xlink:title filtered'");
        assertXpathCount(1, "//gsml:MappedFeature", doc);
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.GML2.getFilename();
    }
}
