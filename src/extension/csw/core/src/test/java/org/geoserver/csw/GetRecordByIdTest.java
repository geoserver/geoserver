/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import org.geoserver.csw.kvp.GetRecordByIdKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class GetRecordByIdTest extends CSWSimpleTestSupport {

    @Test
    public void testKVPReader() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecordById");
        raw.put("elementsetname", "summary");
        raw.put("id", "REC-10,REC-11,REC-12");
        raw.put("outputFormat", "application/xml");
        raw.put("outputSchema", "http://www.opengis.net/cat/csw/2.0.2");

        GetRecordByIdKvpRequestReader reader = new GetRecordByIdKvpRequestReader();
        Object request = reader.createRequest();
        GetRecordByIdType dr = (GetRecordByIdType) reader.read(request, parseKvp(raw), raw);

        assertGetRecordByIdValid(dr);
    }

    private void assertGetRecordByIdValid(GetRecordByIdType dr) {
        assertEquals("CSW", dr.getService());
        assertEquals("2.0.2", dr.getVersion());
        assertEquals(ElementSetType.SUMMARY, dr.getElementSetName().getValue());
        assertEquals("REC-10", dr.getId().get(0).toString());
        assertEquals("REC-11", dr.getId().get(1).toString());
        assertEquals("REC-12", dr.getId().get(2).toString());
    }

    @Test
    public void testXMLReader() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetRecordById",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        GetRecordByIdType dr =
                (GetRecordByIdType)
                        reader.read(null, getResourceAsReader("GetRecordById.xml"), (Map) null);
        assertGetRecordByIdValid(dr);
    }

    @Test
    public void testEntityExpansion() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetRecordById",
                        "2.0.2",
                        new CSWConfiguration(),
                        GeoServerExtensions.bean(EntityResolverProvider.class));
        try {
            GetRecordByIdType dr =
                    (GetRecordByIdType)
                            reader.read(
                                    null,
                                    getResourceAsReader("GetRecordByIdEntityExpansion.xml"),
                                    (Map) null);
            fail("Should have failed with an entity expansion disallowed exception");
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            assertTrue(cause.getMessage().contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
        }
    }

    @Test
    public void testGetMissingId() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById");
        checkOws10Exception(dom, ServiceException.MISSING_PARAMETER_VALUE, "id");
    }

    @Test
    public void testGetSingle() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        // print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo("1", "count(//csw:SummaryRecord/dc:identifier)", dom);
        assertXpathEvaluatesTo(
                "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f",
                "//csw:SummaryRecord/dc:identifier",
                dom);
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:SummaryRecord/dc:title", dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Image", "//csw:SummaryRecord/dc:type", dom);
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:SummaryRecord/dc:subject", dom);
        assertXpathEvaluatesTo("image/svg+xml", "//csw:SummaryRecord/dc:format", dom);
        assertXpathEvaluatesTo(
                "Quisque lacus diam, placerat mollis, pharetra in, commodo sed, augue. Duis iaculis arcu vel arcu.",
                "//csw:SummaryRecord/dct:abstract",
                dom);
        assertXpathEvaluatesTo("GR-22", "//csw:SummaryRecord/dct:spatial", dom);
    }

    @Test
    public void testGetMultiple() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f,urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        // print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo("2", "count(//csw:SummaryRecord/dc:identifier)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:SummaryRecord[dc:identifier='urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:SummaryRecord[dc:identifier='urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd'])",
                dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Service",
                "//csw:SummaryRecord[dc:identifier='urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/dc:type",
                dom);
        assertXpathEvaluatesTo(
                "Proin sit amet justo. In justo. Aenean adipiscing nulla id tellus.",
                "//csw:SummaryRecord[dc:identifier='urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/dct:abstract",
                dom);
    }

    @Test
    public void testGetNothing() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=REC-1,REC-2");
        // print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo("0", "count(//csw:SummaryRecord)", dom);
    }

    @Test
    public void testGetFull() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=full&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        // print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo(
                "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f", "//csw:Record/dc:identifier", dom);
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:Record/dc:title", dom);
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Image", "//csw:Record/dc:type", dom);
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:Record/dc:subject", dom);
        assertXpathEvaluatesTo("image/svg+xml", "//csw:Record/dc:format", dom);
        assertXpathEvaluatesTo(
                "Quisque lacus diam, placerat mollis, pharetra in, commodo sed, augue. Duis iaculis arcu vel arcu.",
                "//csw:Record/dct:abstract",
                dom);
        assertXpathEvaluatesTo("GR-22", "//csw:Record/dct:spatial", dom);
    }

    @Test
    public void testGetBrief() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=brief&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        // print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo("1", "count(//csw:BriefRecord/dc:identifier)", dom);
        assertXpathEvaluatesTo(
                "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f",
                "//csw:BriefRecord/dc:identifier",
                dom);
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:BriefRecord/dc:title", dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Image", "//csw:BriefRecord/dc:type", dom);
        assertXpathEvaluatesTo("", "//csw:BriefRecord/dc:subject", dom);
    }

    @Test
    public void testPostSummary() throws Exception {
        String request = getResourceAsString("GetRecordById.xml");
        request = request.replace("REC-10", "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        Document dom = postAsDOM(BASEPATH, request);
        print(dom);
        checkValidationErrors(dom);

        // check we have the expected results
        assertXpathEvaluatesTo(
                "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f",
                "//csw:SummaryRecord/dc:identifier",
                dom);
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:SummaryRecord/dc:title", dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Image", "//csw:SummaryRecord/dc:type", dom);
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:SummaryRecord/dc:subject", dom);
        assertXpathEvaluatesTo("image/svg+xml", "//csw:SummaryRecord/dc:format", dom);
        assertXpathEvaluatesTo(
                "Quisque lacus diam, placerat mollis, pharetra in, commodo sed, augue. Duis iaculis arcu vel arcu.",
                "//csw:SummaryRecord/dct:abstract",
                dom);
        assertXpathEvaluatesTo("GR-22", "//csw:SummaryRecord/dct:spatial", dom);
    }
}
