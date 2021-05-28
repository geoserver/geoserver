package org.geoserver.featurestemplating.response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class GMLComplexFeatureResponseAPITest extends TemplateComplexTestSupport {

    @Test
    public void getMappedFeature() throws IOException {
        setUpComplex("MappedFeatureGML32.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "ogc/features/collections/gsml:MappedFeature"
                                + "/items?f=application%2Fgml%2Bxml%3Bversion%3D3.2");
        assertXpathCount(5, "//gsml:MappedFeature", doc);
        assertXpathCount(5, "//gsml:samplingFrame//@xlink:href", doc);
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Surface", doc);
        assertXpathCount(4, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);

        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description/@xlink:href",
                doc);
        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent",
                doc);
        assertXpathCount(
                4,
                "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gsml:staticContent/@xlink:title",
                doc);

        // filter on array element lithology
        assertXpathCount(2, "//gsml:lithology", doc);
    }

    @Test
    public void getMappedFeatureBackwardsMappingToExpression() throws IOException {
        setUpComplex("MappedFeatureGML32.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "ogc/features/collections/gsml:MappedFeature/items?filter-lang=cql-text&f=application%2Fgml%2Bxml%3Bversion%3D3.2"
                                + "&filter=wfs:FeatureCollection.wfs:member"
                                + ".gsml:MappedFeature.gml:name='mf.GUNTHORPE FORMATION'");
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf1", "//gsml:MappedFeature/@gml:id", doc);
    }

    @Test
    public void getMappedFeatureBackwardsMappingPointingToExpression2() throws IOException {
        setUpComplex("MappedFeatureGML32.xml", mappedFeature);
        Document doc =
                getAsDOM(
                        "ogc/features/collections/gsml:MappedFeature/items?filter-lang=cql-text&f=application%2Fgml%2Bxml%3Bversion%3D3.2"
                                + "&filter=wfs:FeatureCollection.wfs:member.gsml:MappedFeature.gsml:specification.gsml:GeologicUnit"
                                + ".gsml:composition.gsml:CompositionPart.gsml:role='interbedded component'");
        assertXpathCount(3, "//gsml:MappedFeature", doc);
        assertXpathCount(0, "gsml:MappedFeature[@gml:id=mf1 or @gml:id=mf5]", doc);
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.GML32.getFilename();
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData() {
            @Override
            public Map<String, String> getNamespaces() {
                Map<String, String> namespaces = new HashMap<>();
                namespaces.put("gml", "http://www.opengis.net/gml/3.2");
                namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
                namespaces.put("gsml", "urn:cgi:xmlns:CGI:GeoSciML:2.0");
                namespaces.put("xlink", "http://www.w3.org/1999/xlink");
                return namespaces;
            }
        };
    }
}
