package org.geoserver.gss.functional.v_1_0_0;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import junit.framework.Test;

import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.config.ContactInfo;
import org.geoserver.gss.xml.GSSSchema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Discovery_OGC_KVP_Test extends GSSFunctionalTestSupport {

    private static final String BASE_REQUEST_PATH = "/ows?service=GSS&version=1.0.0&request=GetCapabilities";

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new Discovery_OGC_KVP_Test());
    }

    public void testNamespaces() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);
        Element root = dom.getDocumentElement();
        assertEquals(GSSSchema.NAMESPACE, root.getNamespaceURI());
        assertEquals("gss:GSS_Capabilities", root.getNodeName());

        assertEquals("http://a9.com/-/spec/opensearch/1.1/", attValue(root, "xmlns:os"));
        assertEquals("http://www.opengis.net/gss/1.0", attValue(root, "xmlns:gss"));
        assertEquals("http://www.opengis.net/ows/1.1", attValue(root, "xmlns:ows"));
        assertEquals("http://www.opengis.net/gml", attValue(root, "xmlns:gml"));
        assertEquals("http://www.opengis.net/ogc", attValue(root, "xmlns:fes"));
        assertEquals("http://www.w3.org/2007/app", attValue(root, "xmlns:app"));
        assertEquals("http://www.w3.org/2005/Atom", attValue(root, "xmlns:atom"));
        assertEquals("http://www.w3.org/1999/xlink", attValue(root, "xmlns:xlink"));
        assertEquals("http://www.w3.org/2001/XMLSchema-instance", attValue(root, "xmlns:xsi"));
    }

    private String attValue(Element elem, String attName) {
        NamedNodeMap attributes = elem.getAttributes();
        Node node = attributes.getNamedItem(attName);
        if (node == null) {
            return null;
        }
        return node.getNodeValue();
    }

    public void testBasicStructure() throws Exception {
        ContactInfo contact = getGeoServer().getGlobal().getContact();
        contact.setOnlineResource("http://test.example.com");
        contact.setContactVoice("1-800-OPENGEO");

        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);

        assertXpathEvaluatesTo("1.0.0", "/gss:GSS_Capabilities/@version", dom);

        String serviceIdPath = "/gss:GSS_Capabilities/ows:ServiceIdentification/";
        assertXpathExists(serviceIdPath + "ows:Title", dom);
        assertXpathExists(serviceIdPath + "ows:Abstract", dom);
        assertXpathEvaluatesTo("GSS", serviceIdPath + "ows:ServiceType", dom);
        assertXpathEvaluatesTo("1.0.0", serviceIdPath + "ows:ServiceTypeVersion", dom);
        assertXpathExists(serviceIdPath + "ows:Fees", dom);
        assertXpathExists(serviceIdPath + "ows:AccessConstraints", dom);

        String serviceProvider = "/gss:GSS_Capabilities/ows:ServiceProvider/";
        assertXpathExists(serviceProvider + "ows:ProviderName", dom);
        assertXpathExists(serviceProvider + "ows:ProviderSite", dom);
        assertXpathExists(serviceProvider + "ows:ServiceContact/ows:IndividualName", dom);
        assertXpathExists(serviceProvider + "ows:ServiceContact/ows:PositionName", dom);

        String contactInfo = serviceProvider + "ows:ServiceContact/ows:ContactInfo/";
        assertXpathExists(contactInfo + "ows:Phone/ows:Voice", dom);
        assertXpathExists(contactInfo + "ows:Phone/ows:Facsimile", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:DeliveryPoint", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:City", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:AdministrativeArea", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:PostalCode", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:Country", dom);
        assertXpathExists(contactInfo + "ows:Address/ows:ElectronicEmailAddress", dom);
        assertXpathExists(contactInfo + "ows:OnlineResource", dom);
    }

    public void testOperationsMetadata() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);

        String opsMeta = "/gss:GSS_Capabilities/ows:OperationsMetadata/";

        String getCapsMeta = opsMeta + "ows:Operation[@name='GetCapabilities']";
        assertXpathExists(getCapsMeta, dom);
        assertXpathExists(getCapsMeta + "/ows:DCP/ows:HTTP/ows:Get", dom);

        assertXpathEvaluatesTo("1.0.0", getCapsMeta
                + "/ows:Parameter[@name='AcceptVersions']/ows:AllowedValues/ows:Value[1]", dom);

        assertXpathEvaluatesTo("text/xml", getCapsMeta
                + "/ows:Parameter[@name='AcceptFormats']/ows:AllowedValues/ows:Value[1]", dom);

        assertXpathEvaluatesTo("ServiceIdentification", getCapsMeta
                + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[1]", dom);

        assertXpathEvaluatesTo("ServiceProvider", getCapsMeta
                + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[2]", dom);

        assertXpathEvaluatesTo("OperationsMetadata", getCapsMeta
                + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[3]", dom);

        assertXpathEvaluatesTo("Service", getCapsMeta
                + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[4]", dom);

        // I don't know where this comes from nor what it means in this context
        // assertXpathEvaluatesTo("SupportsGMLObjectTypeList", getCapsMeta
        // + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[5]", dom);

        assertXpathEvaluatesTo("Filter_Capabilities", getCapsMeta
                + "/ows:Parameter[@name='Sections']/ows:AllowedValues/ows:Value[5]", dom);

        assertXpathExists(opsMeta + "ows:Operation[@name='Transaction']/ows:DCP/ows:HTTP/ows:Post",
                dom);

        assertOperationMetadataGetPostLink(dom, "GetEntries");
        String getEntries = opsMeta + "ows:Operation[@name='GetEntries']";
        assertXpathEvaluatesTo("application/atom+xml", getEntries
                + "/ows:Parameter[@name='outputFormat']/ows:AllowedValues/ows:Value[1]", dom);

        assertOperationMetadataGetPostLink(dom, "AcceptChange");
        assertOperationMetadataGetPostLink(dom, "RejectChange");
        assertOperationMetadataGetPostLink(dom, "CreateTopic");
        assertOperationMetadataGetPostLink(dom, "RemoveTopic");
        assertOperationMetadataGetPostLink(dom, "ListTopics");
        assertOperationMetadataGetPostLink(dom, "Subscribe");
        assertOperationMetadataGetPostLink(dom, "ListSubscriptions");
        assertOperationMetadataGetPostLink(dom, "PauseSubscription");
        assertOperationMetadataGetPostLink(dom, "CancelSubscription");
    }

    private void assertOperationMetadataGetPostLink(Document dom, String opName)
            throws XpathException {

        String opsMeta = "/gss:GSS_Capabilities/ows:OperationsMetadata/";
        String path = opsMeta + "ows:Operation[@name='" + opName + "']";

        assertXpathExists(path + "/ows:DCP/ows:HTTP/ows:Get", dom);
        assertXpathExists(path + "/ows:DCP/ows:HTTP/ows:Post", dom);

    }

    public void testAtomPubServiceSection() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);

        String service = "/gss:GSS_Capabilities/app:service";
        assertXpathExists(service, dom);
        assertXpathExists(service + "/app:workspace/atom:title", dom);
        assertXpathEvaluatesTo("3", "count(" + service + "/app:workspace/app:collection)", dom);
        assertXpathEvaluatesTo("3", "count(/" + service
                + "/app:workspace/app:collection/atom:title)", dom);
    }

    public void testGeoSearchSection() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);

        String os = "/gss:GSS_Capabilities/os:OpenSearchDescription";
        assertXpathExists(os + "/os:ShortName", dom);
        assertXpathExists(os + "/os:Description", dom);
        assertXpathExists(os + "/os:Tags", dom);
        assertXpathExists(os + "/os:Contact", dom);
        assertXpathExists(os + "/os:Url[1]", dom);
        assertXpathExists(os + "/os:Url[2]", dom);
        assertXpathExists(os + "/os:Url[3]", dom);
        assertXpathExists(os + "/os:LongName", dom);
        assertXpathExists(os + "/os:Image", dom);
        assertXpathExists(os + "/os:Query", dom);
        assertXpathExists(os + "/os:Developer", dom);
        assertXpathExists(os + "/os:Attribution", dom);
        assertXpathExists(os + "/os:SyndicationRight", dom);
        assertXpathExists(os + "/os:AdultContent", dom);
        assertXpathExists(os + "/os:Language", dom);
        assertXpathExists(os + "/os:OutputEncoding", dom);
        assertXpathExists(os + "/os:InputEncoding", dom);
    }

    public void testConformanceDeclaration() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);
        assertXpathEvaluatesTo("26", "count(//gss:ConformanceDeclaration/gss:ConformanceClass)",
                dom);
    }

    public void testFilterCapabilities() throws Exception {
        Document dom = super.getAsDOM(BASE_REQUEST_PATH);
        // print(dom);

        String filterCaps = "/gss:GSS_Capabilities/fes:Filter_Capabilities/";

        assertXpathExists("/gss:GSS_Capabilities/fes:Filter_Capabilities", dom);
        assertXpathExists(filterCaps + "fes:Spatial_Capabilities", dom);
        assertXpathExists(filterCaps + "fes:Spatial_Capabilities/fes:Spatial_Operators", dom);
        assertXpathExists(filterCaps
                + "fes:Spatial_Capabilities/fes:Spatial_Operators/fes:Disjoint", dom);
        assertXpathExists(filterCaps + "fes:Spatial_Capabilities/fes:Spatial_Operators/fes:Equals",
                dom);

        assertXpathExists(filterCaps + "fes:Scalar_Capabilities", dom);
        assertXpathExists(filterCaps + "fes:Scalar_Capabilities/fes:Logical_Operators", dom);
        assertXpathExists(filterCaps + "fes:Scalar_Capabilities/fes:Comparison_Operators", dom);

    }

}
