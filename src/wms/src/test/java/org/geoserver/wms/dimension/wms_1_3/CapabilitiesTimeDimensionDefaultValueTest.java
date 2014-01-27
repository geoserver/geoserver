package org.geoserver.wms.dimension.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.GatheringByteChannel;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.dimension.DimensionDefaultValueStrategy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesTimeDimensionDefaultValueTest extends WMSTestSupport {

    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "LayerWithTime",
            MockData.SF_PREFIX);  
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        testData.setUpVectorLayer(TIME_WITH_START_END,Collections.EMPTY_MAP,"TimeElevationWithStartEnd.properties",
                DimensionDefaultValueStrategy.class);
        super.setUpTestData(testData);
    }
    
    @Override
    public void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog cat = getCatalog();
        LayerInfo layer = cat.getLayerByName(TIME_WITH_START_END.getLocalPart());
        layer.setEnabled(true);
        layer.setAdvertised(true);
        layer.setAbstract("Diipadaapa");
        cat.save(layer);
       
    }
    
    @Ignore
    public void testFeatureLayerDefaultTime() throws Exception {
        setupFeatureTimeDimension(null);
        Document doc = getAsDOM("sf/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        //FIXME: the test layer just does not show in capabilities(!?)
        debugDocument(doc);
        String layerName = TIME_WITH_START_END.getLocalPart();
        assertXpathExists("//wms:Layer[wms:Name='"+ layerName+"']/wms:Dimension[@name = 'TIME']", doc);
        assertXpathExists("//wms:Layer[wms:Name='"+ layerName+"']/wms:Dimension[@default = 'current']", doc);

    }
    
    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }
    
    protected void setupFeatureTimeDimension(DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info = getCatalog()
                .getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute("startTime");
        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.TIME, di);
        getCatalog().save(info);
    }
    
    private void debugDocument(Document doc) throws Exception{
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);
    }
}
