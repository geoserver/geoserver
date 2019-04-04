/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wfs;

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

public class QosWFSRestTest extends QosRestTestSupport {

    public static final String QOS_WFS_PATH = "/services/qos/wfs/settings";
    public static final String WFS_GETCAPABILITIES =
            "/ows?service=wfs&version=2.0.0&request=GetCapabilities";

    @Test
    public void testPutXml() throws Exception {
        String xml = getFileData("test-data/wfs-data.xml");
        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + getRestPath(), xml, "text/xml");
        assertEquals(200, response.getStatus());
        // get data
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + getRestPath() + ".xml");
        // operating info
        assertXpathEvaluatesTo(
                "testbed14",
                "/qosMainConfiguration/metadata/operatingInfo/operationalStatus/title",
                dom);
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#PreOperational",
                "/qosMainConfiguration/metadata/operatingInfo/operationalStatus/href",
                dom);
        assertXpathEvaluatesTo(
                "01:00:00+03:00",
                "/qosMainConfiguration/metadata/operatingInfo/byDaysOfWeek/startTime",
                dom);
        assertXpathEvaluatesTo(
                "EVERYDAY", "/qosMainConfiguration/metadata/operatingInfo/byDaysOfWeek/days", dom);
        // Statement
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "/qosMainConfiguration/metadata/statement/metric/href",
                dom);
        assertXpathEvaluatesTo(
                "3189", "/qosMainConfiguration/metadata/statement/meassure/value", dom);
        // Anomaly Feed
        assertXpathEvaluatesTo(
                "asdf.txt", "/qosMainConfiguration/metadata/operationAnomalyFeed/href", dom);
        assertXpathEvaluatesTo(
                "text file qith error logs",
                "/qosMainConfiguration/metadata/operationAnomalyFeed/abstract/value",
                dom);
        // representative operation
        assertXpathEvaluatesTo(
                "POST",
                "/qosMainConfiguration/metadata/representativeOperation/getFeatureOperation/httpMethod",
                dom);
        assertXpathEvaluatesTo(
                "723287.598302843",
                "/qosMainConfiguration/metadata/representativeOperation/getFeatureOperation/adHocQueryConstraints/"
                        + "areaConstraint/minX",
                dom);
    }

    @Test
    public void testGetCapabilitiesMetadata() throws Exception {
        String xml = getFileData("test-data/wfs-data.xml");
        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + getRestPath(), xml, "text/xml");
        assertEquals(200, response.getStatus());
        // get data
        Document dom = getAsDOM(getGetCapabilitiesPath());
        // XpathEngine.
        HashMap prefixMap = new HashMap();
        prefixMap.put("wfs", "http://www.opengis.net/wfs/2.0");
        prefixMap.put("xlink", "http://www.w3.org/1999/xlink");
        prefixMap.put("ows", QosSchema.OWS_NAMESPACE);
        prefixMap.put(QosSchema.QOS_WFS_PREFIX, QosSchema.QOS_WFS_NAMESPACE);
        prefixMap.put(QosSchema.QOS_PREFIX, QosSchema.QOS_NAMESPACE);
        NamespaceContext ctx = new SimpleNamespaceContext(prefixMap);
        XMLUnit.setXpathNamespaceContext(ctx);
        // qos:OperatingInfo
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#PreOperational",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:OperatingInfo"
                        + "/qos:OperationalStatus/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "EveryDay",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:OperatingInfo/qos:ByDaysOfWeek/qos:On",
                dom);
        assertXpathEvaluatesTo(
                "01:00:00+03:00",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:OperatingInfo/qos:ByDaysOfWeek/qos:StartTime",
                dom);
        // statement
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:Metric/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "Requests per second",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:Metric/@xlink:title",
                dom);
        assertXpathEvaluatesTo(
                "s-1",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:LessThanOrEqual/@uom",
                dom);
        assertXpathEvaluatesTo(
                "3189",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:QualityOfServiceStatement/qos:LessThanOrEqual",
                dom);
        // qos:RepresentativeOperation
        XMLAssert.assertXpathExists(
                "wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wfs:GetFeatureOperation/ows:DCP/ows:HTTP/ows:Post",
                dom);
        assertXpathEvaluatesTo(
                "723287.598302843 137886.22531418226",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:RepresentativeOperation"
                        + "/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints/qos:AreaConstraint/qos:LowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "724754.1479223035 138664.3716564186",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata/qos:RepresentativeOperation"
                        + "/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints/qos:AreaConstraint/qos:UpperCorner",
                dom);
        assertXpathEvaluatesTo(
                "topp:SOLARES_LIMONES",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints"
                        + "/qos-wfs:TypeNames",
                dom);
        assertXpathEvaluatesTo(
                "urn:ogc:def:crs:EPSG::32617",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints"
                        + "/qos:RequestParameterConstraint[@name='CRS']/ows:AllowedValues"
                        + "/ows:Value",
                dom);
        assertXpathEvaluatesTo(
                "gml3",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints"
                        + "/qos:RequestParameterConstraint[@name='OutputFormat']/ows:AllowedValues"
                        + "/ows:Value[1]",
                dom);
        assertXpathEvaluatesTo(
                "application/json",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos-wfs:GetFeatureOperation/qos-wfs:AdHocQueryConstraints"
                        + "/qos:RequestParameterConstraint[@name='OutputFormat']/ows:AllowedValues"
                        + "/ows:Value[2]",
                dom);
        assertXpathEvaluatesTo(
                "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#RequestCapacityPerSecond",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:RepresentativeOperation/qos:QualityOfServiceStatement/qos:Metric/@xlink:href",
                dom);
        // Anomaly Feed
        assertXpathEvaluatesTo(
                "asdf.txt",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/@xlink:href",
                dom);
        assertXpathEvaluatesTo(
                "text file qith error logs",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/ows:Abstract",
                dom);
        assertXpathEvaluatesTo(
                "text/plain",
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/qos-wfs:QualityOfServiceMetadata"
                        + "/qos:OperationAnomalyFeed/ows:Format",
                dom);
    }

    protected String getGetCapabilitiesPath() {
        return WFS_GETCAPABILITIES;
    }

    protected String getRestPath() {
        return QOS_WFS_PATH;
    }
}
