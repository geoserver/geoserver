/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.Map;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.catalog.MapXMLConverter;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Converts a RuleMap into XML and back */
@Component
public class RuleMapXMLConverter extends MapXMLConverter {

    static final String ROOTELEMENT = "rules";

    static final String RULEELEMENT = "rule";

    static final String RESOURCEATTR = "resource";

    @Override
    public int getPriority() {
        // pretty specific, but leave some room for more specific converters just in case
        return (ExtensionPriority.HIGHEST + ExtensionPriority.LOWEST) / 2;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RuleMap.class.isAssignableFrom(clazz);
    }

    @Override
    protected String getMapName(Map<?, ?> map) {
        return ROOTELEMENT;
    }

    /**
     * Generate the JDOM element needed to represent an access control rule and insert it into the
     * parent element given.
     *
     * @param elem , the root elment
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final void insert(Element elem, Object o) {
        if (o instanceof RuleMap) {
            Map<String, String> ruleMap = (Map<String, String>) o;
            for (Map.Entry<String, String> entry : ruleMap.entrySet()) {
                Element ruleElement = elem.getOwnerDocument().createElement(RULEELEMENT);
                ruleElement.setAttribute(RESOURCEATTR, entry.getKey());
                ruleElement.setTextContent(entry.getValue());
                elem.appendChild(ruleElement);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Interpret XML and convert it back to a Map<String,String>
     *
     * @param elem a JDOM element
     * @return the Map<String,String> produced by interpreting the XML
     */
    @Override
    protected Map<String, String> convert(Element elem) {
        Map<String, String> ruleMap = new RuleMap<>();
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                Element ruleElement = (Element) n;
                String resource = ruleElement.getAttribute(RESOURCEATTR);
                ruleMap.put(resource, ruleElement.getTextContent());
            }
        }
        return ruleMap;
    }
}
