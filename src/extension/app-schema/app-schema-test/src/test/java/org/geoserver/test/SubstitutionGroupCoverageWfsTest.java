/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * This test ensures that substitution groups in XSD schemas work as expected. Specifically,
 * test:DomainSet is substitutable for gml:domainSet and should appear in the response. This test
 * case is loosely based upon WXXM 2 schemas, but significant portions of the schemas were removed
 * or changed to simplify this test case
 *
 * @author Aaron Braeckel (National Center for Atmospheric Research)
 */
public class SubstitutionGroupCoverageWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new SubstitutionGroupCoverageMockData();
    }

    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&outputFormat=gml32&typeName=test:DiscreteCoverage";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature, typename=test:DiscreteCoverage response:\n" + prettyString(doc));
        validateGet(path);
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//test:DiscreteCoverage", doc);
        String id = "a9274057-604d-427e-87f7-e6c9d846ceb5";
        assertXpathEvaluatesTo(id, "(//test:DiscreteCoverage)[1]/@gml:id", doc);
        assertXpathCount(1, "//test:DiscreteCoverage/test:domainSet", doc);
        assertXpathCount(1, "//test:DiscreteCoverage/test:domainSet/test:DomainObject", doc);
        assertXpathCount(
                1, "//test:DiscreteCoverage/test:domainSet/test:DomainObject/test:elements", doc);
        assertXpathCount(
                2, "//test:DiscreteCoverage/test:domainSet/test:DomainObject/test:elements/*", doc);
        assertXpathEvaluatesTo(
                "-0.6476 81.0527",
                "//test:DiscreteCoverage/test:domainSet/"
                        + "test:DomainObject/test:elements[1]/gml:Point/gml:pos",
                doc);
    }
}
