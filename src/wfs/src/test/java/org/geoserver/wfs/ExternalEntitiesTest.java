/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import junit.framework.Assert;

import org.geoserver.config.GeoServerInfo;
import org.junit.Test;


public class ExternalEntitiesTest extends WFSTestSupport {

    private static final String WFS_1_0_0_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
    		"<!DOCTYPE wfs:GetFeature [\r\n" + 
    		"<!ENTITY c SYSTEM \"file:///this/file/does/not/exist\">\r\n" + 
    		"]>\r\n" + 
    		"<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" \r\n" + 
    		"  outputFormat=\"GML2\"\r\n" + 
    		"  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
    		"  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
    		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n" + 
    		"  xmlns:gml=\"http://www.opengis.net/gml\"\r\n" + 
    		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
    		"  xsi:schemaLocation=\"http://www.opengis.net/wfs\r\n" + 
    		"                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">\r\n" + 
    		"  <wfs:Query typeName=\"topp:states\" handle=\"test\">\r\n" + 
    		"        <ogc:Literal>&c;</ogc:Literal>\r\n" + 
    		"    <ogc:Filter>\r\n" + 
    		"      <ogc:BBOX>\r\n" + 
    		"        <ogc:PropertyName>the_geom</ogc:PropertyName>\r\n" + 
    		"        <gml:Box srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\r\n" + 
    		"           <gml:coordinates>-75.102613,40.212597 -72.361859,41.512517</gml:coordinates>\r\n" + 
    		"        </gml:Box>\r\n" + 
    		"      </ogc:BBOX>\r\n" + 
    		"   </ogc:Filter>\r\n" + 
    		"  </wfs:Query>\r\n" + 
    		"</wfs:GetFeature>";
    
    private static final String WFS_1_1_0_REQUEST = "<!DOCTYPE wfs:GetFeature [\r\n" + 
    		"<!ELEMENT wfs:GetFeature (wfs:Query*)>\r\n" + 
    		"<!ATTLIST wfs:GetFeature\r\n" + 
    		"                service CDATA #FIXED \"WFS\"\r\n" + 
    		"                version CDATA #FIXED \"1.1.0\"\r\n" + 
    		"        xmlns:wfs CDATA #FIXED \"http://www.opengis.net/wfs\"\r\n" + 
    		"                xmlns:ogc CDATA #FIXED \"http://www.opengis.net/ogc\">\r\n" + 
    		"<!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>\r\n" + 
    		"<!ATTLIST wfs:Query typeName CDATA #FIXED \"topp:states\">\r\n" + 
    		"<!ELEMENT wfs:PropertyName (#PCDATA) >\r\n" + 
    		"<!ELEMENT ogc:Filter (ogc:FeatureId*)>\r\n" + 
    		"<!ELEMENT ogc:FeatureId EMPTY>\r\n" + 
    		"<!ATTLIST ogc:FeatureId fid CDATA #FIXED \"states.3\">\r\n" + 
    		"\r\n" + 
    		"<!ENTITY passwd  SYSTEM \"file:///this/file/does/not/exist\">]>\r\n" + 
    		"<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" \r\n" + 
    		"  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
    		"  xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n" + 
    		"  <wfs:Query typeName=\"topp:states\">\r\n" + 
    		"    <wfs:PropertyName>&passwd;</wfs:PropertyName>\r\n" + 
    		"        <ogc:Filter>\r\n" + 
    		"       <ogc:FeatureId fid=\"states.3\"/>\r\n" + 
    		"    </ogc:Filter>\r\n" + 
    		"  </wfs:Query>\r\n" + 
    		"</wfs:GetFeature>";
    
    private static final String WFS_2_0_0_REQUEST = "<?xml version=\"1.0\" ?>\r\n" + 
    		"<!DOCTYPE wfs:GetFeature [\r\n" + 
    		"<!ELEMENT wfs:GetFeature (wfs:Query*)>\r\n" + 
    		"<!ATTLIST wfs:GetFeature\r\n" + 
    		"                service   CDATA #FIXED \"WFS\"\r\n" + 
    		"                version   CDATA #FIXED \"2.0.0\"\r\n" + 
    		"                outputFormat CDATA #FIXED \"application/gml+xml; version=3.2\"\r\n" + 
    		"        xmlns:wfs CDATA #FIXED \"http://www.opengis.net/wfs\"\r\n" + 
    		"                xmlns:ogc CDATA #FIXED \"http://www.opengis.net/ogc\"\r\n" + 
    		"                xmlns:fes CDATA #FIXED \"http://www.opengis.net/fes/2.0\">\r\n" + 
    		"<!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>\r\n" + 
    		"<!ATTLIST wfs:Query typeName CDATA #FIXED \"topp:states\">\r\n" + 
    		"<!ELEMENT wfs:PropertyName (#PCDATA) >\r\n" + 
    		"<!ELEMENT ogc:Filter (fes:ResourceId*)>\r\n" + 
    		"<!ELEMENT fes:ResourceId EMPTY>\r\n" + 
    		"<!ATTLIST fes:ResourceId rid CDATA #FIXED \"states.3\">\r\n" + 
    		"\r\n" + 
    		"<!ENTITY passwd  SYSTEM \"file:///thisfiledoesnotexist\">\r\n" + 
    		"]>\r\n" + 
    		"<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" outputFormat=\"application/gml+xml; version=3.2\"\r\n" + 
    		"        xmlns:wfs=\"http://www.opengis.net/wfs/2.0\"\r\n" + 
    		"        xmlns:fes=\"http://www.opengis.net/fes/2.0\">\r\n" + 
    		"        <wfs:Query typeName=\"topp:states\">\r\n" + 
    		"                <wfs:PropertyName>&passwd;</wfs:PropertyName>\r\n" + 
    		"                <fes:Filter>\r\n" + 
    		"                        <fes:ResourceId rid=\"states.3\"/>\r\n" + 
    		"                </fes:Filter>\r\n" + 
    		"        </wfs:Query>\r\n" + 
    		"</wfs:GetFeature>";
    
    @Test
    public void testWfs1_0() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);            
            getGeoServer().save(cfg);
            
            String output = string(post("wfs", WFS_1_0_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("java.io.FileNotFoundException") > -1);
            
            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);            
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("java.net.MalformedURLException") > -1);
            
            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
            
            output = string(post("wfs", WFS_1_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("java.net.MalformedURLException") > -1);
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
        }
    }
    
    @Test
    public void testWfs1_1() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);            
            getGeoServer().save(cfg);
            
            String output = string(post("wfs", WFS_1_1_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("java.io.FileNotFoundException") > -1);
            
            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);            
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_1_0_REQUEST));
            Assert.assertTrue(output.indexOf("java.net.MalformedURLException") > -1);
            
            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
            
            output = string(post("wfs", WFS_1_1_0_REQUEST));
            Assert.assertTrue(output.indexOf("java.net.MalformedURLException") > -1);
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
        }
    }    
    
    @Test
    public void testWfs2_0() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);            
            getGeoServer().save(cfg);
            
            String output = string(post("wfs", WFS_2_0_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("thisfiledoesnotexist") > -1);
            
            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);            
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_2_0_0_REQUEST));
            System.out.println(output);
            Assert.assertTrue(output.indexOf("Request parsing failed") > -1);
            Assert.assertTrue(output.indexOf("thisfiledoesnotexist") == -1);
            
            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
            
            output = string(post("wfs", WFS_2_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("Request parsing failed") > -1);
            Assert.assertTrue(output.indexOf("thisfiledoesnotexist") == -1);
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);            
            getGeoServer().save(cfg);
        }
    }        
}