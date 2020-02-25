/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * This class serves as a factory for {@link XMLXpath} objects
 *
 * @author christian
 */
public class XMLXpathFactory extends XMLXpath {

    /** Singleton, the implementation is stateless */
    public static final XMLXpathFactory Singleton = new XMLXpathFactory();

    protected XPathExpression urExpression, rrExpression;

    /** Constructor is protected, use the static Singleton instance */
    protected XMLXpathFactory() {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(urContext);
        urExpression = compile(xpath, "/" + NSP_UR + ":" + E_USERREGISTRY_UR + "/@" + A_VERSION_UR);
        xpath.setNamespaceContext(rrContext);
        rrExpression = compile(xpath, "/" + NSP_RR + ":" + E_ROLEREGISTRY_RR + "/@" + A_VERSION_RR);
    }

    /** @return XPathExpression to get the User/Group xml version */
    public XPathExpression getVersionExpressionUR() {
        return urExpression;
    }

    /** @return XPathExpression to get the role xml version */
    public XPathExpression getVersionExpressionRR() {
        return rrExpression;
    }

    /** Get XPath provider for roles, depending on the version */
    public RoleXMLXpath getRoleXMLXpath(String version) {
        if (VERSION_RR_1_0.equals(version)) return RoleXMLXpath_1_0.Singleton;
        return null;
    }

    /** Get XPath provider for user/groups, depending on the version */
    public UserGroupXMLXpath getUserGroupXMLXpath(String version) {
        if (VERSION_RR_1_0.equals(version)) return UserGroupXMLXpath_1_0.Singleton;
        return null;
    }
}
