package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;

import org.apache.commons.io.FileUtils;
import org.geoserver.csw.kvp.GetRecordByIdKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.data.test.MockData;
import org.geotools.csw.CSWConfiguration;
import org.w3c.dom.Document;


/**
 * 
 * @author Niels Charlier
 *
 */
public class GetRecordByIdTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetRecordByIdTest());
    }
 
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

    public void testXMLReader() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetRecordById", "2.0.2", new CSWConfiguration());
        GetRecordByIdType dr = (GetRecordByIdType)  reader.read(null, getResourceAsReader("GetRecordById.xml"), (Map) null);
        assertGetRecordByIdValid(dr);
    }
    
    public void testGetSingle() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        print(dom);
        checkValidationErrors(dom);
        
        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", dom);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/csw:SummaryRecord/dc:identifier)", dom);     
        assertXpathEvaluatesTo("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f", "//csw:SearchResults/csw:SummaryRecord/dc:identifier", dom);     
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:SearchResults/csw:SummaryRecord/dc:title", dom);    
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Image", "//csw:SearchResults/csw:SummaryRecord/dc:type", dom);    
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:SearchResults/csw:SummaryRecord/dc:subject", dom);
        assertXpathEvaluatesTo("image/svg+xml", "//csw:SearchResults/csw:SummaryRecord/dc:format", dom);
        assertXpathEvaluatesTo("Quisque lacus diam, placerat mollis, pharetra in, commodo sed, augue. Duis iaculis arcu vel arcu.", "//csw:SearchResults/csw:SummaryRecord/dct:abstract", dom);
        assertXpathEvaluatesTo("GR-22", "//csw:SearchResults/csw:SummaryRecord/dct:spatial", dom);
    }
    
    public void testGetMultiple() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f,urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        print(dom);
        checkValidationErrors(dom);
        
        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("2", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", dom);
        assertXpathEvaluatesTo("2", "count(//csw:SearchResults/csw:SummaryRecord/dc:identifier)", dom);     
        assertXpathEvaluatesTo("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f", "//csw:SearchResults/csw:SummaryRecord[1]/dc:identifier", dom);     
        assertXpathEvaluatesTo("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd", "//csw:SearchResults/csw:SummaryRecord[2]/dc:identifier", dom);   
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Service", "//csw:SearchResults/csw:SummaryRecord[2]/dc:type", dom);    
        assertXpathEvaluatesTo("Proin sit amet justo. In justo. Aenean adipiscing nulla id tellus.", "//csw:SearchResults/csw:SummaryRecord[2]/dct:abstract", dom);       
    }
    
    public void testGetNothing() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=summary&id=REC-1,REC-2");
        print(dom);
        checkValidationErrors(dom);
        
        // check we have the expected results
        assertXpathEvaluatesTo("summary", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("0", "count(//csw:SearchResults/csw:SummaryRecord/dc:identifier)", dom);       
    }
    
    public void testGetFull() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=full&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        print(dom);
        checkValidationErrors(dom);
        
        // check we have the expected results
        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", dom);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/csw:Record/dc:identifier)", dom);     
        assertXpathEvaluatesTo("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f", "//csw:SearchResults/csw:Record/dc:identifier", dom);     
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:SearchResults/csw:Record/dc:title", dom);    
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Image", "//csw:SearchResults/csw:Record/dc:type", dom);    
        assertXpathEvaluatesTo("Tourism--Greece", "//csw:SearchResults/csw:Record/dc:subject", dom);
        assertXpathEvaluatesTo("image/svg+xml", "//csw:SearchResults/csw:Record/dc:format", dom);
        assertXpathEvaluatesTo("Quisque lacus diam, placerat mollis, pharetra in, commodo sed, augue. Duis iaculis arcu vel arcu.", "//csw:SearchResults/csw:Record/dct:abstract", dom);
        assertXpathEvaluatesTo("GR-22", "//csw:SearchResults/csw:Record/dct:spatial", dom);
    }
    
    public void testGetBrief() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRecordById&elementsetname=brief&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        print(dom);
        checkValidationErrors(dom);
        
        // check we have the expected results
        assertXpathEvaluatesTo("brief", "//csw:SearchResults/@elementSet", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsMatched", dom);
        assertXpathEvaluatesTo("1", "//csw:SearchResults/@numberOfRecordsReturned", dom);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", dom);
        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/csw:BriefRecord/dc:identifier)", dom);     
        assertXpathEvaluatesTo("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f", "//csw:SearchResults/csw:BriefRecord/dc:identifier", dom);     
        assertXpathEvaluatesTo("Lorem ipsum", "//csw:SearchResults/csw:BriefRecord/dc:title", dom);    
        assertXpathEvaluatesTo("http://purl.org/dc/dcmitype/Image", "//csw:SearchResults/csw:BriefRecord/dc:type", dom);    
        assertXpathEvaluatesTo("", "//csw:SearchResults/csw:BriefRecord/dc:subject", dom);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        // copy all records into the data directory
        File root = dataDirectory.getDataDirectoryRoot();
        File catalog = new File(root, "catalog");
        File records = new File("./src/test/resources/org/geoserver/csw/records");
        FileUtils.copyDirectory(records, catalog);
    }

}
