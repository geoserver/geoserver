/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class is a base class for concrete implemenations
 *
 * <p>The main purpose is to serve as registry of precompiled {@link XPathExpression} objects
 *
 * @author christian
 */
public abstract class XMLXpath {

    /**
     * Inner class providing a {@link NamespaceContext} implementation
     *
     * @author christian
     */
    public class NamespaceContextImpl implements NamespaceContext {
        private Map<String, String> prefix_ns_Map = new HashMap<String, String>();
        private Map<String, String> ns_prefix_Map = new HashMap<String, String>();

        public String getNamespaceURI(String prefix) {
            return prefix_ns_Map.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            return ns_prefix_Map.get(namespaceURI);
        }

        public Iterator getPrefixes(String namespaceURI) {
            return prefix_ns_Map.keySet().iterator();
        }

        public void register(String prefix, String ns) {
            prefix_ns_Map.put(prefix, ns);
            ns_prefix_Map.put(ns, prefix);
        }
    }

    /** XML name space context for user/group store */
    protected NamespaceContextImpl urContext;
    /** XML name space context for role store */
    protected NamespaceContextImpl rrContext;

    protected XMLXpath() {

        urContext = new NamespaceContextImpl();
        urContext.register(NSP_UR, NS_UR);

        rrContext = new NamespaceContextImpl();
        rrContext.register(NSP_RR, NS_RR);
    }

    /** Compile XPath Strings to {@link XPathExpression} */
    protected XPathExpression compile(XPath xpath, String expression) {
        try {
            return xpath.compile(expression);
        } catch (XPathExpressionException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    /** Creates a relatvie XPathExpression for a XML attribute, needs name space prefix */
    protected XPathExpression compileRelativeAttribute(
            XPath xpath, String attrName, String prefix) {
        // return compile(xpath,"@"+prefix+":"+attrName);
        return compile(xpath, "@" + attrName);
    }
}
