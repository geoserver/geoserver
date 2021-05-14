package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
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
        assertXpathCount(5, "//gsml:MappedFeature/gsml:geometry/gml:Polygon", doc);
        assertXpathCount(4, "//gsml:MappedFeature/gsml:specification/gsml:GeologicUnit", doc);
        // filter on lithology
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
}
