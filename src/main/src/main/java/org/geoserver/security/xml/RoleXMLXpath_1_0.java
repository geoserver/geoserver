/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.A_GROUPNAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_PARENTID_RR;
import static org.geoserver.security.xml.XMLConstants.A_PROPERTY_NAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEID_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEREFID_RR;
import static org.geoserver.security.xml.XMLConstants.A_USERNAME_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPROLES_RR;
import static org.geoserver.security.xml.XMLConstants.E_PROPERTY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLELIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREF_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREGISTRY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLE_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERROLES_RR;
import static org.geoserver.security.xml.XMLConstants.NSP_RR;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides precompiled XPath Expressions
 *
 * @author christian
 */
public class RoleXMLXpath_1_0 extends RoleXMLXpath {

    /** Singleton, the implementation is stateless */
    public static final RoleXMLXpath_1_0 Singleton = new RoleXMLXpath_1_0();

    /** XML name space context for user/group store */
    protected XPathExpression roleListExpression;

    protected XPathExpression parentExpression;
    protected XPathExpression roleNameExpression;
    protected XPathExpression rolePropertiesExpression;
    protected XPathExpression propertyNameExpression;
    protected XPathExpression propertyValueExpression;
    protected XPathExpression userRolesExpression;
    protected XPathExpression userNameExpression;
    protected XPathExpression userRolRefsExpression;
    protected XPathExpression userRolRefNameExpression;
    protected XPathExpression groupRolesExpression;
    protected XPathExpression groupNameExpression;
    protected XPathExpression groupRolRefsExpression;
    protected XPathExpression groupRolRefNameExpression;

    /** Constructor is protected, use the static Singleton instance */
    protected RoleXMLXpath_1_0() {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(this.rrContext);
        // versionExpression = compile(xpath,"/"+E_USERREGISTRY+"["+A_VERSION_UR + "]");

        roleListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_ROLELIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_ROLE_RR);
        parentExpression = compileRelativeAttribute(xpath, A_PARENTID_RR, NSP_RR);
        roleNameExpression = compileRelativeAttribute(xpath, A_ROLEID_RR, NSP_RR);

        rolePropertiesExpression = compile(xpath, NSP_RR + ":" + E_PROPERTY_RR);
        propertyNameExpression = compileRelativeAttribute(xpath, A_PROPERTY_NAME_RR, NSP_RR);
        propertyValueExpression = compile(xpath, "text()");

        userRolesExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_USERLIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_USERROLES_RR);
        userNameExpression = compileRelativeAttribute(xpath, A_USERNAME_RR, NSP_RR);
        userRolRefsExpression = compile(xpath, NSP_RR + ":" + E_ROLEREF_RR);
        userRolRefNameExpression = compileRelativeAttribute(xpath, A_ROLEREFID_RR, NSP_RR);

        groupRolesExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_RR
                                + ":"
                                + E_ROLEREGISTRY_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_GROUPLIST_RR
                                + "/"
                                + NSP_RR
                                + ":"
                                + E_GROUPROLES_RR);
        groupNameExpression = compileRelativeAttribute(xpath, A_GROUPNAME_RR, NSP_RR);
        groupRolRefsExpression = compile(xpath, NSP_RR + ":" + E_ROLEREF_RR);
        groupRolRefNameExpression = compileRelativeAttribute(xpath, A_ROLEREFID_RR, NSP_RR);
    }

    @Override
    public XPathExpression getRoleListExpression() {
        return roleListExpression;
    }

    @Override
    public XPathExpression getParentExpression() {
        return parentExpression;
    }

    @Override
    public XPathExpression getRoleNameExpression() {
        return roleNameExpression;
    }

    @Override
    public XPathExpression getRolePropertiesExpression() {
        return rolePropertiesExpression;
    }

    @Override
    public XPathExpression getPropertyNameExpression() {
        return propertyNameExpression;
    }

    @Override
    public XPathExpression getPropertyValueExpression() {
        return propertyValueExpression;
    }

    @Override
    public XPathExpression getUserRolesExpression() {
        return userRolesExpression;
    }

    @Override
    public XPathExpression getUserNameExpression() {
        return userNameExpression;
    }

    @Override
    public XPathExpression getUserRolRefsExpression() {
        return userRolRefsExpression;
    }

    @Override
    public XPathExpression getUserRolRefNameExpression() {
        return userRolRefNameExpression;
    }

    @Override
    public XPathExpression getGroupRolesExpression() {
        return groupRolesExpression;
    }

    @Override
    public XPathExpression getGroupNameExpression() {
        return groupNameExpression;
    }

    @Override
    public XPathExpression getGroupRolRefsExpression() {
        return groupRolRefsExpression;
    }

    @Override
    public XPathExpression getGroupRolRefNameExpression() {
        return groupRolRefNameExpression;
    }
}
