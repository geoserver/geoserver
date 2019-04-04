/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.qos.QosRestTestSupport;
import org.geoserver.qos.QosSchema;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Tests on WMS Rest endpoint */
public class QosWMSRestTest extends QosRestTestSupport {

    public static final String QOS_WMS_PATH = "/services/qos/wms/settings";
    public static final String WMS_GETCAPABILITIES =
            "/ows?service=wms&version=1.3.0&request=GetCapabilities";

    @Test
    public void testPutXml() throws Exception {
        String xml = getFileData("test-data/wms-data.xml");
        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + QOS_WMS_PATH, xml, "text/xml");
        assertEquals(200, response.getStatus());
        // get data
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + QOS_WMS_PATH + ".xml");
        // operating info
        assertXpathEvaluatesTo(
                "testbed14",
                "/qosMainConfiguration/metadata/operatingInfo/operationalStatus/title",
                dom);
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#Operational",
                "/qosMainConfiguration/metadata/operatingInfo/operationalStatus/href",
                dom);
        assertXpathEvaluatesTo(
                "01:00:00+03:00",
                "/qosMainConfiguration/metadata/operatingInfo/byDaysOfWeek/startTime",
                dom);
        assertXpathEvaluatesTo(
                "TUESDAY",
                "/qosMainConfiguration/metadata/operatingInfo/byDaysOfWeek/days[2]",
                dom);
        // Statement
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "/qosMainConfiguration/metadata/statement/metric/href",
                dom);
        assertXpathEvaluatesTo(
                "2200", "/qosMainConfiguration/metadata/statement/meassure/value", dom);
        // Anomaly Feed
        assertXpathEvaluatesTo(
                "asdf.txt", "/qosMainConfiguration/metadata/operationAnomalyFeed/href", dom);
        assertXpathEvaluatesTo(
                "text file qith error logs",
                "/qosMainConfiguration/metadata/operationAnomalyFeed/abstract/value",
                dom);
        // representative operation
        assertXpathEvaluatesTo(
                "GET",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/httpMethod",
                dom);
        assertXpathEvaluatesTo(
                "723287.598302843",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/"
                        + "areaConstraint/minX",
                dom);
        assertXpathEvaluatesTo(
                "topp:SOLARES_LIMONES",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/layerName",
                dom);
        assertXpathEvaluatesTo(
                "EPSG:32617",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/crs",
                dom);
        assertXpathEvaluatesTo(
                "256",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/imageWidth/maximunValue",
                dom);
        assertXpathEvaluatesTo(
                "256",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/imageHeight/maximunValue",
                dom);
        assertXpathEvaluatesTo(
                "image/jpeg",
                "/qosMainConfiguration/metadata/representativeOperation/getMapOperation/requestOption/outputFormat[2]",
                dom);
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "/qosMainConfiguration/metadata/representativeOperation/statement/metric/href",
                dom);
        assertXpathEvaluatesTo(
                "s-1",
                "/qosMainConfiguration/metadata/representativeOperation/statement/meassure/uom",
                dom);
        assertXpathEvaluatesTo(
                "moreThanOrEqual",
                "/qosMainConfiguration/metadata/representativeOperation/statement/valueType",
                dom);
    }

    @Test
    public void testGetCapabilitiesMetadata() throws Exception {
        String xml = getFileData("test-data/wms-data.xml");
        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + QOS_WMS_PATH, xml, "text/xml");
        assertEquals(200, response.getStatus());
        // GetCapabilities
        Document dom = getAsDOM(WMS_GETCAPABILITIES);
        // XpathEngine.
        HashMap prefixMap = new HashMap();
        prefixMap.put("wms", "http://www.opengis.net/wms");
        prefixMap.put("xlink", "http://www.w3.org/1999/xlink");
        prefixMap.put("ows", QosSchema.OWS_NAMESPACE);
        prefixMap.put(QosSchema.QOS_WMS_PREFIX, QosSchema.QOS_WMS_NAMESPACE);
        prefixMap.put(QosSchema.QOS_PREFIX, QosSchema.QOS_NAMESPACE);
        NamespaceContext ctx = new SimpleNamespaceContext(prefixMap);
        XMLUnit.setXpathNamespaceContext(ctx);
        // qos:OperatingInfo
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#Operational",
                "/wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:OperatingInfo"
                        + "/qos:OperationalStatus/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "Monday Tuesday Wednesday Thursday Friday Saturday",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:OperatingInfo/qos:ByDaysOfWeek/qos:On",
                dom);
        assertXpathEvaluatesTo(
                "01:00:00+03:00",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:OperatingInfo/qos:ByDaysOfWeek/qos:StartTime",
                dom);
        assertXpathEvaluatesTo(
                "2200",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:MoreThanOrEqual",
                dom);
        // statement
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:Metric/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "Requests per second",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:Metric/@xlink:title",
                dom);
        assertXpathEvaluatesTo(
                "s-1",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:MoreThanOrEqual/@uom",
                dom);
        assertXpathEvaluatesTo(
                "2200",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:MoreThanOrEqual",
                dom);
        // qos:RepresentativeOperation
        XMLAssert.assertXpathExists(
                "wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/ows:DCP/ows:HTTP/ows:Get",
                dom);
        assertXpathEvaluatesTo(
                "723287.598302843 137886.22531418226",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:RepresentativeOperation"
                        + "/qos-wms:GetMapOperation/qos-wms:RequestOption/qos:AreaConstraint/qos:LowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "724754.1479223035 138664.3716564186",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata/qos:RepresentativeOperation"
                        + "/qos-wms:GetMapOperation/qos-wms:RequestOption/qos:AreaConstraint/qos:UpperCorner",
                dom);
        assertXpathEvaluatesTo(
                "topp:SOLARES_LIMONES",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='LayerName']/ows:AllowedValues"
                        + "/ows:Value",
                dom);
        assertXpathEvaluatesTo(
                "EPSG:32617",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='CRS']/ows:AllowedValues"
                        + "/ows:Value",
                dom);
        assertXpathEvaluatesTo(
                "image/png",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='OutputFormat']/ows:AllowedValues"
                        + "/ows:Value[1]",
                dom);
        assertXpathEvaluatesTo(
                "image/jpeg",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='OutputFormat']/ows:AllowedValues"
                        + "/ows:Value[2]",
                dom);
        assertXpathEvaluatesTo(
                "256",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='ImageWidth']/ows:AllowedValues"
                        + "/ows:Range/ows:MaximumValue",
                dom);
        assertXpathEvaluatesTo(
                "256",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wms:GetMapOperation/qos-wms:RequestOption"
                        + "/qos:RequestParameterConstraint[@name='ImageHeight']/ows:AllowedValues"
                        + "/ows:Range/ows:MaximumValue",
                dom);
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos:QualityOfServiceStatement/qos:Metric/@xlink:href",
                dom);
        // Anomaly Feed
        assertXpathEvaluatesTo(
                "asdf.txt",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "text file qith error logs",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/ows:Abstract",
                dom);
        assertXpathEvaluatesTo(
                "text/plain",
                "//wms:WMS_Capabilities/wms:Capability/qos-wms:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/ows:Format",
                dom);
    }
}
