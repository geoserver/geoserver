/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class DescribeRecordTest extends CSWInternalTestSupport {

    @Test
    public void testBasicGetLocalSchemaRecord() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=csw:Record");
        checkValidationErrors(dom);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//csw:SchemaComponent)", dom);
        assertXpathExists(
                "//csw:SchemaComponent/xsd:schema[@targetNamespace='http://www.opengis.net/cat/csw/2.0.2']",
                dom);

        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'BriefRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'SummaryRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'Record'])", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/schemas/csw/2.0.2/rec-dcterms.xsd",
                "//xsd:import[@namespace = 'http://purl.org/dc/terms/']/@schemaLocation",
                dom);
    }

    @Test
    public void testBasicGetLocalSchemaAll() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord");
        checkValidationErrors(dom);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//csw:SchemaComponent)", dom);
        assertXpathExists(
                "//csw:SchemaComponent/xsd:schema[@targetNamespace='http://www.opengis.net/cat/csw/2.0.2']",
                dom);
    }
}
