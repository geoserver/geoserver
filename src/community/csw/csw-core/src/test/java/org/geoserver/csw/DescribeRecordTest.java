package org.geoserver.csw;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Test;
import net.opengis.cat.csw20.DescribeRecordType;
import net.opengis.ows10.GetCapabilitiesType;

import org.geoserver.csw.kvp.DescribeRecordKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSWConfiguration;
import org.xml.sax.SAXParseException;

public class DescribeRecordTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeRecordTest());
    }
 
    public void testKVPReaderNS() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("namespace", "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);
        
        assertDescribeRecordValid(dr);
    }

    private void assertDescribeRecordValid(DescribeRecordType dr) {
        assertEquals("CSW", dr.getService());
        assertEquals("2.0.2", dr.getVersion());
        assertEquals(2, dr.getTypeName().size());
        assertEquals(new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"), dr.getTypeName().get(0));
        assertEquals(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"), dr.getTypeName().get(1));
    }
    
    public void testKVPReaderNoNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);
        
        assertDescribeRecordValid(dr);
    }
    
    public void testKVPReaderDefaultNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("namespace", "xmlns(=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);
        
        assertDescribeRecordValid(dr);
    }
    
    public void testXMLReader() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("DescribeRecord", "2.0.2", new CSWConfiguration());
        DescribeRecordType dr = (DescribeRecordType)  reader.read(null, getResourceAsReader("DescribeRecord.xml"), (Map) null);
        assertDescribeRecordValid(dr);
    }

}
