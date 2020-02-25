/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class GetRecordByIdTest extends CSWInternalTestSupport {

    @Test
    public void test() throws Exception {
        String forestId = getCatalog().getLayerByName("Forests").getResource().getId();

        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=csw:Record&id="
                        + forestId;
        Document d = getAsDOM(request);
        print(d);
        checkValidationErrors(d);

        // check we have the expected results
        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordByIdResponse)", d);
        // check contents Forests record
        assertXpathEvaluatesTo(
                "abstract about Forests",
                "//csw:SummaryRecord[dc:title='Forests']/dct:abstract",
                d);
        assertXpathEvaluatesTo("Forests", "//csw:SummaryRecord[dc:title='Forests']/dc:subject", d);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Dataset",
                "//csw:SummaryRecord[dc:title='Forests']/dc:type",
                d);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:6.11:4326",
                "//csw:SummaryRecord[dc:title='Forests']/ows:BoundingBox/@crs",
                d);
        assertXpathEvaluatesTo(
                "-90.0 -180.0",
                "//csw:SummaryRecord[dc:title='Forests']/ows:BoundingBox/ows:LowerCorner",
                d);
        assertXpathEvaluatesTo(
                "90.0 180.0",
                "//csw:SummaryRecord[dc:title='Forests']/ows:BoundingBox/ows:UpperCorner",
                d);
        // scheme attribute
        assertXpathEvaluatesTo(
                "http://www.digest.org/2.1",
                "//csw:SummaryRecord[dc:title='Forests']/dc:subject/@scheme",
                d);
    }
}
