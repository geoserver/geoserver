package org.geoserver.csw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Test;
import net.opengis.cat.csw20.ElementSetNameType;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import net.opengis.cat.csw20.ResultType;

import org.geoserver.csw.kvp.GetRecordsKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geotools.csw.CSWConfiguration;
import org.geotools.filter.text.cql2.CQL;

public class GetRecordsTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetRecordsTest());
    }
 
    public void testKVPParameterCQL() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecords");
        raw.put("namespace", "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
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
        GetRecordsKvpRequestReader reader = new GetRecordsKvpRequestReader();
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
        assertEquals(new Integer(10), gr.getDistributedSearch().getHopCount());
        assertEquals("http://www.geoserver.org", gr.getResponseHandler());
        
        // now onto the query
        QueryType query = (QueryType) gr.getQuery();
        assertEquals("AnyText like '%pollution%'", query.getConstraint().getCqlText());
        assertEquals(2, query.getTypeNames().size());
        assertEquals(new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"), query.getTypeNames().get(0));
        assertEquals(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"), query.getTypeNames().get(1));
        assertEquals(2, query.getElementName().size());
        assertEquals(2, query.getElementName().size());
    }
    
    public void testKVPParameterFilter() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecords");
        raw.put("namespace", "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2))");
        raw.put("typenames", "csw:Record");
        raw.put("elementSetName", "brief");
        raw.put("constraintLanguage", "FILTER");
        raw.put("constraint", "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"><ogc:Not><ogc:PropertyIsEqualTo><ogc:PropertyName>prop1</ogc:PropertyName><ogc:Literal>10</ogc:Literal></ogc:PropertyIsEqualTo></ogc:Not></ogc:Filter>");

        GetRecordsKvpRequestReader reader = new GetRecordsKvpRequestReader();
        Object request = reader.createRequest();
        GetRecordsType gr = (GetRecordsType) reader.read(request, parseKvp(raw), raw);
        
        // basic checks
        assertEquals("CSW", gr.getService());
        assertEquals("2.0.2", gr.getVersion());
        
        // now onto the query
        QueryType query = (QueryType) gr.getQuery();
        assertEquals(CQL.toFilter("!(prop1 = 10)"), query.getConstraint().getFilter());
        assertEquals("1.1.0", query.getConstraint().getVersion());
        assertEquals(1, query.getTypeNames().size());
        assertEquals(new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"), query.getTypeNames().get(0));
        assertEquals(ElementSetType.BRIEF, query.getElementSetName().getValue());
    }
    
   
    
    public void testXMLReaderParameter() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetRecords", "2.0.2", new CSWConfiguration());
        GetRecordsType gr = (GetRecordsType)  reader.read(null, getResourceAsReader("GetRecordsBrief.xml"), (Map) null);
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
    

}
