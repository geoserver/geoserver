package org.geoserver.data.gss;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.geotools.xml.impl.NamespaceSupportWrapper;
import org.w3c.dom.Document;
import org.xml.sax.helpers.NamespaceSupport;

public class Capabilities {

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private static final NamespaceContext NS_CONTEXT;
    static {
        NamespaceSupport context = new NamespaceSupport();
        context.declarePrefix("gss", "http://www.opengis.net/gss/1.0");
        context.declarePrefix("wfs", "http://www.opengis.net/wfs");
        context.declarePrefix("ows", "http://www.opengis.net/ows/1.1");
        context.declarePrefix("fes", "http://www.opengis.net/ogc");
        context.declarePrefix("gml", "http://www.opengis.net/gml");
        context.declarePrefix("sf", "http://www.openplans.org/spearfish");
        context.declarePrefix("xs", "http://www.w3.org/2001/XMLSchema");
        context.declarePrefix("app", "http://www.w3.org/2007/app");
        context.declarePrefix("atom", "http://www.w3.org/2005/Atom");
        context.declarePrefix("georss", "http://www.georss.org/georss");
        context.declarePrefix("os", "http://a9.com/-/spec/opensearch/1.1/");
        context.declarePrefix("xlink", "http://www.w3.org/1999/xlink");
        context.declarePrefix("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        NS_CONTEXT = new NamespaceSupportWrapper(context);
    }

    private final Document dom;

    public Capabilities(final Document dom) {
        this.dom = dom;
    }

    public String getServiceTitle() {
        String title = evaluate("//gss:GSS_Capabilities/ows:ServiceIdentification/ows:Title");
        return title;
    }

    private XPath getXPath() {
        XPath xpath = XPATH_FACTORY.newXPath();
        xpath.setNamespaceContext(NS_CONTEXT);
        return xpath;
    }

    private String evaluate(String xpathExpr) {
        final XPath xpath = getXPath();
        try {
            String result = xpath.evaluate(xpathExpr, dom);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getOperationUrl(final String operationName, final boolean postMethod) {
        String xpathExpr = "/gss:GSS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='"
                + operationName + "']/ows:DCP/ows:HTTP/";
        xpathExpr += postMethod ? "ows:Post" : "ows:Get";
        xpathExpr += "/@xlink:href";

        String url = evaluate(xpathExpr);
        return url;
    }
}
