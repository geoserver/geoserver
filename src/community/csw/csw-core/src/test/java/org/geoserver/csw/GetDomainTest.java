package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import net.opengis.cat.csw20.GetDomainType;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.csw.kvp.GetDomainKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geotools.csw.CSWConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.w3c.dom.Document;

public class GetDomainTest extends CSWTestSupport {

    static XpathEngine xpath = XMLUnit.newXpathEngine();

    static {
        Map<String, String> prefixMap = new HashMap<String, String>();
        prefixMap.put("ows", OWS.NAMESPACE);
        prefixMap.put("ogc", OGC.NAMESPACE);
        prefixMap.put("gml", "http://www.opengis.net/gml");
        prefixMap.put("gmd", "http://www.isotc211.org/2005/gmd");
        prefixMap.put("xlink", XLINK.NAMESPACE);
        NamespaceContext nameSpaceContext = new SimpleNamespaceContext(prefixMap);
        xpath.setNamespaceContext(nameSpaceContext);
    }

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetDomainTest());
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

    public void testKVPParameter() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetDomain");
        raw.put("parameterName", "GetRecords.resultType");

        GetDomainKvpRequestReader reader = new GetDomainKvpRequestReader();
        Object request = reader.createRequest();
        GetDomainType gd = (GetDomainType) reader.read(request, parseKvp(raw), raw);

        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("GetRecords.resultType", gd.getParameterName());
    }

    public void testKVPProperty() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetDomain");
        raw.put("propertyName", "dc:title");

        GetDomainKvpRequestReader reader = new GetDomainKvpRequestReader();
        Object request = reader.createRequest();
        GetDomainType gd = (GetDomainType) reader.read(request, parseKvp(raw), raw);

        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("dc:title", gd.getPropertyName());
    }

    public void testXMLReaderParameter() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetDomain", "2.0.2", new CSWConfiguration());
        GetDomainType gd = (GetDomainType) reader.read(null,
                getResourceAsReader("GetDomainParameter.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("GetRecords.resultType", gd.getParameterName());
    }

    public void testXMLReaderProperty() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetDomain", "2.0.2", new CSWConfiguration());
        GetDomainType gd = (GetDomainType) reader.read(null,
                getResourceAsReader("GetDomainProperty.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("dc:title", gd.getPropertyName());
    }

    public void testGETReaderParameter() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?service=csw&version=2.0.2&request=GetDomain&parameterName=GetRecords.resultType");
        // print(dom);
        //checkValidationErrors(dom);
        
        assertXpathEvaluatesTo("GetRecords.resultType", "/csw:GetDomainResponse/csw:DomainValues/csw:ParameterName", dom);
        assertXpathEvaluatesTo("3", "count(//csw:Value)", dom);
    }

    public void testGETReaderProperty() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?service=csw&version=2.0.2&request=GetDomain&propertyName=dc:title");
        // print(dom);
        //checkValidationErrors(dom);
        
        assertXpathEvaluatesTo("dc:title", "/csw:GetDomainResponse/csw:DomainValues/csw:PropertyName", dom);
        assertXpathEvaluatesTo("9", "count(//csw:Value)", dom);
    }

}
