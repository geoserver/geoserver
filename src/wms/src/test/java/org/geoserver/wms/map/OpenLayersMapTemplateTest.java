/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OpenLayersMapTemplateTest extends WMSTestSupport {

    @Test
    public void test() throws Exception {
        Configuration cfg = TemplateUtils.getSafeConfiguration();
        cfg.setClassForTemplateLoading(OpenLayersMapOutputFormat.class, "");
        cfg.setObjectWrapper(new BeansWrapper());

        Template template = cfg.getTemplate("OpenLayers2MapTemplate.ftl");
        assertNotNull(template);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        WMSMapContent mapContent = new WMSMapContent();
        mapContent.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
        mapContent.setRequest(request);
        mapContent.setMapWidth(256);
        mapContent.setMapHeight(256);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HashMap map = new HashMap();
        map.put("context", mapContent);
        map.put("request", mapContent.getRequest());
        map.put("maxResolution", Double.valueOf(0.0005)); // just a random number
        map.put("baseUrl", "http://localhost:8080/geoserver/wms");
        map.put("relBaseUrl", "//localhost:8080/geoserver/wms");
        map.put("parameters", new ArrayList());
        map.put("layerName", "layer");
        map.put("units", "degrees");
        map.put("pureCoverage", "false");
        map.put("supportsFiltering", "true");
        map.put("styles", new ArrayList());
        map.put("servicePath", "wms");
        map.put("yx", "false");
        template.process(map, new OutputStreamWriter(output));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        docBuilder.setEntityResolver(
                new EntityResolver() {

                    public InputSource resolveEntity(String publicId, String systemId)
                            throws SAXException, IOException {
                        StringReader reader =
                                new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        InputSource source = new InputSource(reader);
                        source.setPublicId(publicId);
                        source.setSystemId(systemId);
                        return source;
                    }
                });

        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertNotNull(document);

        assertEquals("html", document.getDocumentElement().getNodeName());
    }
}
