/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import net.opengis.cat.csw20.GetDomainType;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.csw.kvp.GetDomainKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetDomainTest extends CSWSimpleTestSupport {

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

    @Test
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

    @Test
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

    @Test
    public void testXMLReaderParameter() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetDomain",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        GetDomainType gd =
                (GetDomainType)
                        reader.read(
                                null, getResourceAsReader("GetDomainParameter.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("GetRecords.resultType", gd.getParameterName());
    }

    @Test
    public void testXMLReaderProperty() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetDomain",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        GetDomainType gd =
                (GetDomainType)
                        reader.read(null, getResourceAsReader("GetDomainProperty.xml"), (Map) null);
        assertEquals("CSW", gd.getService());
        assertEquals("2.0.2", gd.getVersion());
        assertEquals("dc:title", gd.getPropertyName());
    }

    @Test
    public void testGETReaderParameter() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetDomain&parameterName=GetRecords.resultType");
        // print(dom);
        // checkValidationErrors(dom);

        assertXpathEvaluatesTo(
                "GetRecords.resultType",
                "/csw:GetDomainResponse/csw:DomainValues/csw:ParameterName",
                dom);
        assertXpathEvaluatesTo("3", "count(//csw:Value)", dom);
    }

    @Test
    public void testGETReaderProperty() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetDomain&propertyName=dc:title",
                        "ISO-8859-1");
        print(dom);
        // checkValidationErrors(dom);

        assertXpathEvaluatesTo(
                "dc:title", "/csw:GetDomainResponse/csw:DomainValues/csw:PropertyName", dom);
        assertXpathEvaluatesTo("9", "count(//csw:Value)", dom);
    }
}
