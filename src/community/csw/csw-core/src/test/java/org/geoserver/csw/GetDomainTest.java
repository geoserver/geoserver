package org.geoserver.csw;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import net.opengis.cat.csw20.DescribeRecordType;
import net.opengis.cat.csw20.GetDomainType;

import org.geoserver.csw.kvp.GetDomainKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geotools.csw.CSWConfiguration;

public class GetDomainTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetDomainTest());
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
        GetDomainType gd = (GetDomainType)  reader.read(null, getResourceAsReader("GetDomainParameter.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("GetRecords.resultType", gd.getParameterName());
    }
    
    public void testXMLReaderProperty() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetDomain", "2.0.2", new CSWConfiguration());
        GetDomainType gd = (GetDomainType)  reader.read(null, getResourceAsReader("GetDomainProperty.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("dc:title", gd.getPropertyName());
    }


}
