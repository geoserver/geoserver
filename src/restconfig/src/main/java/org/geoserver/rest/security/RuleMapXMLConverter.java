/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.List;
import java.util.Map;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.catalog.MapXMLConverter;
import org.jdom2.Element;
import org.springframework.stereotype.Component;

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
    @SuppressWarnings("unchecked")
    protected final void insert(Element elem, Object o) {
        if (o instanceof RuleMap) {
            Map<String, String> ruleMap = (Map<String, String>) o;
            for (Map.Entry<String, String> entry : ruleMap.entrySet()) {
                Element ruleElement = new Element(RULEELEMENT);
                ruleElement.setAttribute(RESOURCEATTR, entry.getKey());
                ruleElement.setText(entry.getValue());
                elem.getChildren().add(ruleElement);
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
    protected Map<String, String> convert(Element elem) {
        Map<String, String> ruleMap = new RuleMap<>();
        @SuppressWarnings("unchecked")
        List<Element> children = elem.getChildren();
        for (Element ruleElement : children) {
            String resource = ruleElement.getAttributeValue(RESOURCEATTR);
            ruleMap.put(resource, ruleElement.getTextTrim());
        }
        return ruleMap;
    }
}
