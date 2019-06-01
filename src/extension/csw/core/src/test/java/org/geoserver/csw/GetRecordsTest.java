/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.ElementSetNameType;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import net.opengis.cat.csw20.ResultType;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.csw.kvp.GetRecordsKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;
import org.geotools.xml.XmlConverterFactory;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.Document;

public class GetRecordsTest extends CSWSimpleTestSupport {

    @Test
    public void testKVPParameterCQL() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecords");
        raw.put(
                "namespace",
                "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("resultType", "results");
        raw.put("requestId", "myId");
        raw.put("outputFormat", "application/xml");
        raw.put("outputSchema", "http://www.opengis.net/cat/csw/2.0.2");
        raw.put("startPosition", "5");
        raw.put("maxRecords", "20");
        raw.put("typenames", "csw:Record,rim:RegistryPackage");

        raw.put("elementName", "dc:title,dct:abstract");
        raw.put("constraintLanguage", "CQL_TEXT");
        raw.put("constraint", "AnyText like '%pollution%'");
        raw.put("sortby", "title:A,abstract:D");
        raw.put("distributedSearch", "true");
        raw.put("hopCount", "10");
        raw.put("responsehandler", "http://www.geoserver.org");
        GetRecordsKvpRequestReader reader =
                new GetRecordsKvpRequestReader(EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        reader.setApplicationContext(applicationContext);
        Object request = reader.createRequest();
        GetRecordsType gr = (GetRecordsType) reader.read(request, parseKvp(raw), raw);

        // basic checks
        assertEquals("CSW", gr.getService());
        assertEquals("2.0.2", gr.getVersion());
        assertEquals(ResultType.RESULTS, gr.getResultType());
        assertEquals("myId", gr.getRequestId());
        assertEquals("application/xml", gr.getOutputFormat());
        assertEquals("http://www.opengis.net/cat/csw/2.0.2", gr.getOutputSchema());
        assertNotNull(gr.getDistributedSearch());
        assertEquals(Integer.valueOf(10), gr.getDistributedSearch().getHopCount());
        assertEquals("http://www.geoserver.org", gr.getResponseHandler());

        // now onto the query
        QueryType query = (QueryType) gr.getQuery();
        assertEquals("AnyText like '%pollution%'", query.getConstraint().getCqlText());
        assertEquals(2, query.getTypeNames().size());
        assertEquals(
                new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"),
                query.getTypeNames().get(0));
        assertEquals(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                query.getTypeNames().get(1));
        assertEquals(2, query.getElementName().size());
        assertEquals(2, query.getElementName().size());
    }

    @Test
    public void testKVPParameterFilter() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecords");
        raw.put("namespace", "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2)");
        raw.put("typenames", "csw:Record");
        raw.put("elementSetName", "brief");
        raw.put("constraintLanguage", "FILTER");
        raw.put(
                "constraint",
                "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"><ogc:Not><ogc:PropertyIsEqualTo><ogc:PropertyName>dc:title</ogc:PropertyName><ogc:Literal>foo</ogc:Literal></ogc:PropertyIsEqualTo></ogc:Not></ogc:Filter>");

        GetRecordsKvpRequestReader reader =
                new GetRecordsKvpRequestReader(EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        reader.setApplicationContext(applicationContext);
        Object request = reader.createRequest();
        GetRecordsType gr = (GetRecordsType) reader.read(request, parseKvp(raw), raw);

        // basic checks
        assertEquals("CSW", gr.getService());
        assertEquals("2.0.2", gr.getVersion());

        // now onto the query
        QueryType query = (QueryType) gr.getQuery();

        // checking the filter is structured as expected, with the proper namespace support
        Filter filter = query.getConstraint().getFilter();
        assertTrue(filter instanceof Not);
        Filter negated = ((Not) filter).getFilter();
        assertTrue(negated instanceof PropertyIsEqualTo);
        PropertyName pname = (PropertyName) ((PropertyIsEqualTo) negated).getExpression1();
        assertEquals("dc:title", pname.getPropertyName());

        assertEquals("1.1.0", query.getConstraint().getVersion());
        assertEquals(1, query.getTypeNames().size());
        assertEquals(
                new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"),
                query.getTypeNames().get(0));
        assertEquals(ElementSetType.BRIEF, query.getElementSetName().getValue());
    }

    @Test
    public void testXMLReaderParameter() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetRecords",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        GetRecordsType gr =
                (GetRecordsType)
                        reader.read(null, getResourceAsReader("GetRecordsBrief.xml"), (Map) null);
        // check the attributes
        assertEquals("application/xml", gr.getOutputFormat());
        assertEquals("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", gr.getOutputSchema());

        // the query
        QueryType query = (QueryType) gr.getQuery();
        List<QName> expected = new ArrayList<QName>();
        String rimNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0";
        expected.add(new QName(rimNamespace, "Service"));
        expected.add(new QName(rimNamespace, "Classification"));
        expected.add(new QName(rimNamespace, "Association"));
        assertEquals(expected, query.getTypeNames());

        // the element set name
        ElementSetNameType esn = query.getElementSetName();
        expected.clear();
        expected.add(new QName(rimNamespace, "Service"));
        assertEquals(expected, esn.getTypeNames());
        assertEquals(ElementSetType.BRIEF, esn.getValue());
    }

    /*
     * Rigth now we don't support the "validate" mode, we need a way to re-encode the request in XML
     * or to snatch it from the raw POST request
     *
     * @Test
     * public void testValidateRequest() throws Exception { String request =
     * "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=validate";
     * Document d = getAsDOM(request); }
     */

    @Test
    public void testHitRequest() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);
        XpathEngine xpath = XMLUnit.newXpathEngine();

        // check we have a timestamp that is a valid XML date, and it's GMT (we don't
        // test parts of the date since we are bound to fail even the year if the test is run
        // across midnight of
        String timestampPath = "/csw:GetRecordsResponse/csw:SearchStatus/@timestamp";
        String timeStamp = xpath.evaluate(timestampPath, d);
        assertGMLTimestamp(timeStamp);

        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);

        // check we have no results
        assertXpathEvaluatesTo("0", "count(//csw:SearchResults/*)", d);
    }

    private void assertGMLTimestamp(String timeStamp) throws Exception {
        assertNotNull(timeStamp);
        Calendar cal =
                new XmlConverterFactory()
                        .createConverter(String.class, Calendar.class, null)
                        .convert(timeStamp, Calendar.class);
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("GMT"), cal.getTimeZone());
    }

    @Test
    public void testHitMaxOffset() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&startPosition=5&maxRecords=2";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // we have the right kind of document
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("7", "//csw:SearchResults/@nextRecord", d);

        // check we have no results
        assertXpathEvaluatesTo("0", "count(//csw:SearchResults/*)", d);
    }

    @Test
    public void testInvalidStartPosition() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&startPosition=0";
        Document d = getAsDOM(request);
        // print(d);
        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "startPosition");
    }

    @Test
    public void testInvalidOutputSchema() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&outputSchema=http://www.geoserver.org";
        Document d = getAsDOM(request);
        // print(d);
        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "outputSchema");
    }

    @Test
    public void testAllRecordsDefaultElementSet() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results";
        Document d = getAsDOM(request, "ISO-8859-1");
        // print(d);
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);

        // check we 10 summary records (max records defaults to 10)
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/csw:SummaryRecord)", d);
    }

    @Test
    public void testAllRecordsBrief() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief";
        Document d = getAsDOM(request, "ISO-8859-1");
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the expected results
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);

        // check we 10 summary records (max records defaults to 10)
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/csw:BriefRecord)", d);
    }

    @Test
    public void testAllRecordsFull() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=full";
        Document d = getAsDOM(request, "ISO-8859-1");
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);

        // check we 10 summary records (max records defaults to 10)
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/csw:Record)", d);
    }

    @Test
    public void testEmptyResult() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&constraint=dc:title = 'foo'";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());

        // print(d);
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
    }

    @Test
    public void testNoXmlPrefix() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record";

        String response = getAsString(request);
        assertTrue(response.indexOf("xmlns:csw=") >= 0);
        assertTrue(response.indexOf("xmlns:xml=") < 0);
    }

    @Test
    public void testTitleFilter() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=dc:title like '%25ipsum%25'";
        Document d = getAsDOM(request);
        print(d);

        assertIpsumRecords(d);
    }

    @Test
    public void testUnqualifiedTitleFilter() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=title like '%25ipsum%25'";
        Document d = getAsDOM(request);
        // print(d);

        assertIpsumRecords(d);
    }

    private void assertIpsumRecords(Document d) throws Exception {
        checkValidationErrors(d, new CSWConfiguration());

        // basic checks
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);

        // verify we got the records we expected
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:BriefRecord[dc:identifier='urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:BriefRecord[dc:identifier='urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2'])",
                d);
    }

    @Test
    public void testFullTextSearch() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&constraint=AnyText like '%25sed%25'";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // basic checks
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("3", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("3", "count(//csw:SearchResults/*)", d);

        // verify we got the records we expected
        // this one has 'sed' in the abstract
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:BriefRecord[dc:identifier='urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f'])",
                d);
        // this one in the abstract
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:BriefRecord[dc:identifier='urn:uuid:66ae76b7-54ba-489b-a582-0f0633d96493'])",
                d);
        // and this one in the title
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:BriefRecord[dc:identifier='urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63'])",
                d);
    }

    /**
     * This one comes from the CITE tests, like filters are to be applied in a case insensitive
     * fashion
     */
    @Test
    public void testFullTextSearchCaseInsensitive() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=full&constraint=AnyText like '%25lorem%25'";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // basic checks
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("5", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("5", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("5", "count(//csw:SearchResults/*)", d);

        // verify we got the records we expected
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:ab42a8c4-95e8-4630-bf79-33e59241605a'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:88247b56-4cbc-4df9-9860-db3f8042e357'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2'])",
                d);
    }

    @Test
    public void testSortByIdentifier() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&sortBy=dc:identifier:A";
        Document d = getAsDOM(request, "ISO-8859-1");
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // basic checks
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);

        // extract the identifiers and verify they are sorted
        List<String> identifiers = new ArrayList<String>();
        XpathEngine xpath = XMLUnit.newXpathEngine();
        for (int i = 1; i < 11; i++) {
            String id = xpath.evaluate("//csw:SummaryRecord[" + i + "]/dc:identifier", d);
            identifiers.add(id);
        }
        List<String> sorted = new ArrayList<String>(identifiers);
        Collections.sort(sorted);
        assertEquals(sorted, identifiers);
    }

    @Test
    public void testSortByDateSelectElements() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results"
                        + "&elementName=dc:identifier,dc:type,dc:date&sortBy=dc:date:A";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        // basic checks
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("10", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("11", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("10", "count(//csw:SearchResults/*)", d);

        // extract the identifiers and verify they are sorted
        List<String> dates = new ArrayList<String>();
        XpathEngine xpath = XMLUnit.newXpathEngine();
        for (int i = 1; i < 11; i++) {
            String id = xpath.evaluate("//csw:Record[" + i + "]/dc:date", d);
            dates.add(id);
        }
        List<String> sorted = new ArrayList<String>(dates);
        Collections.sort(sorted);
        assertEquals(sorted, dates);
    }

    @Test
    public void testFilterBBox() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results"
                        + "&elementName=dc:identifier,ows:BoundingBox&constraint=BBOX(ows:BoundingBox, 47.0, -4.5, 52.0, 1.0)";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        print(d);

        // basic checks
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/*)", d);

        // verify we got the expected records
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc'])",
                d);
        assertXpathEvaluatesTo(
                "1",
                "count(//csw:Record[dc:identifier='urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63'])",
                d);
    }

    /**
     * From CITE compliance, throw an error if a non spatial property is used in a spatial filter
     */
    @Test
    public void testSpatialFilterNonGeomProperty() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results"
                        + "&elementName=dc:identifier,ows:BoundingBox&constraint=BBOX(dct:spatial, 47.0, -4.5, 52.0, 1.0)";
        Document d = getAsDOM(request);
        // print(d);
        checkOws10Exception(d);
    }

    /** From CITE compliance, throw an error the output format is not supported */
    @Test
    public void testUnsupportedOutputFormat() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&outputFormat=application/xhtml+xml";
        Document d = getAsDOM(request);
        // print(d);
        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "outputFormat");
    }

    @Test
    public void testValidateGet() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=validate";
        Document d = getAsDOM(request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/" + request,
                "/csw:Acknowledgement/csw:EchoedRequest/ows:Get/@xlink:href",
                d);

        String timeStamp = xpath.evaluate("/csw:Acknowledgement/@timeStamp", d);
        assertGMLTimestamp(timeStamp);
    }

    @Test
    public void testValidatePost() throws Exception {
        String request = getResourceAsString("GetRecordsValidate.xml");
        Document d = postAsDOM("csw", request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        String timeStamp = xpath.evaluate("/csw:Acknowledgement/@timeStamp", d);
        assertGMLTimestamp(timeStamp);

        assertXpathEvaluatesTo(
                "*lorem*",
                "/csw:Acknowledgement/csw:EchoedRequest/csw:GetRecords/csw:Query/"
                        + "csw:Constraint/ogc:Filter/ogc:PropertyIsLike/ogc:Literal",
                d);
    }

    @Test
    public void testLikeNoEscape() throws Exception {
        String request = getResourceAsString("GetRecordsAnyTextNoEscape.xml");
        Document d = postAsDOM("csw", request);
        checkValidationErrors(d, new CSWConfiguration());
        // print(d);

        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:SearchResults/csw:Record/dc:subject", d);
    }

    @Test
    public void testStartPositionOverNumberOfRecords() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=brief&startPosition=50&maxRecords=10";
        Document d = getAsDOM(request, "ISO-8859-1");
        checkValidationErrors(d, new CSWConfiguration());

        // check we have the expected results
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);

        // check we have 0 summary records
        assertXpathEvaluatesTo("0", "count(//csw:SearchResults/csw:BriefRecord)", d);
    }
}
