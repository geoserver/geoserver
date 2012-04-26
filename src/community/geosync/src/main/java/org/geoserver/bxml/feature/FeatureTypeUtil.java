package org.geoserver.bxml.feature;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlStreamReader;

/**
 * The Class FeatureTypeUtil.
 * 
 * @author cfarina
 */
public class FeatureTypeUtil {

    /**
     * Builds the feature type name.
     * 
     * @param r
     *            the r
     * @param elementName
     *            the element name
     * @return the q name
     */
    public static QName buildFeatureTypeName(BxmlStreamReader r, QName elementName) {
        QName name = r.getElementName();

        if (elementName.equals(name)) {
            if (r.getAttributeValue(null, "typeName") != null) {

                String value = r.getAttributeValue(null, "typeName");
                String namespaceURI = null;
                String localPart = value;
                if (value.indexOf(':') != -1) {
                    String prefix = value.substring(0, value.indexOf(':'));
                    namespaceURI = r.getNamespaceURI(prefix);
                    localPart = value.substring(value.indexOf(':') + 1);
                }
                QName typeNameValue = new QName(namespaceURI, localPart);
                return typeNameValue;
            }
        }
        return null;
    }

    /**
     * Builds the feature name.
     * 
     * @param nameString
     *            the name string
     * @param namespaceURI
     *            the namespace uri
     * @return the q name
     */
    public static QName buildQName(String nameString, String namespaceURI) {
        QName qName = null;
        String localPart = nameString;
        if (nameString.indexOf(':') != -1) {
            localPart = nameString.substring(nameString.indexOf(':') + 1);
        }
        qName = new QName(namespaceURI, localPart);
        return qName;
    }
}
