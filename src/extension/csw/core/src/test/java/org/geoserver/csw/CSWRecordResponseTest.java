/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.transform.TransformerException;
import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.RequestBaseType;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.response.AbstractRecordsResponse;
import org.geoserver.csw.response.CSWRecordTransformer;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.simple.SimpleCatalogStore;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Files;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.ows.OWS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CSWRecordResponseTest extends CSWSimpleTestSupport {

    class CustomRecordResponse extends AbstractRecordsResponse {

        public CustomRecordResponse(GeoServer gs) {
            super(
                    CSWRecordDescriptor.RECORD_TYPE,
                    CSW.NAMESPACE,
                    new HashSet<String>(
                            Arrays.asList(new String[] {"application/xml", "text/xml"})),
                    gs);
        }

        @Override
        protected void transformResponse(
                OutputStream output,
                CSWRecordsResult result,
                RequestBaseType request,
                CSWInfo csw) {
            CSWRecordTransformer transformer =
                    new CSWRecordTransformer(request, csw.isCanonicalSchemaLocation());
            transformer.setIndentation(2);
            try {
                transformer.transform(result, output);
            } catch (TransformerException e) {
                throw new ServiceException(e);
            }
        }
    }

    SimpleCatalogStore store =
            new SimpleCatalogStore(
                    Files.asResource(new File("./src/test/resources/org/geoserver/csw/records")));

    @Before
    public void setUp() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("dct", DCT.NAMESPACE);
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Test
    public void testEncodeBrief() throws Exception {
        // setup the request
        CSWRecordsResult response = getCSWResponse();
        GetRecordsType request = getCSWRequest();

        // transform it into a GetRecordsResponse
        CSWRecordTransformer tx = new CSWRecordTransformer(request, false);
        tx.setIndentation(2);
        StringWriter sw = new StringWriter();
        tx.transform(response, sw);
        // System.out.println(sw);

        Document dom = XMLUnit.buildControlDocument(sw.toString());

        // checking root elements
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", dom);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/cat/csw/2.0.2 http://localhost:8080/geoserver/csw/schemas/csw/2.0.2/record.xsd",
                "/csw:GetRecordsResponse/@xsi:schemaLocation",
                dom);
        assertXpathEvaluatesTo("2012-07-10T15:00:00Z", "//csw:SearchStatus/@timestamp", dom);

        // checking the search results
        assertXpathEvaluatesTo("100", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("12", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("13", "//csw:SearchResults/@nextRecord", dom);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/cat/csw/2.0.2", "//csw:SearchResults/@recordSchema", dom);
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("12", "count(//csw:BriefRecord)", dom);

        // check one record with the bbox
        assertXpathEvaluatesTo(
                "4",
                "count(//csw:BriefRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/*)",
                dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Service",
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/dc:type",
                dom);
        assertXpathEvaluatesTo(
                CSWRecordDescriptor.DEFAULT_CRS_NAME,
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/ows:BoundingBox/@crs",
                dom);
        assertXpathEvaluatesTo(
                "60.042 13.754",
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/ows:BoundingBox/ows:LowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "68.41 17.92",
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/ows:BoundingBox/ows:UpperCorner",
                dom);

        // check one record without the bbox
        assertXpathEvaluatesTo(
                "3",
                "count(//csw:BriefRecord[dc:identifier = 'urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2']/*)",
                dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Image",
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2']/dc:type",
                dom);
        assertXpathEvaluatesTo(
                "Lorem ipsum dolor sit amet",
                "//csw:BriefRecord[dc:identifier = 'urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2']/dc:title",
                dom);
    }

    @Test
    public void testEncodeSummary() throws Exception {
        // setup the request
        CSWRecordsResult response = getCSWResponse();
        response.setElementSet(ElementSetType.SUMMARY);
        GetRecordsType request = getCSWRequest();

        // transform it into a GetRecordsResponse (this time with the canonical location)
        CSWRecordTransformer tx = new CSWRecordTransformer(request, true);
        tx.setIndentation(2);
        StringWriter sw = new StringWriter();
        tx.transform(response, sw);

        // System.out.println(sw);
        Document dom = XMLUnit.buildControlDocument(sw.toString());

        // checking root elements
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", dom);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/record.xsd",
                "/csw:GetRecordsResponse/@xsi:schemaLocation",
                dom);
        assertXpathEvaluatesTo("2012-07-10T15:00:00Z", "//csw:SearchStatus/@timestamp", dom);

        // check that we got summary records
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("12", "count(//csw:SummaryRecord)", dom);

        // check one summary record
        assertXpathEvaluatesTo(
                "5",
                "count(//csw:SummaryRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/*)",
                dom);
        assertXpathEvaluatesTo(
                "http://purl.org/dc/dcmitype/Service",
                "//csw:SummaryRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/dc:type",
                dom);
        assertXpathEvaluatesTo(
                "Proin sit amet justo. In justo. Aenean adipiscing nulla id tellus.",
                "//csw:SummaryRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/dct:abstract",
                dom);
        assertXpathEvaluatesTo(
                "60.042 13.754",
                "//csw:SummaryRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/ows:BoundingBox/ows:LowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "68.41 17.92",
                "//csw:SummaryRecord[dc:identifier = 'urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd']/ows:BoundingBox/ows:UpperCorner",
                dom);
    }

    @Test
    public void testEncodeFull() throws Exception {
        // setup the request
        CSWRecordsResult response = getCSWResponse();
        response.setElementSet(null);
        GetRecordsType request = getCSWRequest();

        // transform it into a GetRecordsResponse (this time with the canonical location)
        CSWRecordTransformer tx = new CSWRecordTransformer(request, true);
        tx.setIndentation(2);
        StringWriter sw = new StringWriter();
        tx.transform(response, sw);

        // System.out.println(sw);
        Document dom = XMLUnit.buildControlDocument(sw.toString());

        // checking root elements
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", dom);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/record.xsd",
                "/csw:GetRecordsResponse/@xsi:schemaLocation",
                dom);
        assertXpathEvaluatesTo("2012-07-10T15:00:00Z", "//csw:SearchStatus/@timestamp", dom);

        // check that we got summary records
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("12", "count(//csw:Record)", dom);

        // check one full record
        String xpathBase =
                "//csw:Record[dc:identifier = 'urn:uuid:784e2afd-a9fd-44a6-9a92-a3848371c8ec']/";
        assertXpathEvaluatesTo("7", "count(" + xpathBase + "*)", dom);
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Text", xpathBase + "dc:type", dom);
        assertXpathEvaluatesTo("Aliquam fermentum purus quis arcu", xpathBase + "dc:title", dom);
        assertXpathEvaluatesTo("Hydrography--Dictionaries", xpathBase + "dc:subject", dom);
        assertXpathEvaluatesTo("application/pdf", xpathBase + "dc:format", dom);
        assertXpathEvaluatesTo("2006-05-12Z", xpathBase + "dc:date", dom);
        assertXpathEvaluatesTo(
                "Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.",
                xpathBase + "dct:abstract",
                dom);
    }

    @Test
    public void testOutputFormats() throws IOException, SAXException, XpathException {
        GeoServer geoserver = getGeoServer();
        // Collection<? extends ServiceInfo>
        CSWInfo cswInfo = geoserver.getServiceByName("CSW", CSWInfo.class);

        CustomRecordResponse customResponse = new CustomRecordResponse(geoserver);
        assertTrue(customResponse.getOutputFormats().contains("text/xml"));
        GetRecordsType request = getCSWRequest();
        request.setOutputFormat("text/xml");

        CSWRecordsResult result = getCSWResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        customResponse.transformResponse(baos, result, request, cswInfo);
        String responseString = baos.toString();
        Document dom = XMLUnit.buildControlDocument(responseString);

        // checking root elements
        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", dom);
    }

    private GetRecordsType getCSWRequest() {
        GetRecordsType request = Csw20Factory.eINSTANCE.createGetRecordsType();
        request.setBaseUrl("http://localhost:8080/geoserver/csw");
        return request;
    }

    private CSWRecordsResult getCSWResponse() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2012);
        cal.set(Calendar.MONTH, 6);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        cal.set(Calendar.HOUR_OF_DAY, 15);
        FeatureCollection fc = store.getRecords(Query.ALL, Transaction.AUTO_COMMIT);
        CSWRecordsResult response =
                new CSWRecordsResult(
                        ElementSetType.BRIEF,
                        "http://www.opengis.net/cat/csw/2.0.2",
                        100,
                        12,
                        13,
                        cal.getTime(),
                        fc);
        return response;
    }
}
